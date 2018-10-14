package com.kkkkan.youtube.tubtub.youtube

import android.util.Log
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.PlaylistListResponse
import com.google.api.services.youtube.model.SearchResult
import com.kkkkan.youtube.tubtub.model.YouTubePlaylist
import com.kkkkan.youtube.tubtub.model.YouTubeVideo
import com.kkkkan.youtube.tubtub.utils.Config
import com.kkkkan.youtube.tubtub.utils.Utils
import com.kkkkan.youtube.tubtub.utils.Utils.putPlaylistsToArrayList
import java.io.IOException
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
    // 後から何か足したりしたかったら(addとか使いたかったら)MutableListにする必要がある。
    val items: MutableList<YouTubeVideo> = ArrayList()

    videoList.key = Config.YOUTUBE_API_KEY
    videoList.fields = "items(id,contentDetails/duration,statistics/viewCount)"
    //find video list
    videoList.id = Utils.concatenateIDs(searchResults)
    val resp = videoList.execute()
    val videoResults = resp.items

    Log.d(TAG, "YouTube.Search.List execute result size is : " + searchResults.size)
    Log.d(TAG, "YouTube.Videos.List execute result size is : " + videoList.size)


    // どうやらYouTube.Search.Listは削除されたビデオもとってきて、
    // YouTube.Videos.Listはとってきた50個のうち削除されたビデオはスルーして、userにデータは返してくれないようだ
    //（つまり50個中1個削除されたビデオがあったら、searchResults.size()は50,videoResults.size()は49）

    // videoList用のindex
    var videoIndex: Int = 0
    // kotlin:for(i in 0 until 5) -> java:for(int i=0 ;i<5;i++)
    // searchList用のindex
    for (searchIndex in 0 until searchResults.size) {
        val youTubeVideo = YouTubeVideo()

        val searchResult: SearchResult = searchResults[searchIndex]
        val videoResult = videoResults[videoIndex]


        youTubeVideo.title = searchResult.snippet.title
        youTubeVideo.id = searchResult.id.videoId

        if (searchResult.id.videoId.equals(videoResult.id)) {
            //削除されたビデオではないとき
            //videoIndexをインクリメント

            videoIndex++

            //thumbnailDetailsのurlをセット
            youTubeVideo.thumbnailURL = searchResult.snippet.thumbnails.default.url

            //video由来の情報もyouTubeVideoに追加
            if (videoResult.statistics != null) {
                val viewNumber = videoResult.statistics.viewCount
                val viewFormatter = NumberFormat.getIntegerInstance().format(viewNumber) + " views"
                youTubeVideo.viewCount = viewFormatter
            }
            if (videoResult.contentDetails != null) {
                val isoTime = videoResult.contentDetails.duration
                val time = Utils.convertISO8601DurationToNormalTime(isoTime)
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
    val playlistItems: List<YouTubePlaylist> = ArrayList()
    playlistList.key = Config.YOUTUBE_API_KEY
    playlistList.fields = "items(id,snippet/title,snippet/thumbnails/default/url,contentDetails/itemCount,status)"
    playlistList.maxResults = Config.NUMBER_OF_VIDEOS_RETURNED
    playlistList.id = Utils.concatenatePlaylistIDs(searchPlayListResults)
    val playlistListResponse: PlaylistListResponse = playlistList.execute()

    val playlists = playlistListResponse.items

    if (playlists != null) {
        //自分で作った再生リストを返り値用のArrayListに入れる
        putPlaylistsToArrayList(playlistItems, playlists)
    }

    return playlistItems
}

public class VideosLoaderMethods {
    class SearchResultVideo(val resultVideos: List<YouTubeVideo>, val nextPageToken: String?, val keyword: String) {
        //nextPageTokenは最終ページの時はnextPageToken==nullであることに注意
    }

    class SearchResultPlaylist(val resultPlaylists: List<YouTubePlaylist>, val nextPageToken: String?, val keyword: String) {
        //nextPageTokenは最終ページの時はnextPageToken==nullであることに注意
    }
}