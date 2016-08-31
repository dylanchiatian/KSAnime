package com.daose.ksanime.model;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class Episode extends RealmObject {


    @PrimaryKey
    public String url;

    @Index
    public String name;

    @Index
    public String localFilePath;

    public boolean hasWatched;

    public Episode(){}

    public Episode(String name, String url){
        this.name = name;
        this.url = url;
    }
}
