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

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.ThumbnailDetails;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.kkkkan.youtube.tubtub.model.YouTubePlaylist;
import com.kkkkan.youtube.tubtub.model.YouTubeVideo;
import com.kkkkan.youtube.tubtub.utils.Config;
import com.kkkkan.youtube.tubtub.utils.Utils;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.kkkkan.youtube.tubtub.youtube.YouTubeSingleton.getInstance;


/**
 * Created by smedic on 5.3.17..
 */

public class YouTubePlaylistVideosLoader extends AsyncTaskLoader<List<YouTubeVideo>> {

    private final static String TAG = "YouTubePlaylistVideosLo";
    private YouTube youtube = getInstance().getYouTubeWithCredentials();//getYouTube();
    private YouTubePlaylist playlist;

    public YouTubePlaylistVideosLoader(Context context, YouTubePlaylist playlist) {
        super(context);
        this.playlist = playlist;
    }

    @Override
    public List<YouTubeVideo> loadInBackground() {
        String playlistId = playlist.getId();
        long playlistItemCount = playlist.getNumberOfVideos();

        Log.d(TAG, "YTPVL-loadInBackground-1");
        List<PlaylistItem> playlistItemList = new ArrayList<>();
        List<YouTubeVideo> playlistItems = new ArrayList<>();
        // Retrieve the playlist of the channel's uploaded videos.
        YouTube.PlaylistItems.List playlistItemRequest;

        try {
            playlistItemRequest = youtube.playlistItems().list("id,contentDetails,snippet");
            playlistItemRequest.setPlaylistId(playlistId);
            Log.d(TAG, "YTPVL-loadInBackground-id:" + playlistId);
            //playlistItemRequest.setKey("ee");
            playlistItemRequest.setMaxResults(Config.NUMBER_OF_VIDEOS_RETURNED);
            //Get videoid, video title, default thumbnail image url and next page token
            //videoid,動画タイトル、デフォルトのサムネイル画像のurlと次のページのトークンを取得
            playlistItemRequest.setFields("items(contentDetails/videoId,snippet/title,snippet/thumbnails/default/url),nextPageToken"/*,nextPageToken"*/);
            // Call API one or more times to retrieve all items in the list. As long as API
            // response returns a nextPageToken, there are still more items to retrieve.


            PlaylistItemListResponse playlistItemResult = playlistItemRequest.execute();

            for (PlaylistItem p : playlistItemResult.getItems()) {
                // Log.d(TAG_NAME, "title:" + p.getSnippet().getTitle());
            }
            //Log.d(TAG, "nextPageToken is : " + playlistItemResult.getNextPageToken());

            playlistItemList.addAll(playlistItemResult.getItems());

            //Numerals including deleted videos
            //削除されたビデオも含めての数表示
            Log.d(TAG, "all items size: " + playlistItemList.size());

            //videos to get duration
            YouTube.Videos.List videosList = null;
            int ii = 0;

            videosList = youtube.videos().list("id,contentDetails");
            //Fetch the length of the video
            //動画の長さを取ってくる
            videosList.setFields("items(contentDetails/duration)");

            //save all ids from searchList list in order to find video list
            StringBuilder contentDetails = new StringBuilder();

            //Set the videosList as an Id by connecting the videoId to videosList
            //videoIdをつなげたものをIdとしてvideosListにセット

            for (PlaylistItem result : playlistItemList) {
                contentDetails.append(result.getContentDetails().getVideoId());
                if (ii < playlistItemList.size() - 1)
                    contentDetails.append(",");
                ii++;
            }

            //find video list
            videosList.setId(contentDetails.toString());

            VideoListResponse resp = null;
            //I try to take it based on the video id.
            //Are those who have been deleted or those that have been made undocumented go through without notice without any particular notice?
            //ビデオidを基にをとってこようとする。削除されてるやつや非公開にされてしまったやつは特に通知もなく取らずにスルーしてくる？
            resp = videosList.execute();


            //resp (videoList) does not include deleted videos.
            //resp(videoList)の方は削除されたビデオは含まない。
            List<Video> videoResults = resp.getItems();
            //Even if 50 videoIds are inserted, it seems that 50 videolists do not always get them all.
            //Is it an API bug that can not get it properly when getting a list containing deleted video?
            //https://stackoverflow.com/questions/21189885/youtube-api-playlistitems-deleted-videos
            //50個videoIdを入れても、videolistでは50個全部取ってこれるとは限らないようである。
            //<ーdeleted videoの含まれるリストを取得する際にちゃんととってこれないのはAPIバグ?
            // https://stackoverflow.com/questions/21189885/youtube-api-playlistitems-deleted-videos
            //Log.d(TAG, String.valueOf(ii) + "NUMBER OF VIDEOS:" + videoResults.size());
            //The player of playlistItemList also has the deleted video as an element
            //playlistItemListの方は削除されたビデオも要素として持ってしまってる。
            Iterator<PlaylistItem> pit = playlistItemList.iterator();
            Iterator<Video> vit = videoResults.iterator();

            //Even if 50 videoIds are inserted, 50 videos can not always be taken in videolist, so it matches the number of videos
            //50個videoIdを入れても、videolistでは50個全部取ってこれるとは限らないのでビデオの数に合わせる
            int count = 0;
            while (vit.hasNext()) {
                PlaylistItem playlistItem = pit.next();
                // Log.d(TAG_NAME, playlistItem.getSnippet().getTitle());
                YouTubeVideo youTubeVideo = new YouTubeVideo();
                youTubeVideo.setId(playlistItem.getContentDetails().getVideoId());
                youTubeVideo.setTitle(playlistItem.getSnippet().getTitle());
                ThumbnailDetails thumbnailDetails = playlistItem.getSnippet().getThumbnails();

                if (thumbnailDetails != null) {
                    youTubeVideo.setThumbnailURL(playlistItem.getSnippet().getThumbnails().getDefault().getUrl());

                    //You can also advance the video only when it is not a deleted video.
                    //削除されたビデオではないときのみビデオも進める。
                    Video videoItem = vit.next();
                    //video info
                    String isoTime = videoItem.getContentDetails().getDuration();
                    String time = Utils.convertISO8601DurationToNormalTime(isoTime);
                    youTubeVideo.setDuration(time);
                    //Log.d(TAG_NAME, String.valueOf(++count) + "-" + playlistItem.getSnippet().getTitle());
                } else {
                    youTubeVideo.setThumbnailURL(null);
                    youTubeVideo.setDuration("00:00");
                    //Log.d(TAG_NAME, String.valueOf(++count) + "-" + playlistItem.getSnippet().getTitle());
                }

                playlistItems.add(youTubeVideo);
                String nextToken = playlistItemResult.getNextPageToken();
                if (nextToken == null || nextToken.equals("")) {

                }
            }
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                Log.d(TAG, "loadInBackground: 404 error");
                return Collections.emptyList();
            } else {
                e.printStackTrace();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return Collections.emptyList();
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
        //Log.d(TAG_NAME, "YTPVL-loadInBackground-25");
        return playlistItems;
    }


    @Override
    public void deliverResult(List<YouTubeVideo> data) {
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            return;
        }
        super.deliverResult(data);
    }

