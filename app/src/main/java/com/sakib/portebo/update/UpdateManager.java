package com.sakib.portebo.update;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.FileProvider;
import com.sakib.portebo.AppConfig;
import com.sakib.portebo.message.MessageManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UpdateManager {

    public interface UpdateCallback {
        void onUpdateResult(boolean hasUpdate, String latestVersion, String releaseNotes);
    }

    public interface ReleaseCallback {
        void done(boolean ok, String message, String downloadUrl);
    }

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static volatile boolean available;
    private static volatile String latest = "1.1";
    private static volatile String notes = "";
    private static volatile String apkUrl = "";

    private UpdateManager() {
    }

    public static void init(Context context) {
        checkForUpdate(context, null);
        MessageManager.refreshReleases(context.getApplicationContext());
    }

    public static int getCurrentVersionCode(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= 28) {
                return (int) packageInfo.getLongVersionCode();
            }
            return packageInfo.versionCode;
        } catch (Exception ignored) {
            return 2;
        }
    }

    public static String getCurrentVersionName(Context context) {
        try {
            String versionName = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .versionName;
            return versionName == null ? "1.1" : versionName;
        } catch (Exception ignored) {
            return "1.1";
        }
    }

    public static void checkForUpdate(Context context, UpdateCallback callback) {
        Context appContext = context.getApplicationContext();
        EXEC.execute(() -> {
            boolean hasUpdate = false;
            String latestVersion = getCurrentVersionName(appContext);
            String releaseNotes = "";
            String assetUrl = "";
            HttpURLConnection connection = null;
            try {
                String releasesUrl = "https://api.github.com/repos/"
                        + AppConfig.GITHUB_OWNER + "/" + AppConfig.GITHUB_REPO + "/releases?per_page=100";
                connection = (HttpURLConnection) new URL(releasesUrl).openConnection();
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(7000);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                if (connection.getResponseCode() == 200) {
                    JSONArray releases = new JSONArray(read(connection.getInputStream()));
                    for (int i = 0; i < releases.length(); i++) {
                        JSONObject release = releases.getJSONObject(i);
                        if (release.optBoolean("draft") || release.optBoolean("prerelease")) {
                            continue;
                        }
                        String tag = release.optString("tag_name").replaceFirst("^[vV]", "");
                        String[] tagParts = tag.split("\\.");
                        versionCode(tagParts);
                        JSONObject apkAsset = null;
                        if (release.optJSONArray("assets") != null) {
                            JSONArray assets = release.getJSONArray("assets");
                            for (int assetIndex = 0; assetIndex < assets.length(); assetIndex++) {
                                JSONObject candidate = assets.getJSONObject(assetIndex);
                                if (candidate.optString("name", "").toLowerCase(java.util.Locale.ROOT).endsWith(".apk")) {
                                    apkAsset = candidate;
                                    break;
                                }
                            }
                        }
                        if (apkAsset != null) {
                            assetUrl = apkAsset.optString("browser_download_url", "");
                        }
                        if (compare(tag, getCurrentVersionName(appContext)) > 0) {
                            hasUpdate = true;
                            latestVersion = tag;
                            releaseNotes = release.optString("body", "");
                            if (apkAsset == null) {
                                assetUrl = "";
                            }
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            available = hasUpdate;
            latest = latestVersion;
            notes = releaseNotes;
            apkUrl = assetUrl;
            final boolean result = hasUpdate;
            final String resultVersion = latestVersion;
            final String resultNotes = releaseNotes;
            MAIN.post(() -> {
                if (callback != null) {
                    callback.onUpdateResult(result, resultVersion, resultNotes);
                }
            });
        });
    }

    private static int compare(String firstVersion, String secondVersion) {
        try {
            String[] first = firstVersion.replaceFirst("^[vV]", "").split("\\.");
            String[] second = secondVersion.replaceFirst("^[vV]", "").split("\\.");
            int length = Math.max(first.length, second.length);
            for (int i = 0; i < length; i++) {
                int firstNumber = i < first.length ? Integer.parseInt(first[i]) : 0;
                int secondNumber = i < second.length ? Integer.parseInt(second[i]) : 0;
                if (firstNumber != secondNumber) {
                    return firstNumber > secondNumber ? 1 : -1;
                }
            }
            return 0;
        } catch (Exception ignored) {
            return firstVersion.compareTo(secondVersion);
        }
    }

    private static int versionCode(String[] parts) {
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return major * 10000 + minor * 100 + patch;
        } catch (Exception ignored) {
            return -1;
        }
    }

    public static boolean isUpdateAvailable() {
        return available;
    }

    public static String getLatestVersionName() {
        return latest;
    }

    public static String getLatestReleaseNotes() {
        return notes;
    }
    public static void downloadAndInstall(Context context, String url, ReleaseCallback callback) {
        Context appContext = context.getApplicationContext();
        EXEC.execute(() -> {
            boolean success = false;
            String message = "আপডেট ফাইল নামানো যায়নি।";
            HttpURLConnection connection = null;
            File apkFile = null;
            try {
                String targetUrl = url == null || url.trim().isEmpty() ? apkUrl : url.trim();
                if (targetUrl.isEmpty()) {
                    throw new IllegalStateException("no_apk_asset");
                }
                connection = (HttpURLConnection) new URL(targetUrl).openConnection();
                connection.setInstanceFollowRedirects(true);
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(30000);
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    throw new IllegalStateException("http_" + responseCode);
                }
                File updateFolder = new File(appContext.getCacheDir(), "updates");
                if (!updateFolder.exists() && !updateFolder.mkdirs()) {
                    throw new IllegalStateException("cache_unavailable");
                }
                apkFile = new File(updateFolder, "portebo-update.apk");

                // --- NEW: Progress Tracking & Notification ---
                int fileLength = connection.getContentLength();
                long totalBytes = 0;
                long startTime = System.currentTimeMillis();

                try (
                        InputStream input = new BufferedInputStream(connection.getInputStream());
                        java.io.FileOutputStream output = new java.io.FileOutputStream(apkFile)
                ) {
                    byte[] buffer = new byte[16384];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        totalBytes += count;
                        output.write(buffer, 0, count);

                        if (fileLength > 0) {
                            int progress = (int) ((totalBytes * 100) / fileLength);
                            long timeTaken = System.currentTimeMillis() - startTime;
                            long etaMillis = (timeTaken * (fileLength - totalBytes)) / totalBytes;
                            int etaSeconds = (int) (etaMillis / 1000);
                            String etaText = etaSeconds > 0 ? etaSeconds + "s left" : "Almost done...";
                            UpdateNotificationManager.showProgress(appContext, progress, 100, "Downloading update...", etaText);
                        } else {
                            UpdateNotificationManager.showProgress(appContext, 0, 0, "Downloading update...", null);
                        }
                    }
                    output.flush();
                }
                // Stage: Verifying
                UpdateNotificationManager.showProgress(appContext, 100, 100, "Verifying APK...", null);
                // --- END NEW ---

                if (!apkFile.isFile() || apkFile.length() < 1024) {
                    throw new IllegalStateException("empty_apk");
                }
                PackageInfo archiveInfo = appContext.getPackageManager().getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
                if (archiveInfo == null) {
                    throw new IllegalStateException("invalid_apk");
                }
                if (!appContext.getPackageName().equals(archiveInfo.packageName)) {
                    throw new IllegalStateException("wrong_package");
                }
                long apkVersionCode;
                if (Build.VERSION.SDK_INT >= 28) {
                    apkVersionCode = archiveInfo.getLongVersionCode();
                } else {
                    apkVersionCode = archiveInfo.versionCode;
                }
                if (apkVersionCode <= getCurrentVersionCode(appContext)) {
                    throw new IllegalStateException("not_newer");
                }
                boolean canInstall = Build.VERSION.SDK_INT < 26 || appContext.getPackageManager().canRequestPackageInstalls();
                if (!canInstall) {
                    Intent settingsIntent = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + appContext.getPackageName()));
                    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    appContext.startActivity(settingsIntent);
                    message = "এই অ্যাপ থেকে APK ইনস্টলের অনুমতি দিয়ে আবার Update চাপো।";
                } else {
                    Uri contentUri = FileProvider.getUriForFile(appContext, appContext.getPackageName() + ".fileprovider", apkFile);
                    Intent installIntent = new Intent(Intent.ACTION_VIEW);
                    installIntent.setDataAndType(contentUri, "application/vnd.android.package-archive");
                    installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    
                    // --- NEW: Show Installing (Green) & Hide later ---
                    UpdateNotificationManager.showInstalling(appContext);
                    appContext.startActivity(installIntent);
                    success = true;
                    message = "Android installer খোলা হয়েছে। ইনস্টল নিশ্চিত করো।";
                    
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        UpdateNotificationManager.hide(appContext);
                    }, 3000);
                    // --- END NEW ---
                }
            } catch (Exception exception) {
                // --- NEW: Show Error (Red) ---
                UpdateNotificationManager.showError(appContext, "Error: " + exception.getMessage());
                // --- END NEW ---

                if (exception instanceof java.net.UnknownHostException || exception instanceof java.net.ConnectException || exception instanceof java.net.SocketTimeoutException) {
                    message = "ইন্টারনেট সংযোগে সমস্যা—আবার চেষ্টা করো।";
                } else if (exception instanceof SecurityException) {
                    message = "APK ইনস্টলের অনুমতি নেই। সেটিংসে অনুমতি দিয়ে আবার চেষ্টা করো।";
                } else if (exception instanceof android.content.ActivityNotFoundException) {
                    message = "এই ফোনে APK installer খোলা যায়নি।";
                } else if (exception instanceof IllegalStateException) {
                    String detail = exception.getMessage();
                    if ("no_apk_asset".equals(detail)) {
                        message = "GitHub release-এ APK ফাইল পাওয়া যায়নি।";
                    } else if (detail != null && detail.startsWith("http_")) {
                        message = "GitHub থেকে APK নামানো যায়নি (HTTP " + detail.substring(5) + ")।";
                    } else if ("wrong_package".equals(detail)) {
                        message = "APK-টি PorteBo-এর নয়। সঠিক APK বেছে নাও।";
                    } else if ("not_newer".equals(detail)) {
                        message = "APK ভার্সন ইনস্টল করা ভার্সনের চেয়ে নতুন নয়।";
                    } else if ("invalid_apk".equals(detail) || "empty_apk".equals(detail)) {
                        message = "নামানো ফাইলটি সঠিক APK নয়।";
                    } else {
                        message = "APK প্রস্তুত করা যায়নি। আবার চেষ্টা করো.";
                    }
                } else {
                    message = "APK installer-এ পাঠানো যায়নি (" + exception.getClass().getSimpleName() + ").";
                }
                if (apkFile != null && apkFile.exists()) {
                    apkFile.delete();
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            final boolean result = success;
            final String resultMessage = message;
            MAIN.post(() -> {
                if (callback != null) {
                    callback.done(result, resultMessage, apkUrl);
                }
            });
        });
    }
    public static void publishRelease(Context context, Uri apkUri, String version, String title, String body, String token, ReleaseCallback callback) {
        Context appContext = context.getApplicationContext();
        EXEC.execute(() -> {
            boolean success = false;
            String message = "রিলিজ প্রকাশ করা যায়নি।";
            String assetUrl = "";
            HttpURLConnection createConnection = null;
            HttpURLConnection uploadConnection = null;
            try {
                if (version == null || version.trim().isEmpty() || title == null || title.trim().isEmpty() || token == null || token.trim().isEmpty()) {
                    throw new IllegalArgumentException();
                }
                String cleanBody = cleanReleaseNotes(body);
                String releaseEndpoint = "https://api.github.com/repos/" + AppConfig.GITHUB_OWNER + "/" + AppConfig.GITHUB_REPO + "/releases";
                createConnection = (HttpURLConnection) new URL(releaseEndpoint).openConnection();
                createConnection.setRequestMethod("POST");
                createConnection.setConnectTimeout(12000);
                createConnection.setReadTimeout(30000);
                createConnection.setDoOutput(true);
                createConnection.setRequestProperty("Accept", "application/vnd.github+json");
                createConnection.setRequestProperty("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
                createConnection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
                createConnection.setRequestProperty("Content-Type", "application/json");
                JSONObject releaseRequest = new JSONObject();
                releaseRequest.put("tag_name", "v" + version.trim());
                releaseRequest.put("name", title.trim());
                releaseRequest.put("body", "Updated: " + cleanBody);
                releaseRequest.put("draft", true);
                releaseRequest.put("prerelease", false);
                try (java.io.OutputStream output = createConnection.getOutputStream()) {
                    output.write(releaseRequest.toString().getBytes("UTF-8"));
                }
                if (createConnection.getResponseCode() != 201) {
                    throw new IllegalStateException();
                }
                JSONObject release = new JSONObject(read(createConnection.getInputStream()));
                String uploadTemplate = release.optString("upload_url", "");
                if (uploadTemplate.isEmpty()) {
                    throw new IllegalStateException();
                }
                String uploadUrl = uploadTemplate.replaceAll("\\{[^}]*\\}$", "") + "?name=portebo-" + java.net.URLEncoder.encode(version, "UTF-8") + ".apk";
                uploadConnection = (HttpURLConnection) new URL(uploadUrl).openConnection();
                uploadConnection.setRequestMethod("POST");
                uploadConnection.setConnectTimeout(12000);
                uploadConnection.setReadTimeout(60000);
                uploadConnection.setDoOutput(true);
                uploadConnection.setRequestProperty("Accept", "application/vnd.github+json");
                uploadConnection.setRequestProperty("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
                uploadConnection.setRequestProperty("Content-Type", "application/vnd.android.package-archive");
                try (InputStream input = appContext.getContentResolver().openInputStream(apkUri); java.io.OutputStream output = uploadConnection.getOutputStream()) {
                    if (input == null) {
                        throw new IllegalStateException();
                    }
                    byte[] buffer = new byte[16384];
                    int count;
                    while ((count = input.read(buffer)) > 0) {
                        output.write(buffer, 0, count);
                    }
                }
                if (uploadConnection.getResponseCode() != 201) {
                    throw new IllegalStateException();
                }
                JSONObject uploadResult = new JSONObject(read(uploadConnection.getInputStream()));
                assetUrl = uploadResult.optString("browser_download_url", "");
                String releaseId = release.optString("id", "");
                if (releaseId.isEmpty()) {
                    throw new IllegalStateException();
                }
                HttpURLConnection publishConnection = (HttpURLConnection) new URL("https://api.github.com/repos/" + AppConfig.GITHUB_OWNER + "/" + AppConfig.GITHUB_REPO + "/releases/" + releaseId).openConnection();
                publishConnection.setRequestMethod("POST");
                publishConnection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
                publishConnection.setConnectTimeout(12000);
                publishConnection.setReadTimeout(30000);
                publishConnection.setDoOutput(true);
                publishConnection.setRequestProperty("Accept", "application/vnd.github+json");
                publishConnection.setRequestProperty("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
                publishConnection.setRequestProperty("Content-Type", "application/json");
                JSONObject publishRequest = new JSONObject();
                publishRequest.put("draft", false);
                try (java.io.OutputStream output = publishConnection.getOutputStream()) {
                    output.write(publishRequest.toString().getBytes("UTF-8"));
                }
                if (publishConnection.getResponseCode() != 200) {
                    throw new IllegalStateException();
                }
                publishConnection.disconnect();
                success = true;
                message = "নতুন ভার্সন প্রকাশ হয়েছে।";
                MessageManager.addHistory(appContext, "release_v" + version, title, cleanBody, "Update");
            } catch (Exception ignored) {
                message = "রিলিজ হয়নি। GitHub token, পারমিশন আর APK ফাইল দেখো।";
            } finally {
                if (createConnection != null) {
                    createConnection.disconnect();
                }
                if (uploadConnection != null) {
                    uploadConnection.disconnect();
                }
            }
            final boolean result = success;
            final String resultMessage = message;
            final String resultUrl = assetUrl;
            MAIN.post(() -> {
                if (callback != null) {
                    callback.done(result, resultMessage, resultUrl);
                }
            });
        });
    }

    private static String cleanReleaseNotes(String text) {
        String cleaned = text == null ? "" : text.trim();
        while (cleaned.toLowerCase(java.util.Locale.ROOT).startsWith("updated:")) {
            cleaned = cleaned.substring("updated:".length()).trim();
            if (cleaned.startsWith("-")) {
                cleaned = cleaned.substring(1).trim();
            }
        }
        return cleaned;
    }

    private static String read(InputStream input) throws Exception {
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(input, "UTF-8"));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }
}