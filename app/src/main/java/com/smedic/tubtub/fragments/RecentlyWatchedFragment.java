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
package com.smedic.tubtub.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


import com.smedic.tubtub.MainActivity;
import com.smedic.tubtub.R;
import com.smedic.tubtub.adapters.RecentlyVideosAdapter;
import com.smedic.tubtub.database.YouTubeSqlDb;
import com.smedic.tubtub.interfaces.ItemEventsListener;
import com.smedic.tubtub.interfaces.OnFavoritesSelected;
import com.smedic.tubtub.interfaces.OnItemSelected;
import com.smedic.tubtub.model.YouTubePlaylist;
import com.smedic.tubtub.model.YouTubeVideo;
import com.smedic.tubtub.utils.Config;

import java.util.ArrayList;


/**
 * Class that handles list of the recently watched YouTube
 * Created by smedic on 7.3.16..
 */

/*Recycleviewを持つ
* adapter:RecentlyVideosAdapter
* dataの型:YoutubeVideot*/
public class RecentlyWatchedFragment extends BaseFragment implements
        ItemEventsListener<YouTubeVideo> {

    private static final String TAG = "SMEDIC RecentlyWatched";

    private ArrayList<YouTubeVideo> recentlyPlayedVideos;
    private RecyclerView recentlyPlayedListView;
    private RecentlyVideosAdapter videoListAdapter;
    private OnItemSelected itemSelected;
    private OnFavoritesSelected onFavoritesSelected;
    private Context context;
    private Handler mainHandler;
    private SwipeRefreshLayout swipeToRefresh;

    public RecentlyWatchedFragment() {
        // Required empty public constructor
    }

    public static RecentlyWatchedFragment newInstance() {
        return new RecentlyWatchedFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recentlyPlayedVideos = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_list, container, false);
        recentlyPlayedListView = (RecyclerView) v.findViewById(R.id.fragment_list_items);
        recentlyPlayedListView.setLayoutManager(new LinearLayoutManager(context));
        videoListAdapter = new RecentlyVideosAdapter(context, recentlyPlayedVideos);
        videoListAdapter.setOnItemEventsListener(this);
        recentlyPlayedListView.setAdapter(videoListAdapter);
        mainHandler=((MainActivity)getActivity()).mainHandler;

         /*swipeで更新*/
        swipeToRefresh = (SwipeRefreshLayout) v.findViewById(R.id.swipe_to_refresh);
        swipeToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(TAG,"onRefresh");
                recentlyPlayedVideos.clear();
                recentlyPlayedVideos.addAll(YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).readAll());
                videoListAdapter.notifyDataSetChanged();
                swipeToRefresh.setRefreshing(false);
            }
        });


        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        recentlyPlayedVideos.clear();
        recentlyPlayedVideos.addAll(YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).readAll());
        videoListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            this.context = context;
            itemSelected = (MainActivity) context;
            onFavoritesSelected = (MainActivity) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.context = null;
        itemSelected = null;
        onFavoritesSelected = null;
    }

    /**
     * Clears recently played list items
     */
    public void clearRecentlyPlayedList() {
        recentlyPlayedVideos.clear();
        videoListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onShareClicked(String itemId) {
        share(Config.SHARE_VIDEO_URL + itemId);
    }

    @Override
    public void onFavoriteClicked(YouTubeVideo video, boolean isChecked) {
        onFavoritesSelected.onFavoritesSelected(video, isChecked); // pass event to MainActivity
    }

    @Override
    public void onAddClicked(YouTubeVideo video){
        onFavoritesSelected.onAddSelected(video); // pass event to MainActivity
    }
    @Override
    public void onItemClick(YouTubeVideo video) {
        itemSelected.onPlaylistSelected(recentlyPlayedVideos, recentlyPlayedVideos.indexOf(video));
    }

    public void onDeleteClicked(final YouTubeVideo video){
        /*削除の確認のダイアログを出す。*/
        AlertDialog.Builder dialog=new AlertDialog.Builder(getContext());
        dialog.setTitle("削除").setMessage(video.getTitle()+"\nを履歴から削除しますか？")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                if (YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).delete(video.getId())) {
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getContext(), "削除に成功しました。", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                } else {
                                    Log.d(TAG, "RecentlyWztchedFragment-delete-error-" + video.getTitle());
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getContext(), "削除に失敗しました。", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }

                            }

                        }).start();
                    }
                }).show();
    }

    @Override
    public void onDeleteClicked(YouTubePlaylist playlist) {

    }
}

