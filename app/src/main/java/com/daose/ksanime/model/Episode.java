package com.daose.ksanime.model;

import io.realm.RealmCollection;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class Episode extends RealmObject {


    @PrimaryKey
    public String url;

    @Index
    public String name;

    public String url1080p;
    public String url720p;
    public String url480p;
    public String url360p;

    public String videoURL;
    public boolean hasWatched;

    public Episode(){}

    public Episode(String name, String url){
        this.name = name;
        this.url = url;
    }
}
