package com.daose.ksanime.service;


import com.daose.ksanime.model.KitsuData;
import com.daose.ksanime.model.KitsuDataWrapper;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface KitsuService {
    @Headers({ "Accept: application/vnd.api+json" })
    @GET("api/edge/anime?fields[anime]=slug,canonicalTitle,titles,posterImage&page[limit]=2")
    Call<KitsuDataWrapper<KitsuData>> getAnimeData(@Query("filter[text]") String animeTitle);
}
