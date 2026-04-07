package com.example.tleilax;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

public class TleilaxApp extends Application {

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }

    @NonNull
    public static Context getAppContext() {
        if (appContext == null) {
            throw new IllegalStateException("Application context is not initialized.");
        }
        return appContext;
    }
}
