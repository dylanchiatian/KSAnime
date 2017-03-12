package com.daose.ksanime.model;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class Episode extends RealmObject {


    @PrimaryKey
    public String url;

    @Index
    public String name;

    @Index
    public boolean hasWatched;

    @Ignore
    public static final String URL = "url";
    @Ignore
    public static final String NAME = "name";

    public Episode(){}

    public Episode(String name, String url){
        this.name = name;
        this.url = url;
    }
}
