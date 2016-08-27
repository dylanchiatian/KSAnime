package com.daose.anime;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.applovin.sdk.AppLovinSdk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by STUDENT on 2016-08-19.
 */
public class MyApplication extends Application {

    private static final String TAG = MyApplication.class.getSimpleName();

    @Override
    public void onCreate(){
        super.onCreate();

        SharedPreferences settings = getSharedPreferences("daose", 0);
        if (settings.getBoolean("first_install", true)) {
            Log.d(TAG, "first install detected");
            copyBundledRealmFile(getResources().openRawResource(R.raw.export), "default.realm");
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("first_install", false);
            editor.apply();
        }

        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder(this).deleteRealmIfMigrationNeeded().build();
        Realm.setDefaultConfiguration(realmConfiguration);

        AppLovinSdk.initializeSdk(getApplicationContext());

        /* Get Database
        Realm realm = Realm.getDefaultInstance();
        Log.d("REALM", realm.getPath());
        File exportRealmFile = null;
        try {
            // get or create an "export.realm" file
            exportRealmFile = new File(this.getExternalCacheDir(), "export.realm");

            // if "export.realm" already exists, delete
            exportRealmFile.delete();

            // copy current realm to "export.realm"
            realm.writeCopyTo(exportRealmFile);
            Log.d("REALM", exportRealmFile.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
        realm.close();
        */
    }

    private String copyBundledRealmFile(InputStream inputStream, String outFileName) {
        try {
            File file = new File(this.getFilesDir(), outFileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, bytesRead);
            }
            outputStream.close();
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
