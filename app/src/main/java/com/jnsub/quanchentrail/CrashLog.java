package com.jnsub.quanchentrail;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class CrashLog {
    private static final String TAG = "QuanchenTrail";

    private CrashLog() {}

    public static void write(Throwable error) {
        Log.e(TAG, "Startup/runtime failure", error);
        try {
            File dir = new File(QuanchenTrailApp.appContext().getFilesDir(), "logs");
            if (!dir.exists() && !dir.mkdirs()) return;
            File file = new File(dir, "crash.log");
            try (FileWriter writer = new FileWriter(file, true)) {
                String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                        .format(new Date());
                writer.write("\n[" + time + "]\n");
                StringWriter stack = new StringWriter();
                error.printStackTrace(new PrintWriter(stack));
                writer.write(stack.toString());
            }
        } catch (Throwable ignored) {
            // Logging must never take down the app again.
        }
    }
}

