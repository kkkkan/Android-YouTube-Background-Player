package com.smedic.tubtub.interfaces;

import com.smedic.tubtub.model.YouTubeVideo;

import java.util.List;

/**
 * Created by smedic on 5.3.17..
 */

public interface OnItemSelected {

    void onPlaylistSelected(List<YouTubeVideo> playlist, int position);
}
