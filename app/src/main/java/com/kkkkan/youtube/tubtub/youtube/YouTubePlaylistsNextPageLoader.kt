package com.kkkkan.youtube.tubtub.youtube


import android.content.Context
import android.support.v4.content.AsyncTaskLoader
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.SearchListResponse
import com.google.api.services.youtube.model.SearchResult
import com.kkkkan.youtube.tubtub.model.YouTubePlaylist
import com.kkkkan.youtube.tubtub.utils.Config
import com.kkkkan.youtube.tubtub.youtube.YouTubeSingleton.getYouTube
import java.io.IOException

/**
 * 検索結果のYoutube PlaylistのnextPageTokenを渡すとそのpageとさらにnextPageTokenをとってきてくれるAsyncLoader
 */
class YouTubePlaylistsNextPageLoader(context: Context, private val nextPageToken: String, private val keyword: String) : AsyncTaskLoader<VideosLoaderMethods.SearchResultPlaylist>(context) {
    private val youtube: YouTube = getYouTube()
    override fun loadInBackground(): VideosLoaderMethods.SearchResultPlaylist {
        var playlistItem: List<YouTubePlaylist> = ArrayList<YouTubePlaylist>()
        var playlistNextPageToken: String? = null
        try {
            var searchPlayList: YouTube.Search.List = youtube.search().list("id,snippet")
            var playlistList: YouTube.Playlists.List = youtube.playlists().list("id,snippet,contentDetails,status")

            //わたってきたTokenのページをとってくる
            var searchPlaylistResponse: SearchListResponse = searchPlaylist(searchPlayList)
            //取ってきたページ情報のうちplaylist部分のみを取得
            var searchPlaylistResults: List<SearchResult> = searchPlaylistResponse.items
            //それらのplaylistについてさらに細かい情報を取得
            playlistItem = getPlaylistList(playlistList, searchPlaylistResults)
            //次のページへのTokenを取得
            playlistNextPageToken = searchPlaylistResponse.nextPageToken
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return VideosLoaderMethods.SearchResultPlaylist(playlistItem, playlistNextPageToken, keyword)
    }


    /**
     * わたってきたTokenのページをとってくる
     */
    private fun searchPlaylist(searchList: YouTube.Search.List): SearchListResponse {
        searchList.key = Config.YOUTUBE_API_KEY
        searchList.type = "playlist"
        searchList.maxResults = Config.NUMBER_OF_VIDEOS_RETURNED
        searchList.fields = "nextPageToken,items(id/playlistId,snippet/title,snippet/thumbnails/default/url,id/kind)"
        searchList.q = keyword
        searchList.pageToken = nextPageToken
        var searchListResponse = searchList.execute()
        return searchListResponse
    }
}