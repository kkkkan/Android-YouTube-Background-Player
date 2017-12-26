package com.kkkkan.youtube.tubtub;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.Nullable;

/**
 * Created by kkkkan on 2017/12/26.
 */

public class MainViewModel extends ViewModel {
    private final MutableLiveData<String> videoTitle = new MutableLiveData<>();

    /**
     * Serviceとバインドしたときに呼ぶことを想定したメゾッド。
     * ServiceのもつLiveDataのobserveを始める。
     *
     * @param service
     */
    public void startObserve(MediaPlayerService service) {
        //ビデオタイトルをobserve
        service.getVideoTitle().observeForever(new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                videoTitle.setValue(s);
            }
        });
    }

    public MutableLiveData<String> getVideoTitle() {
        return videoTitle;
    }
}
