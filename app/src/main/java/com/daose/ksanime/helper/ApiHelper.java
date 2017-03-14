package com.daose.ksanime.helper;


import com.daose.ksanime.model.Anime;
import com.daose.ksanime.model.AnimeList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.realm.Realm;

public class ApiHelper {
    public static void saveListToRealm(final JSONArray list, final String key) {
        final Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                try {
                    final AnimeList realmList = new AnimeList();
                    realmList.key = key;
                    for (int i = 0; i < list.length(); i++) {
                        final JSONObject obj = list.getJSONObject(i);
                        final Anime anime = realm.createOrUpdateObjectFromJson(Anime.class, list.getJSONObject(i));
                        realmList.animeList.add(anime);
                    }
                    realm.insertOrUpdate(realmList);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        realm.close();
    }

    public static void saveAnimeToRealm(final JSONObject obj) {
        final Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.createOrUpdateObjectFromJson(Anime.class, obj);
            }
        });
        realm.close();
    }
}
