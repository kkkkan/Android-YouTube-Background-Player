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
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.kkkkan.youtube.R;
import com.kkkkan.youtube.tubtub.MainActivity;
import com.kkkkan.youtube.tubtub.adapters.PlaylistsAdapter;
import com.kkkkan.youtube.tubtub.database.YouTubeSqlDb;
import com.kkkkan.youtube.tubtub.interfaces.ItemEventsListener;
import com.kkkkan.youtube.tubtub.interfaces.OnItemSelected;
import com.kkkkan.youtube.tubtub.model.YouTubePlaylist;
import com.kkkkan.youtube.tubtub.model.YouTubeVideo;
import com.kkkkan.youtube.tubtub.utils.Config;
import com.kkkkan.youtube.tubtub.youtube.YouTubePlaylistVideosLoader;
import com.kkkkan.youtube.tubtub.youtube.YouTubePlaylistsLoader;
import com.kkkkan.youtube.tubtub.youtube.YouTubeSingleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Have a Recycleview
 * adapter:playlistadapter
 * data type:YoutubePlaylis
 * <p>
 * Recycleviewを持つ
 * adapter:playlistadapter
 * dataの型:YoutubePlaylis
 * <p>
 * Class that handles list of the playlists acquired from YouTube
 * Created by smedic on 7.3.16..
 */
