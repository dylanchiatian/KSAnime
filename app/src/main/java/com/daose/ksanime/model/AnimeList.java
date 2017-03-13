package com.daose.ksanime.model;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

public class AnimeList extends RealmObject{

    @PrimaryKey
    public String key;

    public RealmList<Anime> animeList;

    @Ignore
    public static final String UPDATED = "home_updated";
    @Ignore
    public static final String TRENDING = "home_trending";
    @Ignore
    public static final String POPULAR = "home_popular";
    @Ignore
    public static final String MORE_TRENDING = "Trending";
    @Ignore
    public static final String MORE_POPULAR = "Popular";

    @Ignore
    public static final String KEY = "key";
    @Ignore
    public static final String LIST = "animeList";

    public AnimeList(){
        animeList = new RealmList<Anime>();
    }
}
