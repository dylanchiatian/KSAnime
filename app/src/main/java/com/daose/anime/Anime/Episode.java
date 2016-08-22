package com.daose.anime.Anime;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by STUDENT on 2016-08-17.
 */
public class Episode extends RealmObject {


    @PrimaryKey
    public String url;
    public String videoURL;

    public String name;
    public boolean hasWatched;

    public Episode(){

    }

    public Episode(String name, String url){
        this.name = name;
        this.url = url;
    }
}
