package com.kkkkan.youtube.tubtub;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;


/**
 * Created by ka1n9 on 2017/11/24.
 */

public class MainActivityViewModel extends ViewModel {
    final private static String TAG = "MainActivityViewModel";

    public enum LoadingState {
        StartLoading,
        StopLoading,
        Error
    }

    private final MutableLiveData<LoadingState> loadingState = new MutableLiveData<>();
    private final MutableLiveData<String> videoTitle = new MutableLiveData<>();

    public MutableLiveData<LoadingState> getLoadingState() {
        return loadingState;
    }

    public MutableLiveData<String> getVideoTitle() {
        return videoTitle;
    }

    public void setStateStartLoading() {
        loadingState.setValue(LoadingState.StartLoading);
    }

    public void setStateStopLoading() {
        loadingState.setValue(LoadingState.StopLoading);
    }

    public void setStateError() {
        loadingState.setValue(LoadingState.Error);
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle.setValue(videoTitle);
    }
}
