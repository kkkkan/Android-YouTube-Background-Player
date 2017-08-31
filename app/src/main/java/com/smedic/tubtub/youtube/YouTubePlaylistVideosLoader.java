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
    private final static String TAG_NAME="kandabashi";
    private YouTube youtube = getInstance().getYouTubeWithCredentials();//getYouTube();
    private String playlistId;

    public YouTubePlaylistVideosLoader(Context context, String playlistId) {
        super(context);
        this.playlistId = playlistId;
    }

    @Override
    public List<YouTubeVideo> loadInBackground() {

        Log.d(TAG_NAME, "YTPVL-loadInBackground-1");
        List<PlaylistItem> playlistItemList = new ArrayList<>();
        List<YouTubeVideo> playlistItems = new ArrayList<>();
        String nextToken = "";
        // Retrieve the playlist of the channel's uploaded videos.
        YouTube.PlaylistItems.List playlistItemRequest;

        try {
            playlistItemRequest = youtube.playlistItems().list("id,contentDetails,snippet");
            playlistItemRequest.setPlaylistId(playlistId);
            Log.d(TAG_NAME, "YTPVL-loadInBackground-id:" + playlistId);
            //playlistItemRequest.setKey("ee");
            playlistItemRequest.setMaxResults(Config.NUMBER_OF_VIDEOS_RETURNED);
            /*videoid,動画タイトル、デフォルトのサムネイル画像のurlと次のページのトークンを取得*/
            playlistItemRequest.setFields("items(contentDetails/videoId,snippet/title,snippet/thumbnails/default/url)"/*,nextPageToken"*/);
            // Call API one or more times to retrieve all items in the list. As long as API
            // response returns a nextPageToken, there are still more items to retrieve.


            PlaylistItemListResponse playlistItemResult = playlistItemRequest.execute();

            for(PlaylistItem p:playlistItemResult.getItems()){
                Log.d(TAG_NAME,"title:"+p.getSnippet().getTitle());
            }

            playlistItemList.addAll(playlistItemResult.getItems());


            /*削除されたビデオも含めての数表示*/
            Log.d(TAG, "all items size: " + playlistItemList.size());
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


        //videos to get duration
        YouTube.Videos.List videosList = null;
        int ii = 0;
        try {
            videosList = youtube.videos().list("id,contentDetails");
            /*動画の長さを取ってくる*/
            videosList.setFields("items(contentDetails/duration)");

            //save all ids from searchList list in order to find video list
            StringBuilder contentDetails = new StringBuilder();

            /*videoIdをつなげたものをIdとしてvideosListにセット*/

            for (PlaylistItem result : playlistItemList) {
                contentDetails.append(result.getContentDetails().getVideoId());
                if (ii < playlistItemList.size() - 1)
                    contentDetails.append(",");
                ii++;
            }

            //find video list
            videosList.setId(contentDetails.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }

        VideoListResponse resp = null;
        try {

            /*ビデオidを基にをとってこようとする。削除されてるやつや非公開にされてしまったやつは特に通知もなく取らずにスルーしてくる？*/
            resp = videosList.execute();

        } catch (IOException e) {
            e.printStackTrace();
        }


        /*resp(videoList)の方は削除されたビデオは含まない。*/
        List<Video> videoResults = resp.getItems();
        /*50個videoIdを入れても、videolistでは50個全部取ってこれるとは限らないようである。*/
        /*<ーdeleted videoの含まれるリストを取得する際にちゃんととってこれないのはAPIバグ?
        * https://stackoverflow.com/questions/21189885/youtube-api-playlistitems-deleted-videos*/
        Log.d(TAG_NAME,String.valueOf(ii)+"NUMBER OF VIDEOS:"+videoResults.size());
        /*playlistItemListの方は削除されたビデオも要素として持ってしまってる。*/
        Iterator<PlaylistItem> pit = playlistItemList.iterator();
        Iterator<Video> vit = videoResults.iterator();


        /*50個videoIdを入れても、videolistでは50個全部取ってこれるとは限らないのでビデオの数に合わせる*/
        int count=0;
        while (vit.hasNext()) {
            PlaylistItem playlistItem = pit.next();
            Log.d(TAG_NAME,playlistItem.getSnippet().getTitle());
            YouTubeVideo youTubeVideo = new YouTubeVideo();
            youTubeVideo.setId(playlistItem.getContentDetails().getVideoId());
            youTubeVideo.setTitle(playlistItem.getSnippet().getTitle());

            ThumbnailDetails thumbnailDetails = playlistItem.getSnippet().getThumbnails();

            if (thumbnailDetails != null) {
                youTubeVideo.setThumbnailURL(playlistItem.getSnippet().getThumbnails().getDefault().getUrl());

                /*削除されたビデオではないときのみビデオも進める。*/
                Video videoItem = vit.next();
                //video info
                    String isoTime = videoItem.getContentDetails().getDuration();
                    String time = Utils.convertISO8601DurationToNormalTime(isoTime);
                    youTubeVideo.setDuration(time);
                    Log.d(TAG_NAME,String.valueOf(++count)+"-"+playlistItem.getSnippet().getTitle());
            }else{
                youTubeVideo.setThumbnailURL(null);
                youTubeVideo.setDuration("00:00");
                Log.d(TAG_NAME, String.valueOf(++count)+"-"+playlistItem.getSnippet().getTitle());
            }

            playlistItems.add(youTubeVideo);
            }
            Log.d(TAG_NAME, "YTPVL-loadInBackground-25");
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
