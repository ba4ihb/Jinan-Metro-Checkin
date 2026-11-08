package com.jnsub.quanchentrail;

import android.app.Application;
import android.content.Context;

public final class QuanchenTrailApp extends Application {
    private static volatile QuanchenTrailApp instance;

    public static QuanchenTrailApp get() {
        return instance;
    }

    public static Context appContext() {
        return instance == null ? null : instance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        instance = this;
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            CrashLog.write(error);
            if (previous != null) previous.uncaughtException(thread, error);
        });
        super.onCreate();
    }
}
