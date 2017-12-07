/*
 * Copyright (C) 2017 kkkkan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kkkkan.youtube.tubtub.utils;

/**
 * Basic configuration values used in app
 * Created by smedic on 2.2.16..
 */

public final class Config {

    public static final boolean DEBUG = false;

    public static final String SUGGESTIONS_URL = "http://suggestqueries.google.com/complete/search?client=youtube&ds=yt&q=";
    public static final String YOUTUBE_BASE_URL = "http://youtube.com/watch?v=";
    public static final String YOUTUBE_BASE_SHARE = "https://youtu.be/";
    public static final String SHARE_VIDEO_URL = "http://youtube.com/watch?v=";
    public static final String SHARE_PLAYLIST_URL = "https://www.youtube.com/playlist?list=";
    public static final String YOUTUBE_TYPE = "YT_MEDIA_TYPE";
    public static final String YOUTUBE_TYPE_VIDEO = "YT_VIDEO";
    public static final String YOUTUBE_TYPE_PLAYLIST = "YT_PLAYLIST";
    public static final String YOUTUBE_TYPE_PLAYLIST_VIDEO_POS = "YT_PLAYLIST_VIDEO_POS";

    public static final String YOUTUBE_API_KEY = "AIzaSyApidIQCEBbqishTDtwuNky9uA-wyqZlR0";

    public static final long NUMBER_OF_VIDEOS_RETURNED = 50; //due to YouTube API rules - MAX 50


    final static public int YouTubeVideosLoaderId = 1;
    final static public int YouTubePlaylistLoaderId = 2;
    final static public int YouTubePlaylistDetailLoaderId = 3;
    final static public int SuggestionsLoaderId = 4;
    final static public int YouTubeShareVideoGetLoader = 5;

}
