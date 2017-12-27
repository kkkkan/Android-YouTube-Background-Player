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
