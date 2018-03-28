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
package com.kkkkan.youtube.tubtub.youtube;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;
import android.util.Pair;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.kkkkan.youtube.tubtub.model.YouTubePlaylist;
import com.kkkkan.youtube.tubtub.model.YouTubeVideo;
import com.kkkkan.youtube.tubtub.utils.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.kkkkan.youtube.tubtub.youtube.VideosLoaderMethods.getPlaylistList;
import static com.kkkkan.youtube.tubtub.youtube.VideosLoaderMethods.getVideoList;
import static com.kkkkan.youtube.tubtub.youtube.YouTubeSingleton.getYouTube;

/**
 * Created by smedic on 13.2.17..
 */

public class YouTubeVideosLoader extends AsyncTaskLoader<Pair<VideosLoaderMethods.SearchResultVideo, VideosLoaderMethods.SearchResultPlaylist>> {
    static private final String TAG = "YouTubeVideosLoader";
    private YouTube youtube = getYouTube();
    private String keywords;

    public YouTubeVideosLoader(Context context, String keywords) {
        super(context);
        this.keywords = keywords;
    }

    @Override
    public Pair<VideosLoaderMethods.SearchResultVideo, VideosLoaderMethods.SearchResultPlaylist> loadInBackground() {
        List<YouTubeVideo> videoItems = new ArrayList<>();
        List<YouTubePlaylist> playlistItems = new ArrayList<>();
        String videosNextToken = null;
        String playlistsNextToken = null;
        try {
            YouTube.Search.List searchList = youtube.search().list("id,snippet");
            YouTube.Search.List searchPlayList = youtube.search().list("id,snippet");
            YouTube.Videos.List videosList = youtube.videos().list("id,contentDetails,statistics");
            YouTube.Playlists.List playlistList = youtube.playlists().list("id,snippet,contentDetails,status");

            //videoの検索
            SearchListResponse searchVideoResponse = searchVideo(searchList);
            //検索結果のvideoのリストを取得
            List<SearchResult> searchVideoResults = searchVideoResponse.getItems();
            //検索結果のそれぞれのvideoの詳細情報の取得
            videoItems = getVideoList(videosList, searchVideoResults);
            //videoのnextToken取得(最終ページの場合はnull?)
            videosNextToken = searchVideoResponse.getNextPageToken();

            //playlistの検索
            SearchListResponse searchPlaylistResponse = searchPlaylist(searchPlayList);
            //検索結果のplaylistのリストを取得
            List<SearchResult> searchPlayListResults = searchPlaylistResponse.getItems();
            //検索結果のそれぞれのplaylistの詳細情報の取得
            playlistItems = getPlaylistList(playlistList, searchPlayListResults);
            //playlistのnextToken取得(最終ページの場合はnull?)
            playlistsNextToken = searchPlaylistResponse.getNextPageToken();

        } catch (IOException e) {
            Log.d(TAG, "IOException");
            e.printStackTrace();
        }

        return new Pair<>(new VideosLoaderMethods.SearchResultVideo(videoItems, videosNextToken), new VideosLoaderMethods.SearchResultPlaylist(playlistItems, playlistsNextToken));
    }

    @Override
    public void deliverResult(Pair<VideosLoaderMethods.SearchResultVideo, VideosLoaderMethods.SearchResultPlaylist> data) {
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            return;
        }
        super.deliverResult(data);
    }

    /**
     * ビデオの検索を行い、検索結果を返す
     *
     * @param searchList
     * @return
     * @throws IOException
     */
    public SearchListResponse searchVideo(YouTube.Search.List searchList) throws IOException {
        searchList.setKey(Config.YOUTUBE_API_KEY);
        searchList.setType("video"); //TODO ADD PLAYLISTS SEARCH
        searchList.setMaxResults(Config.NUMBER_OF_VIDEOS_RETURNED);
        searchList.setFields("items(id/videoId,snippet/title,snippet/thumbnails/default/url)");
        //search
        searchList.setQ(keywords);
        SearchListResponse searchListResponse = searchList.execute();
        return searchListResponse;
    }

    /**
     * プレイリストの検索を行い、検索結果を返す
     *
     * @param searchPlayList
     * @return
     * @throws IOException
     */
    public SearchListResponse searchPlaylist(YouTube.Search.List searchPlayList) throws IOException {
        searchPlayList.setKey(Config.YOUTUBE_API_KEY);
        searchPlayList.setType("playlist"); //TODO ADD PLAYLISTS SEARCH
        searchPlayList.setMaxResults(Config.NUMBER_OF_VIDEOS_RETURNED);
        searchPlayList.setFields("items(id/playlistId,snippet/title,snippet/thumbnails/default/url,id/kind)");
        searchPlayList.setQ(keywords);
        SearchListResponse searchPlayListResponse = searchPlayList.execute();
        Log.d(TAG, "searchPlayListResponse.size() is : " + searchPlayListResponse.getItems().size());
        return searchPlayListResponse;
    }


}
