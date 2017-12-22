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
        String nextPageToken;

        try {
            playlistItemRequest = makeRequest(playlistId);
            // Call API one or more times to retrieve all items in the list. As long as API
            // response returns a nextPageToken, there are still more items to retrieve.
            PlaylistItemListResponse playlistItemResult = playlistItemRequest.execute();
            //playlistItemList.addAll(playlistItemResult.getItems());

            //51個以上のビデオがプレイリストに登録されていた時のためにNext Page Tokenを取得しておく。
            nextPageToken = playlistItemResult.getNextPageToken();

            //Numerals including deleted videos
            //削除されたビデオも含めての数表示
            Log.d(TAG, "all items size: " + playlistItemResult.getItems().size());

            //ビデオの長さなどの情報も付け加えて、 List<YouTubeVideo>を作る
            List<YouTubeVideo> result = makeVideosList(playlistItemResult.getItems());
            //return するオブジェクトに List<YouTubeVideo>を追加
            playlistItems.addAll(result);


            //Next Pageがある限り同じ事を繰り返してreturn するオブジェクトに List<YouTubeVideo>を追加していく。
            while (nextPageToken != null && !nextPageToken.equals("")) {
                playlistItemRequest = makeRequest(playlistId, nextPageToken);
                // Call API one or more times to retrieve all items in the list. As long as API
                // response returns a nextPageToken, there are still more items to retrieve.
                playlistItemResult = playlistItemRequest.execute();
                // playlistItemList.addAll(playlistItemResult.getItems());
                //まだ次のページがあったときのためにNext Page Tokenを取っておく
                nextPageToken = playlistItemResult.getNextPageToken();
                Log.d(TAG, "all items size: " + playlistItemResult.getItems().size());

                result = makeVideosList(playlistItemResult.getItems());
                playlistItems.addAll(result);
            }
        } catch (GoogleJsonResponseException e) {
            Log.d(TAG, "GoogleJsonResponseException : " + e.getMessage() + "\n" + e.getLocalizedMessage());
            if (e.getStatusCode() == 404) {
                Log.d(TAG, "loadInBackground: 404 error");
                return Collections.emptyList();
            } else {
                e.printStackTrace();
            }
        } catch (UnknownHostException e) {
            Log.d(TAG, "UnknownHostException : " + e.getMessage() + "\n" + e.getLocalizedMessage());
            e.printStackTrace();
            return Collections.emptyList();
        } catch (IOException e) {
            Log.d(TAG, "IOException : " + e.getMessage() + "\n" + e.getLocalizedMessage());
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

    /**
     * 1ページ目（最初の50ビデオ）を取得するためのリクエストを作るメゾッド
     *
     * @param playlistId
     * @return
     * @throws IOException
     */
    private YouTube.PlaylistItems.List makeRequest(String playlistId) throws IOException {
        YouTube.PlaylistItems.List request;
        request = youtube.playlistItems().list("id,contentDetails,snippet");
        request.setPlaylistId(playlistId);
        request.setMaxResults(Config.NUMBER_OF_VIDEOS_RETURNED);
        //Get videoid, video title, default thumbnail image url and next page token
        //videoid,動画タイトル、デフォルトのサムネイル画像のurlと次のページのトークンを取得
        request.setFields("items(contentDetails/videoId,snippet/title,snippet/thumbnails/default/url),nextPageToken"/*,nextPageToken"*/);

        return request;
    }

    /**
     * 2ページ目以降（51個目以降のビデオ）を取得するためのリクエストを作るメゾッド
     *
     * @param playlistId
     * @param nextPageToken
     * @return
     * @throws IOException
     */
    private YouTube.PlaylistItems.List makeRequest(String playlistId, String nextPageToken) throws IOException {
        YouTube.PlaylistItems.List request = makeRequest(playlistId);
        request.setPageToken(nextPageToken);

        return request;
    }

    /**
     * Videos#List apiを用いて動画の長さなどを取ってきて、引数のList<PlaylistItem>との情報を合わせて、
     * List<YouTubeVideo>を作って返すメゾッド
     *
     * @param playlistItemList
     * @return
     * @throws IOException
     */
    private List<YouTubeVideo> makeVideosList(List<PlaylistItem> playlistItemList) throws IOException {
        List<YouTubeVideo> playlistItems = new ArrayList<>();
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
            if (ii < playlistItemList.size() - 1) {
                contentDetails.append(",");
            }
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
            count++;
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
        }
        //videolistは終わっていてもまだplaylistitemsが残っている（リストの最後の方にdeleted videoなどがあったとき）
        while (pit.hasNext()) {
            PlaylistItem playlistItem = pit.next();
            // Log.d(TAG_NAME, playlistItem.getSnippet().getTitle());
            YouTubeVideo youTubeVideo = new YouTubeVideo();
            youTubeVideo.setId(playlistItem.getContentDetails().getVideoId());
            youTubeVideo.setTitle(playlistItem.getSnippet().getTitle());
            count++;
            ThumbnailDetails thumbnailDetails = playlistItem.getSnippet().getThumbnails();

            if (thumbnailDetails != null) {
                youTubeVideo.setThumbnailURL(playlistItem.getSnippet().getThumbnails().getDefault().getUrl());
            } else {
                youTubeVideo.setThumbnailURL(null);
                youTubeVideo.setDuration("00:00");
            }
            //Log.d(TAG_NAME, String.valueOf(++count) + "-" + playlistItem.getSnippet().getTitle());
            playlistItems.add(youTubeVideo);
        }
        return playlistItems;
    }
}
