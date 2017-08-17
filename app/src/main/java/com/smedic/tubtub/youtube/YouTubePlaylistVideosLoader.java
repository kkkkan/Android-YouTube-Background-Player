package com.smedic.tubtub.youtube;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.google.api.services.youtube.model.Thumbnail;
import com.google.api.services.youtube.model.ThumbnailDetails;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.smedic.tubtub.model.YouTubeVideo;
import com.smedic.tubtub.utils.Config;
import com.smedic.tubtub.utils.Utils;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.smedic.tubtub.youtube.YouTubeSingleton.getInstance;
import static com.smedic.tubtub.youtube.YouTubeSingleton.getYouTubeWithCredentials;

/**
 * Created by smedic on 5.3.17..
 */

public class YouTubePlaylistVideosLoader extends AsyncTaskLoader<List<YouTubeVideo>> {

    private final static String TAG = "SMEDIC";
    private YouTube youtube = getInstance().getYouTubeWithCredentials();//getYouTube();
    private String playlistId;

    public YouTubePlaylistVideosLoader(Context context, String playlistId) {
        super(context);
        this.playlistId = playlistId;
    }

    @Override
    public List<YouTubeVideo> loadInBackground() {

        Log.d("kandabashi", "YTPVL-loadInBackground-1");
        List<PlaylistItem> playlistItemList = new ArrayList<>();
        List<YouTubeVideo> playlistItems = new ArrayList<>();
        String nextToken = "";
        // Retrieve the playlist of the channel's uploaded videos.
        YouTube.PlaylistItems.List playlistItemRequest;
        Log.d("kandabashi", "YTPVL-loadInBackground-2");
        try {
            playlistItemRequest = youtube.playlistItems().list("id,contentDetails,snippet");
            playlistItemRequest.setPlaylistId(playlistId);
            Log.d("kandabashi", "YTPVL-loadInBackground-id:" + playlistId);
            //playlistItemRequest.setKey("ee");
            playlistItemRequest.setMaxResults(Config.NUMBER_OF_VIDEOS_RETURNED);
            playlistItemRequest.setFields("items(contentDetails/videoId,snippet/title,snippet/thumbnails/default/url),nextPageToken");
            // Call API one or more times to retrieve all items in the list. As long as API
            // response returns a nextPageToken, there are still more items to retrieve.
            // do {
            // playlistItemRequest.setPageToken(nextToken);
            Log.d("kandabashi", "YTPVL-loadInBackground-3");
            PlaylistItemListResponse playlistItemResult = playlistItemRequest.execute();
            Log.d("kandabashi", "YTPVL-loadInBackground-4");
            //List  str= playlistItemResult.getItems();
            playlistItemList.addAll(playlistItemResult.getItems());
            //nextToken = playlistItemResult.getNextPageToken();
            //} while (nextToken != null);

            /*削除されたビデオも含めての数表示*/
            Log.d(TAG, "all items size: " + playlistItemList.size());
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                //youTubeVideosReceiver.onPlaylistNotFound(playlistId, e.getStatusCode());
                Log.d(TAG, "loadInBackground: 404 error");
                return Collections.emptyList();
            } else {
                e.printStackTrace();
            }
        } catch (UnknownHostException e) {
            //Toast.makeText(activity.getApplicationContext(), "Check internet connection", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return Collections.emptyList();
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

        Log.d("kandabashi", "YTPVL-loadInBackground-5");
        //videos to get duration
        YouTube.Videos.List videosList = null;
        Log.d("kandabashi", "YTPVL-loadInBackground-6");
        try {
            videosList = youtube.videos().list("id,contentDetails");
            //videosList.setKey("ee");
            videosList.setFields("items(contentDetails/duration)");
            Log.d("kandabashi", "YTPVL-loadInBackground-7");
            //save all ids from searchList list in order to find video list
            StringBuilder contentDetails = new StringBuilder();

            int ii = 0;
            for (PlaylistItem result : playlistItemList) {
                //String aa = result.getContentDetails().getVideoId();
                contentDetails.append(result.getContentDetails().getVideoId());
                if (ii < playlistItemList.size() - 1)
                    contentDetails.append(",");
                ii++;
            }
            Log.d("kandabashi", "YTPVL-loadInBackground-8");
            //find video list
            videosList.setId(contentDetails.toString());
            Log.d("kandabashi", "YTPVL-loadInBackground-9");
        } catch (IOException e) {
            e.printStackTrace();
        }

        VideoListResponse resp = null;
        try {
            Log.d("kandabashi", "YTPVL-loadInBackground-10");
            /*ビデオidを基にをとってこようとする。削除されてるやつや非公開にされてしまったやつは特に通知もなく取らずにスルーしてくる？*/
            resp = videosList.execute();
            Log.d("kandabashi", "YTPVL-loadInBackground-11");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("kandabashi", "YTPVL-loadInBackground-12");
        /*resp(videoList)の方は削除されたビデオは含まない。*/
        List<Video> videoResults = resp.getItems();
        /*playlistItemListの方は削除されたビデオも要素として持ってしまってる。*/
        Iterator<PlaylistItem> pit = playlistItemList.iterator();
        Iterator<Video> vit = videoResults.iterator();
        Log.d("kandabashi", "YTPVL-loadInBackground-13");
        /*ビデオの数に合わせる*/
        while (vit.hasNext()) {
            Log.d("kandabashi", "YTPVL-loadInBackground-14");
            PlaylistItem playlistItem = pit.next();
            Log.d("kandabashi", "YTPVL-loadInBackground-16");
            YouTubeVideo youTubeVideo = new YouTubeVideo();
            Log.d("kandabashi", "YTPVL-loadInBackground-17");
            youTubeVideo.setId(playlistItem.getContentDetails().getVideoId());
            Log.d("kandabashi", "YTPVL-loadInBackground-18");
            youTubeVideo.setTitle(playlistItem.getSnippet().getTitle());
           /* Log.d("kandabashi", "YTPVL-loadInBackground-19");
            playlistItem.getSnippet();
            Log.d("kandabashi", "YTPVL-loadInBackground-19-1");*/
            ThumbnailDetails thumbnailDetails = playlistItem.getSnippet().getThumbnails();
            Log.d("kandabashi", "YTPVL-loadInBackground-19-2");
            if (thumbnailDetails != null) {
               /* Thumbnail a = playlistItem.getSnippet().getThumbnails().getDefault();
                Log.d("kandabashi", "YTPVL-loadInBackground-19-3");
                playlistItem.getSnippet().getThumbnails().getDefault().getUrl();
                Log.d("kandabashi", "YTPVL-loadInBackground-19-4");*/
                youTubeVideo.setThumbnailURL(playlistItem.getSnippet().getThumbnails().getDefault().getUrl());

                /*削除されたビデオではないときのみビデオも進める。*/
                Log.d("kandabashi", "YTPVL-loadInBackground-15");
                Video videoItem = vit.next();
                Log.d("kandabashi", "YTPVL-loadInBackground-20");
                //video info
                /*ビデオがnullなことはあり得ない。*/
               /* if (videoItem != null) {*/
                    Log.d("kandabashi", "YTPVL-loadInBackground-21");
                    String isoTime = videoItem.getContentDetails().getDuration();
                    Log.d("kandabashi", "YTPVL-loadInBackground-22");
                    String time = Utils.convertISO8601DurationToNormalTime(isoTime);
                    Log.d("kandabashi", "YTPVL-loadInBackground-23");
                    youTubeVideo.setDuration(time);
                    Log.d("kandabashi", "YTPVL-loadInBackground-24");
               /* } else {
                    Log.d("kandabashi", "YTPVL-loadInBackground-14");
                    youTubeVideo.setDuration("NA");
                }*/
                playlistItems.add(youTubeVideo);
            }

            }
            Log.d("kandabashi", "YTPVL-loadInBackground-25");
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
}
