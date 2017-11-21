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
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.kkkkan.youtube.tubtub.model.YouTubeVideo;
import com.kkkkan.youtube.tubtub.utils.Config;
import com.kkkkan.youtube.tubtub.utils.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.List;

import static com.kkkkan.youtube.tubtub.youtube.YouTubeSingleton.getYouTube;

/**
 * When you receive a share, fetch data such as duration from that url, make YouTubeVideo and erase it Loader
 * 共有を受け取ったときにそのurlからduration等のデータを取ってきてYouTubeVideoを作って消すLoader
 * Created by admin on 2017/11/21.
 */

public class YouTubeShareVideoGetLoader extends AsyncTaskLoader<YouTubeVideo> {
    final private String TAG = "YouTubeShareVideoGetLoa";
    private YouTube youtube = getYouTube();
    private String url;

    public YouTubeShareVideoGetLoader(Context context, String url) {
        super(context);
        this.url = url;
    }

    @Override
    public YouTubeVideo loadInBackground() {
        String videoId = getVideoId(url);
        Log.d(TAG, "videoId : " + videoId);
        YouTubeVideo item = new YouTubeVideo();
        try {
            YouTube.Videos.List videosList = youtube.videos().list("id,contentDetails,statistics,snippet");

            videosList.setKey(Config.YOUTUBE_API_KEY);
            videosList.setFields("items(snippet/title,snippet/thumbnails/default/url,contentDetails/duration,statistics/viewCount)");

            //find video list
            videosList.setId(videoId);  //save all ids from searchList list in order to find video list
            VideoListResponse resp = videosList.execute();
            List<Video> videoResults = resp.getItems();
            //make items for displaying in listView
            if (videoResults.size() != 1) {
                Log.d(TAG, "videoResults.size() != 1\nvideoResults.size():" + String.valueOf(videoResults.size()));
                return null;
            }
            Video video = videoResults.get(0);

            //searchList list info
            item.setTitle(video.getSnippet().getTitle());
            item.setThumbnailURL(video.getSnippet().getThumbnails().getDefault().getUrl());
            item.setId(videoId);
            //video info
            if (video.getStatistics() != null) {
                BigInteger viewsNumber = video.getStatistics().getViewCount();
                String viewsFormatted = NumberFormat.getIntegerInstance().format(viewsNumber) + " views";
                item.setViewCount(viewsFormatted);
            }
            if (video.getContentDetails() != null) {
                String isoTime = video.getContentDetails().getDuration();
                String time = Utils.convertISO8601DurationToNormalTime(isoTime);
                item.setDuration(time);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return item;
    }


    /**
     * Cut out only the videoId from url and return it
     * urlからvideoIdだけ切り抜いて返します
     *
     * @param url
     * @return
     */
    private String getVideoId(String url) {
        String videoId = url.replace(Config.YOUTUBE_BASE_URL, "");
        videoId = videoId.replace(Config.YOUTUBE_BASE_SHARE, "");
        return videoId;
    }


}
