package com.daose.anime.Anime;

import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.PrimaryKey;

/**
 * Created by STUDENT on 2016-08-19.
 */
public class AnimeList extends RealmObject{

    @PrimaryKey
    public String key;

    public RealmList<Anime> animeList;

    public AnimeList(String key, RealmList<Anime> animeList){
        this.key = key;
        this.animeList = animeList;
    }

    public AnimeList(){

    }
}
