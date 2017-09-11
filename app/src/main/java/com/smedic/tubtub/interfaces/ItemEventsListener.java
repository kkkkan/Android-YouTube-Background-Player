package com.smedic.tubtub.interfaces;

import com.smedic.tubtub.model.YouTubePlaylist;
import com.smedic.tubtub.model.YouTubeVideo;

/**
 * Created by smedic on 9.2.17..
 */

public interface ItemEventsListener<Model> {
    void onShareClicked(String itemId);

    void onFavoriteClicked(YouTubeVideo video, boolean isChecked);

    void onAddClicked(YouTubeVideo video);

    void onItemClick(Model model); //handle click on a row (video or playlist)

    void onDeleteClicked(YouTubeVideo video);

    void onDeleteClicked(YouTubePlaylist playlist);

}
