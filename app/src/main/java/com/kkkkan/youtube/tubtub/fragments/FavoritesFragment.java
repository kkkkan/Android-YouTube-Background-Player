/*
 * Copyright (C) 2016 SMedic
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
package com.kkkkan.youtube.tubtub.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kkkkan.youtube.R;
import com.kkkkan.youtube.tubtub.MainActivity;
import com.kkkkan.youtube.tubtub.adapters.VideosAdapter;
import com.kkkkan.youtube.tubtub.database.YouTubeSqlDb;
import com.kkkkan.youtube.tubtub.interfaces.ItemEventsListener;
import com.kkkkan.youtube.tubtub.interfaces.OnFavoritesSelected;
import com.kkkkan.youtube.tubtub.interfaces.OnItemSelected;
import com.kkkkan.youtube.tubtub.model.YouTubePlaylist;
import com.kkkkan.youtube.tubtub.model.YouTubeVideo;
import com.kkkkan.youtube.tubtub.utils.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * Have a Recycleview
 * adapter:VideosAdapter
 * data type: YoutubeVideo
 * <p>
 * Recycleviewを持つ
 * adapter:VideosAdapter
 * dataの型:YoutubeVideo
 * <p>
 * Created by Stevan Medic on 21.3.16..
 */
public class FavoritesFragment extends BaseFragment implements ItemEventsListener<YouTubeVideo> {

    private static final String TAG = "FavoritesFragment";
    private List<YouTubeVideo> favoriteVideos;

    private RecyclerView favoritesListView;
    private VideosAdapter videoListAdapter;
    private OnItemSelected itemSelected;
    private Context context;
    private SwipeRefreshLayout swipeToRefresh;

    public FavoritesFragment() {
        // Required empty public constructor
    }

    public static FavoritesFragment newInstance() {
        Log.d(TAG, "newInstance()");
        return new FavoritesFragment();
    }

    static public void addToFavoritesList(YouTubeVideo video) {
        YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE).create(video);
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "onAttach");
        super.onAttach(context);
        if (context instanceof MainActivity) {
            this.itemSelected = (MainActivity) context;
            this.context = context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        favoriteVideos = new ArrayList<>();
        favoriteVideos.addAll(YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE).readAll());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.d(TAG, "onCreateView");
        View v = inflater.inflate(R.layout.fragment_list, container, false);
        favoritesListView = (RecyclerView) v.findViewById(R.id.fragment_list_items);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        favoritesListView.setLayoutManager(linearLayoutManager);
        //区切り線追加
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(favoritesListView.getContext(),
                linearLayoutManager.getOrientation());
        favoritesListView.addItemDecoration(dividerItemDecoration);

        videoListAdapter = new VideosAdapter(context, favoriteVideos);
        Log.d(TAG, "videoListAdapter is create!");
        videoListAdapter.setOnItemEventsListener(this);
        favoritesListView.setAdapter(videoListAdapter);

          /*swipeで更新*/
        swipeToRefresh = (SwipeRefreshLayout) v.findViewById(R.id.swipe_to_refresh);
        swipeToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(TAG, "onRefresh");
                favoriteVideos.clear();
                favoriteVideos.addAll(YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE).readAll());
                videoListAdapter.notifyDataSetChanged();
                swipeToRefresh.setRefreshing(false);
            }
        });
        return v;
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        favoriteVideos.clear();
        favoriteVideos.addAll(YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE).readAll());
        videoListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach");
        super.onDetach();
        this.itemSelected = null;
        this.context = null;
    }

    public void removeFromFavorites(YouTubeVideo video) {
        YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE).deleteByVideoId(video.getId());
        favoriteVideos.remove(video);
        videoListAdapter.notifyDataSetChanged();

    }


    @Override
    public void onShareClicked(String itemId) {
        share(Config.SHARE_VIDEO_URL + itemId);
    }

    @Override
    public void onFavoriteClicked(YouTubeVideo video, boolean isChecked) {
        if (isChecked) {
            addToFavoritesList(video);
        } else {
            removeFromFavorites(video);
        }
    }

    @Override
    public void onAddClicked(YouTubeVideo video) {
        Fragment fragment = getParentFragment();
        if (fragment instanceof OnFavoritesSelected) {
            ((OnFavoritesSelected) fragment).onAddSelected(video);
        }
    }

    @Override
    public void onItemClick(YouTubeVideo video) {
        itemSelected.onPlaylistSelected(favoriteVideos, favoriteVideos.indexOf(video));
    }

    @Override
    public void onDeleteClicked(YouTubeVideo video) {

    }

    @Override
    public void onDeleteClicked(YouTubePlaylist playlist) {

    }
}
