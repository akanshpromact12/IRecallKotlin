package com.promact.akansh.irecall_kotlin;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

/**
 * Created by Akansh on 04-08-2017.
 */

public class MyApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
