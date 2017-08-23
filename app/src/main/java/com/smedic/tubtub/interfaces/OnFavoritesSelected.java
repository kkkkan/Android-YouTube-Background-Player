package com.smedic.tubtub.interfaces;

import com.smedic.tubtub.model.YouTubeVideo;

/**
 * Created by smedic on 5.3.17..
 */

public interface OnFavoritesSelected {
    void onFavoritesSelected(YouTubeVideo video, boolean isChecked);

    void onAddSelected(YouTubeVideo video);
}
