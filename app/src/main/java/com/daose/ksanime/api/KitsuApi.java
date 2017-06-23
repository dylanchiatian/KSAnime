package com.daose.ksanime.api;


import android.util.Log;

import com.daose.ksanime.model.Anime;
import com.daose.ksanime.model.KitsuData;
import com.daose.ksanime.model.KitsuDataWrapper;
import com.daose.ksanime.service.KitsuService;

import java.util.Arrays;

import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class KitsuApi {
    private static final String TAG = KitsuApi.class.getSimpleName();
    private static final String BASE_URL = "https://kitsu.io";


    private static KitsuApi ourInstance;
    private Retrofit retrofit;

    public static KitsuApi getInstance() {
        if (ourInstance == null) {
            ourInstance = new KitsuApi();
        }
        return ourInstance;
    }

    private KitsuApi() {
        retrofit = new Retrofit.Builder()
                .baseUrl(KitsuApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    private String cleanTitle(final String animeTitle) {
        return animeTitle.replace("(Sub)", "").replace("(Dub)", "");
    }

    public void fetchCoverUrl(final String animeTitle) {
        final KitsuService service = retrofit.create(KitsuService.class);
        final String title = cleanTitle(animeTitle);

        final Call<KitsuDataWrapper<KitsuData>> list = service.getAnimeData(title);
        list.enqueue(new Callback<KitsuDataWrapper<KitsuData>>() {
            @Override
            public void onResponse(Call<KitsuDataWrapper<KitsuData>> call, Response<KitsuDataWrapper<KitsuData>> response) {
                if(response.body() != null && response.body().data.size() > 0) {
                    final KitsuData data = response.body().data.get(0);

                    Realm realm = Realm.getDefaultInstance();
                    realm.executeTransactionAsync(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            final Anime anime = realm.where(Anime.class).equalTo(Anime.TITLE, animeTitle).findFirst();
                            anime.coverURL = data.attributes.posterImage.small;
                        }
                    });
                    realm.close();
                } else {
                    Log.e(TAG, "cover url returned size of 0");
                }
            }

            @Override
            public void onFailure(Call<KitsuDataWrapper<KitsuData>> call, Throwable t) {
                Log.e(TAG, "Failed getting cover url", t);
            }
        });
    }
}
