package com.sakib.portebo;

import android.app.Application;
import com.sakib.portebo.data.AppTierManager;
import com.sakib.portebo.data.PrefsManager;
import com.sakib.portebo.update.UpdateManager;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class PorteBoApp extends Application {
    private static PorteBoApp instance;
    private static final long MAX_CRASH_LOG_BYTES = 32 * 1024; // 32KB silent crash file size cap

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Silent crash handler with file size cap (Requirement 1 - no UI)
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                File crashFile = new File(getFilesDir(), "last_crash.txt");
                if (crashFile.exists() && crashFile.length() > MAX_CRASH_LOG_BYTES) {
                    crashFile.delete();
                }
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
                String crashLog = "=== Crash Report (" + timestamp + ") ===\n"
                    + "Thread: " + thread.getName() + " (id " + thread.getId() + ")\n"
                    + "Exception: " + throwable.getClass().getName() + "\n"
                    + "Message: " + throwable.getMessage() + "\n\n"
                    + "Stack Trace:\n" + sw.toString() + "\n";
                FileWriter writer = new FileWriter(crashFile, false);
                writer.write(crashLog);
                writer.close();
            } catch (Exception ignored) {
            }
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });

        PrefsManager.init(this);

        Executors.newSingleThreadExecutor().execute(() -> {
            AppTierManager.init(getApplicationContext());
            UpdateManager.init(getApplicationContext());
        });
    }

    public static PorteBoApp getInstance() {
        return instance;
    }
}