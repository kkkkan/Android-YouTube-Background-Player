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
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import com.kkkkan.youtube.tubtub.model.YouTubePlaylist;
import com.kkkkan.youtube.tubtub.model.YouTubeVideo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by kkkkan on 2017/12/07.
 */

public class PlaylistsCache {
    private final String TAG = "PlaylistsCash";
    public static int tag = 0;
    static public PlaylistsCache Instance = new PlaylistsCache();

    private PlaylistsCache() {
        //PlaylistCasheクラスで、設定の再生モードがシャッフルか否か監視し、シャッフルのON/OFFが切り替わるたびに
        //currentIndexを変更先モードでのリストでの今再生中のビデオのindexに変える
        Settings.getInstance().getShuffleMutableLiveData().observeForever(new Observer<Settings.Shuffle>() {
            @Override
            public void onChanged(@Nullable Settings.Shuffle shuffle) {

                if (shuffle == null || getPlayingListSize() == 0) {
                    //何もビデオセットする前からシャッフルモードのON/OFFはいじれるのでその時に落ちないよう対策
                    return;
                }

                //以下、PlaylistCashのcurrentIndexを今再生中のビデオの変更先モードでのリストでのindexに変更する
                YouTubeVideo nowPlayingVideo;
                int currentIndex = getCurrentVideoIndex();
                List<YouTubeVideo> normalList = getNormalList();
                List<YouTubeVideo> shuffleList = getShuffleList();

                switch (shuffle) {
                    case ON:
                        nowPlayingVideo = normalList.get(currentIndex);
                        int nowPlayingVideoInShuffleListIndex = shuffleList.indexOf(nowPlayingVideo);
                        setCurrentVideoIndex(nowPlayingVideoInShuffleListIndex);
                        break;
                    case OFF:
                        nowPlayingVideo = shuffleList.get(currentIndex);
                        int nowPlayingVideoindexInNormalListIndex = normalList.indexOf(nowPlayingVideo);
                        setCurrentVideoIndex(nowPlayingVideoindexInNormalListIndex);
                        //shuffleをOFFにするたびに次シャッフルモードになったときのためにリストをシャッフルし直す
                        reShuffle();
                        break;
                }
            }
        });
    }

    //回転後、Activityのfinish後も検索内容覚えておくためのList
    //内部的にはPairでキャッシュを取っておくことはしない
    private List<YouTubeVideo> searchResultsVideoList = new ArrayList<>();
    private List<YouTubePlaylist> searchResultsPlaylistList = new ArrayList<>();

    /**
     * 検索結果はvideoとplaylist一方だけが変わることは無いのでsetterはPairの形のもののみ
     *
     * @param searchResultsList
     */
    public void setSearchResultsList(Pair<List<YouTubeVideo>, List<YouTubePlaylist>> searchResultsList) {
        //渡した先でリストの中身をいじってもキャッシュに影響でないように必ず新しいListのインスタンスを作って保持
        searchResultsVideoList = new ArrayList<>(searchResultsList.first);
        searchResultsPlaylistList = new ArrayList<>(searchResultsList.second);
    }

    /**
     * Pairの形で返すgetter
     *
     * @return
     */
    @NonNull
    public Pair<List<YouTubeVideo>, List<YouTubePlaylist>> getSearchResultsList() {
        //渡した先でリストの中身をいじってもキャッシュに影響でないように必ず新しいListのインスタンスでPairを作る
        List<YouTubeVideo> videoList = new ArrayList<>(searchResultsVideoList);
        List<YouTubePlaylist> playlistList = new ArrayList<>(searchResultsPlaylistList);
        return new Pair<>(videoList, playlistList);
    }

    public List<YouTubeVideo> getSearchResultsVideoList() {
        return new ArrayList<>(searchResultsVideoList);
    }

    public List<YouTubePlaylist> getSearchResultsPlaylistList() {
        return new ArrayList<>(searchResultsPlaylistList);
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

    /**
     * shuffleし直す
     */
    public void reShuffle() {
        Log.d(TAG, "reShuffle()");
        shuffleList = new ArrayList<>(normalList);
        Collections.shuffle(shuffleList);
    }


}
