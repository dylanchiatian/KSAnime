package com.daose.anime;

import android.app.Application;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by STUDENT on 2016-08-19.
 */
public class MyApplication extends Application {

    @Override
    public void onCreate(){
        super.onCreate();

        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder(this).deleteRealmIfMigrationNeeded().build();
        Realm.setDefaultConfiguration(realmConfiguration);
    }
}
