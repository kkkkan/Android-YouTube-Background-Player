package com.kkkkan.youtube.tubtub.youtube

import android.util.Log
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.*
import com.kkkkan.youtube.tubtub.model.YouTubePlaylist
import com.kkkkan.youtube.tubtub.model.YouTubeVideo
import com.kkkkan.youtube.tubtub.utils.Config
import com.kkkkan.youtube.tubtub.utils.Utils
import com.kkkkan.youtube.tubtub.utils.Utils.putPlaylistsToArrayList
import java.io.IOException
import java.math.BigInteger
import java.text.NumberFormat


private val TAG: String = "VideosLoaderMethods"


/**
 * 検索Typeをvideoにしたときの検索結果のリスト(List<SearchResult>)から検索結果のそれぞれのvideoの詳細情報の取得をし List<YouTubeVideo>を返す
 *
 * @param videosList
 * @param searchResults
 * @return
 * @throws IOException
 */
public fun getVideoList(videoList: YouTube.Videos.List, searchResults: List<SearchResult>): List<YouTubeVideo> {
    //kotlinのListは読むの専用。
    //後から何か足したりしたかったら(addとか使いたかったら)MutableListにする必要がある。
    var items: MutableList<YouTubeVideo> = ArrayList<YouTubeVideo>()

    videoList.key = Config.YOUTUBE_API_KEY
    videoList.fields = "items(id,contentDetails/duration,statistics/viewCount)"
    //find video list
    videoList.id = Utils.concatenateIDs(searchResults)
    var resp: VideoListResponse = videoList.execute()
    var videoResults: List<Video> = resp.items

    Log.d(TAG, "YouTube.Search.List execute result size is : " + searchResults.size)
    Log.d(TAG, "YouTube.Videos.List execute result size is : " + videoList.size)


    //どうやらYouTube.Search.Listは削除されたビデオもとってきて、
    // YouTube.Videos.Listはとってきた50個のうち削除されたビデオはスルーして、userにデータは返してくれないようだ
    //（つまり50個中1個削除されたビデオがあったら、searchResults.size()は50,videoResults.size()は49）

    var j: Int = 0
    //kotlin:for(i in 0 until 5) -> java:for(int i=0 ;i<5;i++)
    for (i in 0 until searchResults.size) {
        val youTubeVideo: YouTubeVideo = YouTubeVideo()

        val searchResult: SearchResult = searchResults[i]
        val videoResult: Video = videoResults[i]


        youTubeVideo.title = searchResult.snippet.title
        youTubeVideo.id = searchResult.id.videoId

        if (searchResult.id.videoId.equals(videoResult.id)) {
            //削除されたビデオではないとき
            //jをインクリメント

            j++

            //thumbnailDetailsのurlをセット
            youTubeVideo.thumbnailURL = searchResult.snippet.thumbnails.default.url

            //video由来の情報もyouTubeVideoに追加
            if (videoResult.statistics != null) {
                val viewNumber: BigInteger = videoResult.statistics.viewCount
                val viewFormatter: String = NumberFormat.getIntegerInstance().format(viewNumber) + " views"
                youTubeVideo.viewCount = viewFormatter
            }
            if (videoResult.contentDetails != null) {
                val isoTime: String = videoResult.contentDetails.duration
                val time: String = Utils.convertISO8601DurationToNormalTime(isoTime)
                youTubeVideo.duration = time
            }
        } else {
            //削除されたビデオの時
            //Log.d(TAG, String.valueOf(++count) + " : " + searchResult.getSnippet().getTitle() + " : Deleted");
            //削除されたビデオの場合はthumnailURLにnull、durationに"00:00"を入れる

            youTubeVideo.thumbnailURL = null
            youTubeVideo.duration = "00:00"
        }
        items.add(youTubeVideo)
    }
    return items
}


public fun getPlaylistList(playlistList: YouTube.Playlists.List, searchPlayListResults: List<SearchResult>): List<YouTubePlaylist> {
    val playlistItems: List<YouTubePlaylist> = ArrayList<YouTubePlaylist>()
    playlistList.key = Config.YOUTUBE_API_KEY
    playlistList.fields = "items(id,snippet/title,snippet/thumbnails/default/url,contentDetails/itemCount,status)"
    playlistList.maxResults = Config.NUMBER_OF_VIDEOS_RETURNED
    playlistList.id = Utils.concatenatePlaylistIDs(searchPlayListResults)
    val playlistListResponse: PlaylistListResponse = playlistList.execute()

    val playlists: List<Playlist> = playlistListResponse.items

    if (playlists != null) {
        //自分で作った再生リストを返り値用のArrayListに入れる
        putPlaylistsToArrayList(playlistItems, playlists)
    }

    return playlistItems
}

public class VideosLoaderMethods {
    class SearchResultVideo(resultVideos: List<YouTubeVideo>, nextPageToken: String?) {
        val resultVideos: List<YouTubeVideo> = resultVideos
        //最終ページの時はnextPageToken==nullであることに注意
        val nextPageToken: String? = nextPageToken

    }

    class SearchResultPlaylist(resultPlaylists: List<YouTubePlaylist>, nextPageToken: String?) {
        public val resultPlaylists: List<YouTubePlaylist> = resultPlaylists
        //最終ページの時はnextPageToken==nullであることに注意
        public val nextPageToken: String? = nextPageToken
    }
}