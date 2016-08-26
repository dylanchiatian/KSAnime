package com.daose.anime.model;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class Episode extends RealmObject {


    @PrimaryKey
    public String url;

    @Index
    public String name;

    public String videoURL;
    public boolean hasWatched;

    public Episode(){

    }

    public Episode(String name, String url){
        this.name = name;
        this.url = url;
    }
}
