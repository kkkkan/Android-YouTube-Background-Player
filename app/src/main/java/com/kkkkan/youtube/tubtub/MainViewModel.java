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

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by kkkkan on 2017/12/26.
 */

public class MainViewModel extends ViewModel {
    final private String TAG = "MainViewModel";
    private final MutableLiveData<String> videoTitle = new MutableLiveData<>();
    private MediaPlayerService service;
    private Observer<String> observer = new Observer<String>() {
        @Override
        public void onChanged(@Nullable String s) {
            Log.d(TAG, "onChanged");
            videoTitle.setValue(s);
        }
    };

    /**
     * Serviceとバインドしたときに呼ぶことを想定したメゾッド。
     * ServiceのもつLiveDataのobserveを始める。
     *
     * @param service
     */
    public void startObserve(MediaPlayerService service) {
        this.service = service;
        //ビデオタイトルをobserve
        service.getVideoTitle().observeForever(observer);
    }

    /**
     * ライフサイクルを合わせているactivityやfragmentのインスタンスが
     * 破棄されたときに呼ばれるメゾッド
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "onCleared");
        //observeやめる
        service.getVideoTitle().removeObserver(observer);
    }

    public MutableLiveData<String> getVideoTitle() {
        return videoTitle;
    }
}
