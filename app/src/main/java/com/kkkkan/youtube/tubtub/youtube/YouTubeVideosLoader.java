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

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.kkkkan.youtube.tubtub.model.YouTubeVideo;
import com.kkkkan.youtube.tubtub.utils.Config;
import com.kkkkan.youtube.tubtub.utils.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import static com.kkkkan.youtube.tubtub.youtube.YouTubeSingleton.getYouTube;

/**
 * Created by smedic on 13.2.17..
 */

public class YouTubeVideosLoader extends AsyncTaskLoader<List<YouTubeVideo>> {

    private YouTube youtube = getYouTube();
    private String keywords;

    public YouTubeVideosLoader(Context context, String keywords) {
        super(context);
        this.keywords = keywords;
    }

    @Override
    public List<YouTubeVideo> loadInBackground() {

        ArrayList<YouTubeVideo> items = new ArrayList<>();
        try {
            YouTube.Search.List searchList = youtube.search().list("id,snippet");
            YouTube.Videos.List videosList = youtube.videos().list("id,contentDetails,statistics");

            searchList.setKey(Config.YOUTUBE_API_KEY);
            searchList.setType("video"); //TODO ADD PLAYLISTS SEARCH
            searchList.setMaxResults(Config.NUMBER_OF_VIDEOS_RETURNED);
            searchList.setFields("items(id/videoId,snippet/title,snippet/thumbnails/default/url)");

            videosList.setKey(Config.YOUTUBE_API_KEY);
            videosList.setFields("items(contentDetails/duration,statistics/viewCount)");

            //search
            searchList.setQ(keywords);
            SearchListResponse searchListResponse = searchList.execute();
            List<SearchResult> searchResults = searchListResponse.getItems();

            //find video list
            videosList.setId(Utils.concatenateIDs(searchResults));  //save all ids from searchList list in order to find video list
            VideoListResponse resp = videosList.execute();
            List<Video> videoResults = resp.getItems();
            //make items for displaying in listView
            for (int i = 0; i < searchResults.size(); i++) {
                YouTubeVideo item = new YouTubeVideo();
                //searchList list info
                item.setTitle(searchResults.get(i).getSnippet().getTitle());
                item.setThumbnailURL(searchResults.get(i).getSnippet().getThumbnails().getDefault().getUrl());
                item.setId(searchResults.get(i).getId().getVideoId());
                //video info
                if (videoResults.get(i) != null) {
                    if (videoResults.get(i).getStatistics() != null) {
                        BigInteger viewsNumber = videoResults.get(i).getStatistics().getViewCount();
                        String viewsFormatted = NumberFormat.getIntegerInstance().format(viewsNumber) + " views";
                        item.setViewCount(viewsFormatted);
                    }
                    if (videoResults.get(i).getContentDetails() != null) {
                        String isoTime = videoResults.get(i).getContentDetails().getDuration();
                        String time = Utils.convertISO8601DurationToNormalTime(isoTime);
                        item.setDuration(time);
                    }
                } else {
                    item.setDuration("");
                }

                //add to the list
                items.add(item);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("SMEDIC", "loadInBackground: return " + items.size());
        return items;
    }

    @Override
    public void deliverResult(List<YouTubeVideo> data) {
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            return;
        }
        super.deliverResult(data);
    }
}
