package com.daose.ksanime;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class MyApplication extends Application {

    private static final String TAG = MyApplication.class.getSimpleName();

    @Override
    public void onCreate(){
        super.onCreate();

        SharedPreferences settings = getSharedPreferences("daose", 0);

        if (settings.getBoolean("first_install", true)) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("first_install", false);
            editor.apply();
        }
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder(this).deleteRealmIfMigrationNeeded().build();
        Realm.setDefaultConfiguration(realmConfiguration);
    }
}
