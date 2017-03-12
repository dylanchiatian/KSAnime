package com.daose.ksanime.model;

import java.io.Serializable;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class Anime extends RealmObject implements Serializable {

    @PrimaryKey
    public String title;

    @Index
    public boolean isLastWatched;

    public RealmList<Episode> episodes;
    public String description;
    public String coverURL;
    public String summaryURL;

    public RealmList<Anime> relatedAnimeList;

    @Index
    public boolean isStarred;

    @Ignore
    public static final String TITLE = "title";
    @Ignore
    public static final String SUMMARY_URL = "summaryURL";

    public Anime(){
        episodes = new RealmList<Episode>();
    }

    public Anime(String title){
        episodes = new RealmList<Episode>();
        this.title = title;
    }
}
