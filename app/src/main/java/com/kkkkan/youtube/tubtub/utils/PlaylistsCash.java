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

import android.arch.lifecycle.MutableLiveData;
import android.util.Log;

import com.kkkkan.youtube.tubtub.model.YouTubeVideo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by admin on 2017/12/07.
 */

public class PlaylistsCash {
    private final String TAG = "PlaylistsCash";
    public static int tag = 0;
    static public PlaylistsCash Instance = new PlaylistsCash();

    private PlaylistsCash() {

    }

    //回転しても検索内容覚えておくためのList
    private List<YouTubeVideo> searchResultsList;

    public void setSearchResultsList(List<YouTubeVideo> searchResultsList) {
        this.searchResultsList = new ArrayList<>(searchResultsList);
    }

    public List<YouTubeVideo> getSearchResultsList() {
        return searchResultsList != null ? new ArrayList<>(searchResultsList) : null;
    }

    //NowPlayingListFragment用の、今再生しているplaylistとpositionを入れとくためのList
    //private List<YouTubeVideo> nowPlaylist;

    //通常の並びに並んでいるリスト
    private List<YouTubeVideo> normalList = new ArrayList<>();
    //shuffle再生用のランダムに並んでいるリスト
    private List<YouTubeVideo> shuffleList = new ArrayList<>();
    //NowPlayingListFragmentで今再生中のビデオのみ色を変えるためのMutableLiveData
    private MutableLiveData<Integer> mutableCurrentVideoIndex = new MutableLiveData<>();


    public void setCurrentVideoIndex(int currentVideoIndex) {
        mutableCurrentVideoIndex.setValue(currentVideoIndex);
    }

    public void setNewList(List<YouTubeVideo> newNormalList) {
        normalList = new ArrayList<>(newNormalList);
        shuffleList = new ArrayList<>(newNormalList);
        Collections.shuffle(shuffleList);
    }

    public void deleteVideoInList(YouTubeVideo video, Settings.Shuffle shuffle) {
        int deleteVideoIndex = 0;
        switch (shuffle) {
            case ON:
                deleteVideoIndex = shuffleList.indexOf(video);
                break;
            case OFF:
                deleteVideoIndex = normalList.indexOf(video);
                break;
        }
        if (getCurrentVideoIndex() > deleteVideoIndex) {
            setCurrentVideoIndex(getCurrentVideoIndex() - 1);
        }
        shuffleList.remove(video);
        normalList.remove(video);
    }

    public List<YouTubeVideo> getNormalList() {
        return normalList != null ? new ArrayList<>(normalList) : null;
    }

    public List<YouTubeVideo> getShuffleList() {
        return shuffleList != null ? new ArrayList<>(shuffleList) : null;
    }

    public int getCurrentVideoIndex() {
        return mutableCurrentVideoIndex.getValue() != null ? mutableCurrentVideoIndex.getValue() : 0;
    }


    public MutableLiveData<Integer> getMutableCurrentVideoIndex() {
        return mutableCurrentVideoIndex;
    }

    public int getPlayingListSize() {
        //normalListとshffleListのサイズは常に同じはず
        return normalList.size();
    }

    /*public boolean isPlayingListNull() {
        return normalList == null || shuffleList == null;
    }
*/

    /**
     * shuffleし直す
     */
    public void reShuffle() {
        Log.d(TAG, "reShuffle()");
        shuffleList = new ArrayList<>(normalList);
        Collections.shuffle(shuffleList);
    }
}
