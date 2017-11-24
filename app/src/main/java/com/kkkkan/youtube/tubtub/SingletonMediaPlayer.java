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
package com.kkkkan.youtube.tubtub;

import android.media.MediaPlayer;

import com.kkkkan.youtube.tubtub.model.YouTubeVideo;

import java.util.List;

/**
 * Created by admin on 2017/11/24.
 */

public class SingletonMediaPlayer {
    static private final String TAG = "SingletonMediaPlayer";
    static public final SingletonMediaPlayer instance = new SingletonMediaPlayer();
    static private MediaPlayer mediaPlayer;

    //Insert the playlist that is currently playing and the number of the video in it
    //今再生中のプレイリストとその中の何番目のビデオかを入れておく
    public List<YouTubeVideo> playlist;
    public int currentVideoIndex;
    //For movie title
    //動画タイトル用
    public String VideoTitle;

    public String videoUrl;


    private SingletonMediaPlayer() {
        mediaPlayer = new MediaPlayer();
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

}
