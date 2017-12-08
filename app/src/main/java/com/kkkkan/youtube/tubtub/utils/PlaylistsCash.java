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
package com.kkkkan.youtube.tubtub.utils;

import com.kkkkan.youtube.tubtub.model.YouTubeVideo;

import java.util.List;

/**
 * Created by admin on 2017/12/07.
 */

public class PlaylistsCash {
    static public PlaylistsCash Instance = new PlaylistsCash();

    private PlaylistsCash() {

    }

    //回転しても検索内容覚えておくためのList
    private List<YouTubeVideo> searchResultsList;

    public void setSearchResultsList(List<YouTubeVideo> searchResultsList) {
        this.searchResultsList = searchResultsList;
    }

    public List<YouTubeVideo> getSearchResultsList() {
        return searchResultsList;
    }

    //NowPlayingListFragment用の、今再生しているplaylistとpositionを入れとくためのList
    private List<YouTubeVideo> nowPlaylist;
    private int currentVideoIndex;

    public void setCurrentVideoIndex(int currentVideoIndex) {
        this.currentVideoIndex = currentVideoIndex;
    }

    public void setNowPlaylist(List<YouTubeVideo> nowPlaylist) {
        this.nowPlaylist = nowPlaylist;
    }

    public int getCurrentVideoIndex() {
        return currentVideoIndex;
    }

    public List<YouTubeVideo> getNowPlaylist() {
        return nowPlaylist;
    }
}
