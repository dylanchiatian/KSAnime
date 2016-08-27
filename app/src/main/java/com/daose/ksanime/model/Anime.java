package com.daose.ksanime.model;

import java.io.Serializable;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class Anime extends RealmObject implements Serializable {

    @PrimaryKey
    public String title;

    public RealmList<Episode> episodes;
    //public String mostRecent;
    //public ArrayList<String> genres;
    public String summary;
    public String coverURL;
    public String summaryURL;

    @Index
    public boolean isStarred;

    public Anime(){
        episodes = new RealmList<Episode>();
    }

    public Anime(String title){
        episodes = new RealmList<Episode>();
        this.title = title;
    }
}
