package com.kkkkan.youtube.tubtub;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;


/**
 * Created by ka1n9 on 2017/11/24.
 */

public class MainActivityViewModel extends ViewModel {
    final private static String TAG = "MainActivityViewModel";

    public enum LoadingDialogState {
        Hide,
        Show
    }

    private LiveData<LoadingDialogState>
}
