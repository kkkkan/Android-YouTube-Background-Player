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

    private List<YouTubeVideo> searchResultsList;

    public void setSearchResultsList(List<YouTubeVideo> searchResultsList) {
        this.searchResultsList = searchResultsList;
    }

    public List<YouTubeVideo> getSearchResultsList() {
        return searchResultsList;
    }

}