    private List<YouTubePlaylist> startMakePlaylistDetail() {
        return null;
    }

    private YouTube.PlaylistItems.List makePlaylistItemsRequest(String playlistId) {
        YouTube.PlaylistItems.List playlistItemRequest = null;
        try {
            playlistItemRequest = youtube.playlistItems().list("id,contentDetails,snippet");

            playlistItemRequest.setPlaylistId(playlistId);
            //playlistItemRequest.setKey("ee");
            playlistItemRequest.setMaxResults(Config.NUMBER_OF_VIDEOS_RETURNED);
            //Get videoid, video title, default thumbnail image url and next page token
            //videoid,動画タイトル、デフォルトのサムネイル画像のurlと次のページのトークンを取得
            playlistItemRequest.setFields("items(contentDetails/videoId,snippet/title,snippet/thumbnails/default/url),nextPageToken"/*,nextPageToken"*/);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return playlistItemRequest;
    }

    private YouTube.PlaylistItems.List makePlaylistItemsRequest(String playlistId, String nextPageToken) {
        YouTube.PlaylistItems.List playlistItemRequest = null;
        try {
            playlistItemRequest = youtube.playlistItems().list("id,contentDetails,snippet");

            playlistItemRequest.setPlaylistId(playlistId);
            //playlistItemRequest.setKey("ee");
            playlistItemRequest.setMaxResults(Config.NUMBER_OF_VIDEOS_RETURNED);
            //Get videoid, video title, default thumbnail image url and next page token
            //videoid,動画タイトル、デフォルトのサムネイル画像のurlと次のページのトークンを取得
            playlistItemRequest.setFields("items(contentDetails/videoId,snippet/title,snippet/thumbnails/default/url),nextPageToken"/*,nextPageToken"*/);

            playlistItemRequest.setPageToken(nextPageToken);


        } catch (IOException e) {
            e.printStackTrace();
        }
        return playlistItemRequest;
    }


}
