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

import com.kkkkan.youtube.tubtub.model.YouTubeVideo;

import java.util.List;

/**
 * Horizontal screen lock · One song repeat · Playlist repeat setting is put in singleton class
 * <p>
 * 横画面ロック・一曲リピート・プレイリストリピートの設定を
 * 入れておくsingletonクラス
 * Created by admin on 2017/11/07.
 */

public class Settings {
    final private static String TAG = "Settings";
    static private Settings settings = new Settings();
    private ScreenLock screenLock = ScreenLock.OFF;
    private RepeatOne repeatOne = RepeatOne.OFF;
    private RepeatPlaylist repeatPlaylist = RepeatPlaylist.OFF;

    private MutableLiveData<Shuffle> shuffleMutableLiveData = new MutableLiveData<>();

    private Settings() {
        shuffleMutableLiveData.setValue(Shuffle.OFF);
    }

    static public Settings getInstance() {
        return settings;
    }

    /**
     * 設定しているshuffleの値だけ取り出す
     *
     * @return
     */
    public Shuffle getShuffle() {
        return shuffleMutableLiveData.getValue();
    }

    public MutableLiveData<Shuffle> getShuffleMutableLiveData() {
        return shuffleMutableLiveData;
    }

    public ScreenLock getScreenLock() {
        return screenLock;
    }

    public RepeatOne getRepeatOne() {
        return repeatOne;
    }

    public RepeatPlaylist getRepeatPlaylist() {
        return repeatPlaylist;
    }

    public void setScreenLock(ScreenLock screenLock) {
        this.screenLock = screenLock;
    }

    public void setShuffle(Shuffle shuffle) {
        //どこから呼ばれてもいいようにpostにする
        //->postにしたら遅すぎてUI表示との齟齬が生まれてしまったのでsetに変更
        shuffleMutableLiveData.setValue(shuffle);

        // XXX settingsモジュールがcacheを直接触るのは変な気がする。以下のいずれかにすると良いのかも
        // - cache側が設定をobserveする
        // - 別の第三者がcacheの設定をする
        // - 設定を変えた時に直ぐにcacheを触らなくても良いような構成にする
        if (PlaylistsCash.Instance.getPlayingListSize() == 0) {
            //何もビデオセットする前からシャッフルモードのON/OFFはいじれるのでその時に落ちないよう対策
            return;
        }

        //以下、PlaylistCashのcurrentIndexを今再生中のビデオの変更先モードでのリストでのindexに変更する
        PlaylistsCash playlistsCash = PlaylistsCash.Instance;
        YouTubeVideo nowPlayingVideo;
        int currentIndex = playlistsCash.getCurrentVideoIndex();
        List<YouTubeVideo> normalList = playlistsCash.getNormalList();
        List<YouTubeVideo> shuffleList = playlistsCash.getShuffleList();

        switch (shuffle) {
            case ON:
                nowPlayingVideo = normalList.get(currentIndex);
                int nowPlayingVideoInShuffleListIndex = shuffleList.indexOf(nowPlayingVideo);
                playlistsCash.setCurrentVideoIndex(nowPlayingVideoInShuffleListIndex);
                break;
            case OFF:
                nowPlayingVideo = shuffleList.get(currentIndex);
                int nowPlayingVideoindexInNormalListIndex = normalList.indexOf(nowPlayingVideo);
                playlistsCash.setCurrentVideoIndex(nowPlayingVideoindexInNormalListIndex);
                //shuffleをOFFにするたびに次シャッフルモードになったときのためにリストをシャッフルし直す
                PlaylistsCash.Instance.reShuffle();
                break;
        }
    }

    public void setRepeatOne(RepeatOne repeatOne) {
        this.repeatOne = repeatOne;
    }

    public void setRepeatPlaylist(RepeatPlaylist repeatPlaylist) {
        this.repeatPlaylist = repeatPlaylist;
    }


    /**
     * Enum for whether horizontal screen lock or not
     * <p>
     * 横画面ロックか否かのための
     * enum
     */
    public enum ScreenLock {
        ON,
        OFF
    }

    /**
     * Enum for whether shuffle or not
     * shuffleか否かのためのenum
     */
    public enum Shuffle {
        ON,
        OFF
    }

    /**
     * Enum for whether to repeat a song or not
     * <p>
     * 一曲リピートか否かのための
     * enum
     */
    public enum RepeatOne {
        ON,
        OFF
    }

    /**
     * Enum for whether playlist repeat or not
     * <p>
     * プレイリストリピートか否かのための
     * enum
     */
    public enum RepeatPlaylist {
        ON,
        OFF
    }


}
