package com.kkkkan.youtube.tubtub.youtube

import android.content.Context
import android.support.v4.content.AsyncTaskLoader
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.SearchListResponse
import com.kkkkan.youtube.tubtub.model.YouTubeVideo
import com.kkkkan.youtube.tubtub.utils.Config
import com.kkkkan.youtube.tubtub.youtube.YouTubeSingleton.getYouTube
import java.io.IOException

/**
 * 検索結果のYoutube VideoのnextPageTokenを渡すとそのpageとさらにnextPageTokenをとってきてくれるAsyncLoader
 */
class YouTubeVideosNextPageLoader(context: Context, private val nextPageToken: String, val keyword: String) : AsyncTaskLoader<VideosLoaderMethods.SearchResultVideo>(context) {
    private val youtube: YouTube = getYouTube()
    override fun loadInBackground(): VideosLoaderMethods.SearchResultVideo {
        var videoItem: List<YouTubeVideo> = ArrayList<YouTubeVideo>()
        var videosNextToken: String? = null

        try {
            var searchList: YouTube.Search.List = youtube.search().list("id,snippet")
            var videoList: YouTube.Videos.List = youtube.videos().list("id,contentDetails,statistics")

            //わたってきたTokenのページをとってくる
            var searchVideoResponse: SearchListResponse = searchVideo(searchList)
            //取ってきたページ情報のうちvideo部分のみを取得
            var searchVideoResults = searchVideoResponse.items
            //それらのvideoについてさらに細かい情報を取得
            videoItem = getVideoList(videoList, searchVideoResults)
            //次のページへのTokenを取得
            videosNextToken = searchVideoResponse.nextPageToken
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return VideosLoaderMethods.SearchResultVideo(videoItem, videosNextToken, keyword)
    }

    /**
     * わたってきたTokenのページをとってくる
     */
    private fun searchVideo(searchList: YouTube.Search.List): SearchListResponse {
        searchList.key = Config.YOUTUBE_API_KEY
        searchList.type = "video"
        searchList.maxResults = Config.NUMBER_OF_VIDEOS_RETURNED
        searchList.fields = "nextPageToken,items(id/videoId,snippet/title,snippet/thumbnails/default/url)"
        searchList.q = keyword
        searchList.pageToken = nextPageToken
        var searchListResponse = searchList.execute()
        return searchListResponse
    }
}