public class PlaylistsFragment extends BaseFragment implements
        ItemEventsListener<YouTubePlaylist> {
    private static final String TAG = "PlaylistsFragment";

    private ArrayList<YouTubePlaylist> playlists;
    private RecyclerView playlistsListView;
    private PlaylistsAdapter playlistsAdapter;
    private SwipeRefreshLayout swipeToRefresh;
    private Context context;
    private OnItemSelected itemSelected;
    private Handler mainHandler;

    public PlaylistsAdapter getPlaylistsAdapter() {
        return playlistsAdapter;
    }

    public PlaylistsFragment() {
        // Required empty public constructor
    }

    public static PlaylistsFragment newInstance() {
        return new PlaylistsFragment();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            this.context = context;
            itemSelected = (MainActivity) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playlists = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list, container, false);

        Log.d(TAG, "Playlistfragment-onCreateView");
        /* Setup the ListView */
        playlistsListView = (RecyclerView) v.findViewById(R.id.fragment_list_items);
        playlistsListView.setLayoutManager(new LinearLayoutManager(context));

        swipeToRefresh = (SwipeRefreshLayout) v.findViewById(R.id.swipe_to_refresh);

        playlistsAdapter = new PlaylistsAdapter(context, playlists);
        playlistsAdapter.setOnItemEventsListener(this);
        playlistsListView.setAdapter(playlistsAdapter);
        playlistsAdapter.setOnDetailClickListener((PlaylistsAdapter.OnDetailClickListener) getParentFragment());
        mainHandler = ((MainActivity) getActivity()).mainHandler;


        swipeToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                searchPlaylists();
            }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        playlists.clear();
        playlists.addAll(YouTubeSqlDb.getInstance().playlists().readAll());
        playlistsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.context = null;
        this.itemSelected = null;
    }


    public void searchPlaylists() {
        getLoaderManager().restartLoader(Config.YouTubePlaylistLoaderId, null, new LoaderManager.LoaderCallbacks<List<YouTubePlaylist>>() {
            @Override
            public Loader<List<YouTubePlaylist>> onCreateLoader(final int id, final Bundle args) {
                Log.d(TAG, "onCreateLoader");
                return new YouTubePlaylistsLoader(context);
            }


            @Override
            public void onLoadFinished(Loader<List<YouTubePlaylist>> loader, List<YouTubePlaylist> data) {
                Log.d(TAG, "onLoadFinished");
                if (data == null) {
                    swipeToRefresh.setRefreshing(false);
                    return;
                }
                YouTubeSqlDb.getInstance().playlists().deleteAll();
                for (YouTubePlaylist playlist : data) {
                    YouTubeSqlDb.getInstance().playlists().create(playlist);
                }

                Log.d(TAG, "onLoadFinished: data size: " + data.size());

                playlists.clear();
                playlists.addAll(data);
                playlistsAdapter.notifyDataSetChanged();
                swipeToRefresh.setRefreshing(false);

                for (YouTubePlaylist playlist : playlists) {
                    Log.d(TAG, "onLoadFinished: >>> " + playlist.getTitle());
                }

            }

            @Override
            public void onLoaderReset(Loader<List<YouTubePlaylist>> loader) {
                playlists.clear();
                playlists.addAll(Collections.<YouTubePlaylist>emptyList());
                playlistsAdapter.notifyDataSetChanged();
                swipeToRefresh.setRefreshing(false);
            }
        }).forceLoad();
    }

    private void acquirePlaylistVideos(final String playlistId) {
        Log.d(TAG, "acquirePlaylistVideos");
        getLoaderManager().restartLoader(Config.YouTubePlaylistDetailLoaderId, null, new LoaderManager.LoaderCallbacks<List<YouTubeVideo>>() {
            //このフラグがないとこのfragmentに戻ったときなぜか onLoadFinishedが呼ばれてしまうことがある
            boolean loaderRunning = false;

            @Override
            public Loader<List<YouTubeVideo>> onCreateLoader(final int id, final Bundle args) {
                Log.d(TAG, "PlaylistsFragment.acquirePlaylistVideos.onCreateLoader-id:" + playlistId);
                loaderRunning = true;
                return new YouTubePlaylistVideosLoader(context, playlistId);
            }

            @Override
            public void onLoadFinished(Loader<List<YouTubeVideo>> loader, List<YouTubeVideo> data) {
                Log.d(TAG, "PlaylistsFragment.acquirePlaylistVideos.onLoadFinished");
                if (!loaderRunning) {
                    return;
                }
                loaderRunning = false;
                if (data == null || data.isEmpty()) {
                    return;
                }
                itemSelected.onPlaylistSelected(playlistsListView, data, 0);
            }

            @Override
            public void onLoaderReset(Loader<List<YouTubeVideo>> loader) {
                Log.d(TAG, "PlaylistsFragment.acquirePlaylistVideos.onLoaderReset");
                playlists.clear();
                playlists.addAll(Collections.<YouTubePlaylist>emptyList());
            }
        }).forceLoad();
    }


    @Override
    public void onShareClicked(String itemId) {
        share(Config.SHARE_PLAYLIST_URL + itemId);
    }

    @Override
    public void onFavoriteClicked(YouTubeVideo video, boolean isChecked) {
        //do nothing
    }

    @Override
    public void onAddClicked(YouTubeVideo video) {
        //何もしない
    }

    @Override
    public void onItemClick(YouTubePlaylist youTubePlaylist) {
        //results are in onVideosReceived callback method
        Log.d(TAG, "onItemClick");
        String id = youTubePlaylist.getId();
        Log.d(TAG, "PlaylistsFragment-onItemClicked-id:" + id);
        acquirePlaylistVideos(youTubePlaylist.getId());
    }

    @Override
    public void onDeleteClicked(YouTubeVideo video) {
        //do nothing
        //何もしない
    }

    public void onDeleteClicked(final YouTubePlaylist playlist) {
        //Give a confirmation dialog of deletion.
        //削除の確認のダイアログを出す。
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle("削除").setMessage("プレイリスト " + playlist.getTitle() + "(" + playlist.getNumberOfVideos() + ")\nを削除しますか？")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    YouTubeSingleton.getYouTubeWithCredentials().playlists().delete(playlist.getId()).execute();
                                } catch (Exception e) {
                                    Log.d(TAG, "PlaylistDetailFragment-onAddClicked-delete-error-:" + e.getMessage());
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getContext(), "削除に失敗しました。", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                    return;
                                }
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getContext(), "削除に成功しました。", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }).start();
                    }
                }).show();

    }

}