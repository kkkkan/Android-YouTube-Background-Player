package com.kkkkan.youtube.tubtub.youtube;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.kkkkan.youtube.tubtub.model.YouTubePlaylist;
import com.kkkkan.youtube.tubtub.model.YouTubeVideo;
import com.kkkkan.youtube.tubtub.utils.Config;
import com.kkkkan.youtube.tubtub.utils.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import static com.kkkkan.youtube.tubtub.utils.Utils.putPlaylistsToArrayList;

public class VideosLoaderMethods {
    static private final String TAG = "VideosLoaderMethods";


    /**
     * 検索Typeをvideoにしたときの検索結果のリスト(List<SearchResult>)から検索結果のそれぞれのvideoの詳細情報の取得をし List<YouTubeVideo>を返す
     *
     * @param videosList
     * @param searchResults
     * @return
     * @throws IOException
     */
    static public List<YouTubeVideo> getVideoList(YouTube.Videos.List videosList, List<SearchResult> searchResults) throws IOException {
        List<YouTubeVideo> items = new ArrayList<>();

        videosList.setKey(Config.YOUTUBE_API_KEY);
        videosList.setFields("items(id,contentDetails/duration,statistics/viewCount)");
        //find video list
        videosList.setId(Utils.concatenateIDs(searchResults));  //save all ids from searchList list in order to find video list
        VideoListResponse resp = videosList.execute();
        List<Video> videoResults = resp.getItems();
        //make items for displaying in listView

        Log.d(TAG, "YouTube.Search.List execute result size is : " + searchResults.size());
        Log.d(TAG, "YouTube.Videos.List execute result size is : " + videoResults.size());

        //どうやらYouTube.Search.Listは削除されたビデオもとってきて、
        // YouTube.Videos.Listはとってきた50個のうち削除されたビデオはスルーして、userにデータは返してくれないようだ
        //（つまり50個中1個削除されたビデオがあったら、searchResults.size()は50,videoResults.size()は49）

        int count = 0;
        for (int i = 0, j = 0; i < searchResults.size(); i++) {
            YouTubeVideo youTubeVideo = new YouTubeVideo();

            SearchResult searchResult = searchResults.get(i);
            Video videoResult = videoResults.get(j);

            youTubeVideo.setTitle(searchResult.getSnippet().getTitle());
            youTubeVideo.setId(searchResult.getId().getVideoId());
            if (searchResult.getId().getVideoId().equals(videoResult.getId())) {
                //Log.d(TAG, String.valueOf(++count) + " : " + searchResult.getSnippet().getTitle() + " : not Deleted");
                //削除されたビデオではないとき
                //jをインクリメント
                j++;
                //thumbnailDetailsのurlをセット
                youTubeVideo.setThumbnailURL(searchResult.getSnippet().getThumbnails().getDefault().getUrl());
                //video由来の情報もyouTubeVideoに追加
                if (videoResult.getStatistics() != null) {
                    BigInteger viewsNumber = videoResult.getStatistics().getViewCount();
                    String viewsFormatted = NumberFormat.getIntegerInstance().format(viewsNumber) + " views";
                    youTubeVideo.setViewCount(viewsFormatted);
                }
                if (videoResult.getContentDetails() != null) {
                    String isoTime = videoResult.getContentDetails().getDuration();
                    String time = Utils.convertISO8601DurationToNormalTime(isoTime);
                    youTubeVideo.setDuration(time);
                }
            } else {
                //削除されたビデオの時
                //Log.d(TAG, String.valueOf(++count) + " : " + searchResult.getSnippet().getTitle() + " : Deleted");
                //削除されたビデオの場合はthumnailURLにnull、durationに"00:00"を入れる
                youTubeVideo.setThumbnailURL(null);
                youTubeVideo.setDuration("00:00");
            }
            items.add(youTubeVideo);
        }
        return items;
    }


    /**
     * 検索Typeをplaylistにしたときの検索結果のリスト(List<SearchResult>)から検索結果のそれぞれのplaylistの詳細情報の取得をし List<YouTubePlaylist>を返す
     *
     * @param playlistList
     * @param searchPlayListResults
     * @return
     * @throws IOException
     */
    static public List<YouTubePlaylist> getPlaylistList(YouTube.Playlists.List playlistList, List<SearchResult> searchPlayListResults) throws IOException {
        List<YouTubePlaylist> playlistItems = new ArrayList<>();
        playlistList.setKey(Config.YOUTUBE_API_KEY);
        playlistList.setFields("items(id,snippet/title,snippet/thumbnails/default/url,contentDetails/itemCount,status)");
        playlistList.setMaxResults(Config.NUMBER_OF_VIDEOS_RETURNED);
        playlistList.setId(Utils.concatenatePlaylistIDs(searchPlayListResults));
        PlaylistListResponse playlistListResponse = playlistList.execute();

        List<Playlist> playlists = playlistListResponse.getItems();

        Log.d(TAG, "playlists.size() is : " + playlists.size());

        if (playlists != null) {
            //自分で作った再生リストを返り値用のArrayListに入れる
            putPlaylistsToArrayList(playlistItems, playlists);
        }

        return playlistItems;
    }

    static public class SearchResultVideo {
        final public List<YouTubeVideo> resultVideos;
        //最終ページの時はnextPageToken==nullであることに注意
        @Nullable
        final public String nextPageToken;

        public SearchResultVideo(List<YouTubeVideo> resultVideos, @Nullable String nextPageToken) {
            this.resultVideos = resultVideos;
            this.nextPageToken = nextPageToken;
        }
    }

    static public class SearchResultPlaylist {
        final public List<YouTubePlaylist> resultPlaylists;
        //最終ページの時はnextPageToken==nullであることに注意
        @Nullable
        final public String nextPageToken;

        public SearchResultPlaylist(List<YouTubePlaylist> resultPlaylists, @Nullable String nextPageToken) {
            this.resultPlaylists = resultPlaylists;
            this.nextPageToken = nextPageToken;
        }
    }

}
