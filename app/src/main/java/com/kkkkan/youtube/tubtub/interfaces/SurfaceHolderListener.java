package com.kkkkan.youtube.tubtub.interfaces;

import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by ka1n9 on 2017/11/29.
 */

public interface SurfaceHolderListener {
    void changeSurfaceHolder(SurfaceHolder holder, SurfaceView surfaceView);

    void releaseSurfaceHolder(SurfaceHolder holder);
}
