package com.daose.ksanime.web;

/**
 * Created by STUDENT on 2016-08-17.
 */
public class Selector {
    public static final String POPULAR_TITLE = "div#tab-mostview span.title";
    public static final String POPULAR_IMAGE = "div#tab-mostview img";
    public static final String POPULAR = POPULAR_IMAGE + "," + POPULAR_TITLE;

    public static final String TRENDING_TITLE = "div#tab-trending span.title";
    public static final String TRENDING_IMAGE = "div#tab-trending img";
    public static final String TRENDING = TRENDING_IMAGE + "," + TRENDING_TITLE;

    public static final String UPDATED = "div.barContent";

    public static final String MAL_IMAGE = "div.picSurround img";
    public static final String MAL_IMAGE_ATTR = "abs:data-src";

    public static final String EPISODE_LIST = "table.listing tbody tr td a";

    public static final String ANIME_DESCRIPTION = "div.barContent p:not(:has(span.info))";
    public static final String VIDEO = "div#divContentVideo video";

    public static final String ANIME_LIST = "table.listing td:eq(0) a";
}
