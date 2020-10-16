package com.example.numberaddrdemo;

import android.app.Application;
import android.util.AndroidRuntimeException;

public class BtApplication extends Application {


    public static BtApplication btApplication;

    public static BtApplication getApplication(){
        if (btApplication ==  null) {
            throw new AndroidRuntimeException("btApplication is null, do noting.");
        }
        return btApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        btApplication = this;
    }
}
