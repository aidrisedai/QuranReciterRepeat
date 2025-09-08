package com.repeatquran;

import android.app.Application;

import com.repeatquran.data.PageSeeder;

public class RepeatQuranApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PageSeeder.seedIfNeeded(this);
    }
}

