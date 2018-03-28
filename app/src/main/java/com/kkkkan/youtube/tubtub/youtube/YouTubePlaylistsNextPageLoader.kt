package com.kkkkan.youtube.tubtub.youtube

import android.content.AsyncTaskLoader
import android.content.Context
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.SearchListResponse
import com.google.api.services.youtube.model.SearchResult
import com.kkkkan.youtube.tubtub.model.YouTubePlaylist
import com.kkkkan.youtube.tubtub.utils.Config
import com.kkkkan.youtube.tubtub.youtube.YouTubeSingleton.getYouTube
import java.io.IOException

class YouTubePlaylistsNextPageLoader(context: Context, nextPageToken: String) : AsyncTaskLoader<VideosLoaderMethods.SearchResultPlaylist>(context) {
    private val youtube: YouTube = getYouTube()
    private val nextPageToken = nextPageToken
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

        return VideosLoaderMethods.SearchResultPlaylist(playlistItem, playlistNextPageToken)
    }


    /**
     * わたってきたTokenのページをとってくる
     */
    private fun searchPlaylist(searchList: YouTube.Search.List): SearchListResponse {
        searchList.key = Config.YOUTUBE_API_KEY
        searchList.type = "playlist"
        searchList.maxResults = Config.NUMBER_OF_VIDEOS_RETURNED
        searchList.fields = "items(id/playlistId,snippet/title,snippet/thumbnails/default/url,id/kind)"
        searchList.pageToken = nextPageToken
        var searchListResponse = searchList.execute()
        return searchListResponse
    }
}