package com.daose.anime.Anime;

import java.io.Serializable;
import java.util.ArrayList;

import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * Created by STUDENT on 2016-08-16.
 */
public class Anime extends RealmObject implements Serializable {

    @PrimaryKey
    public String title;

    public RealmList<Episode> episodes;
    //public String mostRecent;
    //public ArrayList<String> genres;
    public String summary;
    public String coverURL;
    public String summaryURL;

    public Anime(){
        episodes = new RealmList<Episode>();
    }

    public Anime(String title){
        episodes = new RealmList<Episode>();
        this.title = title;
    }
}
