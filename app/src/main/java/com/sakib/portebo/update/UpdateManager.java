package com.sakib.portebo.update;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import com.sakib.portebo.AppConfig;
import com.sakib.portebo.message.MessageManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import androidx.core.content.FileProvider;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public final class UpdateManager {
    public interface UpdateCallback {void onUpdateResult(boolean hasUpdate,String latestVersion,String releaseNotes);}
    public interface ReleaseCallback {void done(boolean ok,String message,String downloadUrl);}
    private static final ExecutorService EXEC=Executors.newSingleThreadExecutor();private static final Handler MAIN=new Handler(Looper.getMainLooper());private static volatile boolean available;private static volatile String latest="1.1",notes="";private static volatile String apkUrl="";
    private UpdateManager(){}
    public static void init(Context c){checkForUpdate(c,null);MessageManager.refreshReleases(c.getApplicationContext());}
    public static int getCurrentVersionCode(Context c){try{PackageInfo p=c.getPackageManager().getPackageInfo(c.getPackageName(),0);return Build.VERSION.SDK_INT>=28?(int)p.getLongVersionCode():p.versionCode;}catch(Exception e){return 2;}}
    public static String getCurrentVersionName(Context c){try{String s=c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName;return s==null?"1.1":s;}catch(Exception e){return "1.1";}}
    public static void checkForUpdate(Context c,UpdateCallback cb){Context app=c.getApplicationContext();EXEC.execute(()->{boolean has=false;String ver=getCurrentVersionName(app),body="";String asset="";HttpURLConnection h=null;try{h=(HttpURLConnection)new URL("https://api.github.com/repos/"+AppConfig.GITHUB_OWNER+"/"+AppConfig.GITHUB_REPO+"/releases?per_page=100").openConnection();h.setConnectTimeout(7000);h.setReadTimeout(7000);h.setRequestProperty("Accept","application/vnd.github+json");if(h.getResponseCode()==200){JSONArray arr=new JSONArray(read(h.getInputStream()));for(int i=0;i<arr.length();i++){JSONObject r=arr.getJSONObject(i);if(r.optBoolean("draft")||r.optBoolean("prerelease"))continue;String tag=r.optString("tag_name").replaceFirst("^[vV]","");String[] parts=tag.split("\\.");int code=versionCode(parts);JSONObject assets=null;if(r.optJSONArray("assets")!=null){JSONArray aa=r.getJSONArray("assets");for(int ai=0;ai<aa.length();ai++){JSONObject candidate=aa.getJSONObject(ai);if(candidate.optString("name","").toLowerCase(java.util.Locale.ROOT).endsWith(".apk")){assets=candidate;break;}}}if(assets!=null)asset=assets.optString("browser_download_url","");if(compare(tag,getCurrentVersionName(app))>0){has=true;ver=tag;body=r.optString("body","");if(assets==null)asset="";break;}}}}catch(Exception ignored){}finally{if(h!=null)h.disconnect();}available=has;latest=ver;notes=body;apkUrl=asset;final boolean f=has;final String v=ver,n=body;MAIN.post(()->{if(cb!=null)cb.onUpdateResult(f,v,n);});});}
    private static int compare(String a,String b){try{String[] x=a.replaceFirst("^[vV]","").split("\\."),y=b.replaceFirst("^[vV]","").split("\\.");for(int i=0;i<Math.max(x.length,y.length);i++){int q=i<x.length?Integer.parseInt(x[i]):0,r=i<y.length?Integer.parseInt(y[i]):0;if(q!=r)return q>r?1:-1;}return 0;}catch(Exception e){return a.compareTo(b);}}
    private static int versionCode(String[] p){try{int a=Integer.parseInt(p[0]);int b=p.length>1?Integer.parseInt(p[1]):0;int c=p.length>2?Integer.parseInt(p[2]):0;return a*10000+b*100+c;}catch(Exception e){return -1;}}
    public static boolean isUpdateAvailable(){return available;} public static String getLatestVersionName(){return latest;}public static String getLatestReleaseNotes(){return notes;}
    public static void downloadAndInstall(Context context,String url,ReleaseCallback cb){
        Context app=context.getApplicationContext();
        EXEC.execute(()->{
            boolean ok=false;String msg="আপডেট ফাইল নামানো যায়নি।";HttpURLConnection connection=null;File apk=null;
            try{
                String target=url==null||url.trim().isEmpty()?apkUrl:url.trim();
                if(target.isEmpty())throw new IllegalStateException("no_apk_asset");
                connection=(HttpURLConnection)new URL(target).openConnection();connection.setInstanceFollowRedirects(true);connection.setConnectTimeout(12000);connection.setReadTimeout(30000);
                int http=connection.getResponseCode();if(http!=200)throw new IllegalStateException("http_"+http);
                File folder=new File(app.getCacheDir(),"updates");if(!folder.exists()&&!folder.mkdirs())throw new IllegalStateException("cache_unavailable");
                apk=new File(folder,"portebo-update.apk");
                try(InputStream in=new BufferedInputStream(connection.getInputStream());java.io.FileOutputStream out=new java.io.FileOutputStream(apk)){byte[] buffer=new byte[16384];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);out.flush();}
                if(!apk.isFile()||apk.length()<1024)throw new IllegalStateException("empty_apk");
                PackageInfo archive=app.getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(),0);
                if(archive==null)throw new IllegalStateException("invalid_apk");
                if(!app.getPackageName().equals(archive.packageName))throw new IllegalStateException("wrong_package");
                long apkVersion=Build.VERSION.SDK_INT>=28?archive.getLongVersionCode():archive.versionCode;
                if(apkVersion<=getCurrentVersionCode(app))throw new IllegalStateException("not_newer");
                if(Build.VERSION.SDK_INT>=26&&!app.getPackageManager().canRequestPackageInstalls()){
                    Intent settings=new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+app.getPackageName()));settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);app.startActivity(settings);
                    msg="এই অ্যাপ থেকে APK ইনস্টলের অনুমতি দিয়ে আবার Update চাপো।";
                }else{
                    Uri contentUri=FileProvider.getUriForFile(app,app.getPackageName()+".fileprovider",apk);
                    Intent install=new Intent(Intent.ACTION_VIEW);install.setDataAndType(contentUri,"application/vnd.android.package-archive");install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION);app.startActivity(install);
                    ok=true;msg="Android installer খোলা হয়েছে। ইনস্টল নিশ্চিত করো।";
                }
            }catch(Exception e){
                if(e instanceof java.net.UnknownHostException||e instanceof java.net.ConnectException||e instanceof java.net.SocketTimeoutException)msg="ইন্টারনেট সংযোগে সমস্যা—আবার চেষ্টা করো।";
                else if(e instanceof SecurityException)msg="APK ইনস্টলের অনুমতি নেই। সেটিংসে অনুমতি দিয়ে আবার চেষ্টা করো।";
                else if(e instanceof android.content.ActivityNotFoundException)msg="এই ফোনে APK installer খোলা যায়নি।";
                else if(e instanceof IllegalStateException){String detail=e.getMessage();if("no_apk_asset".equals(detail))msg="GitHub release-এ APK ফাইল পাওয়া যায়নি।";else if(detail!=null&&detail.startsWith("http_"))msg="GitHub থেকে APK নামানো যায়নি (HTTP "+detail.substring(5)+")।";else if("wrong_package".equals(detail))msg="APK-টি PorteBo-এর নয়। সঠিক APK বেছে নাও।";else if("not_newer".equals(detail))msg="APK ভার্সন ইনস্টল করা ভার্সনের চেয়ে নতুন নয়।";else if("invalid_apk".equals(detail)||"empty_apk".equals(detail))msg="নামানো ফাইলটি সঠিক APK নয়।";else msg="APK প্রস্তুত করা যায়নি। আবার চেষ্টা করো.";}
                else msg="APK installer-এ পাঠানো যায়নি ("+e.getClass().getSimpleName()+").";
                if(apk!=null&&apk.exists())apk.delete();
            }finally{if(connection!=null)connection.disconnect();}
            final boolean result=ok;final String resultMessage=msg;MAIN.post(()->{if(cb!=null)cb.done(result,resultMessage,apkUrl);});
        });
    }
    public static void publishRelease(Context context,Uri apk,String version,String title,String body,String token,ReleaseCallback cb){Context app=context.getApplicationContext();EXEC.execute(()->{boolean ok=false;String msg="রিলিজ প্রকাশ করা যায়নি।";String assetUrl="";HttpURLConnection create=null,upload=null;try{if(version==null||version.trim().isEmpty()||title==null||title.trim().isEmpty()||token==null||token.trim().isEmpty())throw new IllegalArgumentException();String endpoint="https://api.github.com/repos/"+AppConfig.GITHUB_OWNER+"/"+AppConfig.GITHUB_REPO+"/releases";create=(HttpURLConnection)new URL(endpoint).openConnection();create.setRequestMethod("POST");create.setConnectTimeout(12000);create.setReadTimeout(30000);create.setDoOutput(true);create.setRequestProperty("Accept","application/vnd.github+json");create.setRequestProperty("Authorization",token.startsWith("Bearer ")?token:"Bearer "+token);create.setRequestProperty("X-GitHub-Api-Version","2022-11-28");create.setRequestProperty("Content-Type","application/json");JSONObject j=new JSONObject();j.put("tag_name","v"+version.trim());j.put("name",title.trim());j.put("body","Updated: "+(body==null?"":body));j.put("draft",true);j.put("prerelease",false);try(java.io.OutputStream o=create.getOutputStream()){o.write(j.toString().getBytes("UTF-8"));}if(create.getResponseCode()!=201)throw new IllegalStateException();JSONObject release=new JSONObject(read(create.getInputStream()));String uploadTemplate=release.optString("upload_url","");if(uploadTemplate.isEmpty())throw new IllegalStateException();String uploadUrl=uploadTemplate.replaceAll("\\{[^}]*\\}$","")+"?name=portebo-"+java.net.URLEncoder.encode(version,"UTF-8")+".apk";upload=(HttpURLConnection)new URL(uploadUrl).openConnection();upload.setRequestMethod("POST");upload.setConnectTimeout(12000);upload.setReadTimeout(60000);upload.setDoOutput(true);upload.setRequestProperty("Accept","application/vnd.github+json");upload.setRequestProperty("Authorization",token.startsWith("Bearer ")?token:"Bearer "+token);upload.setRequestProperty("Content-Type","application/vnd.android.package-archive");try(InputStream in=app.getContentResolver().openInputStream(apk);java.io.OutputStream out=upload.getOutputStream()){if(in==null)throw new IllegalStateException();byte[] buf=new byte[16384];int n;while((n=in.read(buf))>0)out.write(buf,0,n);}if(upload.getResponseCode()!=201)throw new IllegalStateException();JSONObject result=new JSONObject(read(upload.getInputStream()));assetUrl=result.optString("browser_download_url","");String releaseId=release.optString("id","");if(releaseId.isEmpty())throw new IllegalStateException();HttpURLConnection publish=(HttpURLConnection)new URL("https://api.github.com/repos/"+AppConfig.GITHUB_OWNER+"/"+AppConfig.GITHUB_REPO+"/releases/"+releaseId).openConnection();publish.setRequestMethod("POST");publish.setRequestProperty("X-HTTP-Method-Override","PATCH");publish.setConnectTimeout(12000);publish.setReadTimeout(30000);publish.setDoOutput(true);publish.setRequestProperty("Accept","application/vnd.github+json");publish.setRequestProperty("Authorization",token.startsWith("Bearer ")?token:"Bearer "+token);publish.setRequestProperty("Content-Type","application/json");JSONObject finalize=new JSONObject();finalize.put("draft",false);try(java.io.OutputStream out=publish.getOutputStream()){out.write(finalize.toString().getBytes("UTF-8"));}if(publish.getResponseCode()!=200)throw new IllegalStateException();publish.disconnect();ok=true;msg="নতুন ভার্সন প্রকাশ হয়েছে।";MessageManager.addHistory(app,"release_v"+version,title,"Updated: "+(body==null?"":body),"Update");}catch(Exception e){msg="রিলিজ হয়নি। GitHub token, পারমিশন আর APK ফাইল দেখো।";}finally{if(create!=null)create.disconnect();if(upload!=null)upload.disconnect();}final boolean f=ok;final String m=msg,u=assetUrl;MAIN.post(()->{if(cb!=null)cb.done(f,m,u);});});}
    private static String read(InputStream i)throws Exception{java.io.BufferedReader r=new java.io.BufferedReader(new java.io.InputStreamReader(i,"UTF-8"));StringBuilder b=new StringBuilder();String s;while((s=r.readLine())!=null)b.append(s);r.close();return b.toString();}
}
