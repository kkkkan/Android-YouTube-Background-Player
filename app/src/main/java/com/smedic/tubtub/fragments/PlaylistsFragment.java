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
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.smedic.tubtub.MainActivity;
import com.smedic.tubtub.R;
import com.smedic.tubtub.adapters.PlaylistsAdapter;
import com.smedic.tubtub.database.YouTubeSqlDb;
import com.smedic.tubtub.interfaces.ItemEventsListener;
import com.smedic.tubtub.interfaces.OnItemSelected;
import com.smedic.tubtub.model.YouTubePlaylist;
import com.smedic.tubtub.model.YouTubeVideo;
import com.smedic.tubtub.utils.Config;
import com.smedic.tubtub.youtube.YouTubePlaylistVideosLoader;
import com.smedic.tubtub.youtube.YouTubePlaylistsLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.smedic.tubtub.youtube.YouTubeSingleton.getCredential;

/**
 * Class that handles list of the playlists acquired from YouTube
 * Created by smedic on 7.3.16..
 */
public class PlaylistsFragment extends BaseFragment implements
        ItemEventsListener<YouTubePlaylist> {

    private static final String TAG = "SMEDIC PlaylistsFrag";

    private ArrayList<YouTubePlaylist> playlists;
    private RecyclerView playlistsListView;
    private PlaylistsAdapter playlistsAdapter;
    private SwipeRefreshLayout swipeToRefresh;
    private Context context;
    private OnItemSelected itemSelected;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playlists = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list, container, false);

        Log.d("kandabshi","Playlistfragment-onCreateView");
        /* Setup the ListView */
        playlistsListView = (RecyclerView) v.findViewById(R.id.fragment_list_items);
        playlistsListView.setLayoutManager(new LinearLayoutManager(context));

        swipeToRefresh = (SwipeRefreshLayout) v.findViewById(R.id.swipe_to_refresh);

        playlistsAdapter = new PlaylistsAdapter(context, playlists);
        playlistsAdapter.setOnItemEventsListener(this);
        playlistsListView.setAdapter(playlistsAdapter);
        playlistsAdapter.setOnDetailClickListener((PlaylistsAdapter.OnDetailClickListener) getActivity());
        playlistsAdapter.setmTextView(((MainActivity)getActivity()).getmTextView());



        swipeToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                searchPlaylists();
            }
        });
        return v;
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
    public void onDetach() {
        super.onDetach();
        this.context = null;
        this.itemSelected = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        playlists.clear();
        playlists.addAll(YouTubeSqlDb.getInstance().playlists().readAll());
        playlistsAdapter.notifyDataSetChanged();
    }

    public void searchPlaylists() {
        getLoaderManager().restartLoader(2, null, new LoaderManager.LoaderCallbacks<List<YouTubePlaylist>>() {
            @Override
            public Loader<List<YouTubePlaylist>> onCreateLoader(final int id, final Bundle args) {
                Log.d("kandabashi", "onCreateLoader");
                return new YouTubePlaylistsLoader(context);
            }


            @Override
            public void onLoadFinished(Loader<List<YouTubePlaylist>> loader, List<YouTubePlaylist> data) {
                Log.d("kandabashi","onLoadFinished");
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
            }
        }).forceLoad();
    }

    private void acquirePlaylistVideos(final String playlistId) {
        Log.d("kandabashi","acquirePlaylistVideos");
        getLoaderManager().restartLoader(3, null, new LoaderManager.LoaderCallbacks<List<YouTubeVideo>>() {
            @Override
            public Loader<List<YouTubeVideo>> onCreateLoader(final int id, final Bundle args) {
                Log.d("kandabashi","PlaylistsFragment.acquirePlaylistVideos.onCreateLoader-id:"+playlistId);
                return new YouTubePlaylistVideosLoader(context, playlistId);
            }

            @Override
            public void onLoadFinished(Loader<List<YouTubeVideo>> loader, List<YouTubeVideo> data) {
                Log.d("kandabashi","PlaylistsFragment.acquirePlaylistVideos.onLoadFinished");
                if (data == null || data.isEmpty()) {
                    return;
                }
                itemSelected.onPlaylistSelected(data, 0);
            }

            @Override
            public void onLoaderReset(Loader<List<YouTubeVideo>> loader) {
                Log.d("kandabashi","PlaylistsFragment.acquirePlaylistVideos.onLoaderReset");
                playlists.clear();
                playlists.addAll(Collections.<YouTubePlaylist>emptyList());
            }
        }).forceLoad();
    }

    /**
     * Remove playlist with specific ID from DB and list TODO
     *
     * @param playlistId
     */
    private void removePlaylist(final String playlistId) {
        Log.d("kandabashi","removePlaylist");
        YouTubeSqlDb.getInstance().playlists().delete(playlistId);

        for (YouTubePlaylist playlist : playlists) {
            if (playlist.getId().equals(playlistId)) {
                playlists.remove(playlist);
                break;
            }
        }

        playlistsAdapter.notifyDataSetChanged();
    }

    /**
     * Extracts user name from email address
     *
     * @param emailAddress
     * @return
     */
    private String extractUserName(String emailAddress) {
        Log.d("kandabsahi","extraUserName");
        if (emailAddress != null) {
            String[] parts = emailAddress.split("@");
            if (parts.length > 0) {
                if (parts[0] != null) {
                    return parts[0];
                }
            }
        }
        return "";
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
    public void onAddClicked(YouTubeVideo video){
        //何もしない
    }
    @Override
    public void onItemClick(YouTubePlaylist youTubePlaylist) {
        //results are in onVideosReceived callback method
        String id=youTubePlaylist.getId();
        Log.d("kandabashi","PlaylistsFragment-onItemClicked-id:"+id);
        acquirePlaylistVideos(youTubePlaylist.getId());
    }

}