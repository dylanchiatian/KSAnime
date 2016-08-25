package com.daose.anime.Anime;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

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
