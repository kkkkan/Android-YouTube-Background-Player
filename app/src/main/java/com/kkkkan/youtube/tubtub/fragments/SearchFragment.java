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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.kkkkan.youtube.R;
import com.kkkkan.youtube.tubtub.MainActivity;
import com.kkkkan.youtube.tubtub.adapters.VideosAdapter;
import com.kkkkan.youtube.tubtub.interfaces.ItemEventsListener;
import com.kkkkan.youtube.tubtub.interfaces.OnFavoritesSelected;
import com.kkkkan.youtube.tubtub.interfaces.OnItemSelected;
import com.kkkkan.youtube.tubtub.model.YouTubePlaylist;
import com.kkkkan.youtube.tubtub.model.YouTubeVideo;
import com.kkkkan.youtube.tubtub.utils.Config;
import com.kkkkan.youtube.tubtub.utils.NetworkConf;
import com.kkkkan.youtube.tubtub.youtube.YouTubeVideosLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Have a Recycleview
 * adapter:VideosAdapter
 * data type:YoutubeVideo
 * <p>
 * Recycleviewを持つ
 * adapter:VideosAdapter
 * dataの型:YoutubeVideo
 * <p>
 * Class that handles list of the videos searched on YouTube
 * Created by smedic on 7.3.16..
 */

public class SearchFragment extends BaseFragment implements ItemEventsListener<YouTubeVideo> {

    private RecyclerView videosFoundListView;
    private List<YouTubeVideo> searchResultsList;
    private VideosAdapter videoListAdapter;
    private ProgressBar loadingProgressBar;
    private NetworkConf networkConf;
    private Context context;
    private OnItemSelected itemSelected;
    private OnFavoritesSelected onFavoritesSelected;

    public SearchFragment() {
        // Required empty public constructor
    }

    public static SearchFragment newInstance() {
        return new SearchFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        searchResultsList = new ArrayList<>();
        networkConf = new NetworkConf(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_list, container, false);
        videosFoundListView = (RecyclerView) v.findViewById(R.id.fragment_list_items);
        videosFoundListView.setLayoutManager(new LinearLayoutManager(context));
        loadingProgressBar = (ProgressBar) v.findViewById(R.id.fragment_progress_bar);
        videoListAdapter = new VideosAdapter(context, searchResultsList);
        videoListAdapter.setOnItemEventsListener(this);
        videosFoundListView.setAdapter(videoListAdapter);

        //disable swipe to refresh for this tab
        v.findViewById(R.id.swipe_to_refresh).setEnabled(false);
        return v;
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
        this.itemSelected = null;
        this.onFavoritesSelected = null;
    }

    /**
     * Search for query on youTube by using YouTube Data API V3
     *
     * @param query
     */
    public void searchQuery(final String query) {
        //check network connectivity
        //When searching, if you are not connected to the network, issue an error.
        //検索するにあたって、ネットワークにつながってなかったらerrorを出す。
        if (!networkConf.isNetworkAvailable()) {
            networkConf.createNetErrorDialog();
            return;
        }

        loadingProgressBar.setVisibility(View.VISIBLE);

        getLoaderManager().restartLoader(1, null, new LoaderManager.LoaderCallbacks<List<YouTubeVideo>>() {
            @Override
            public Loader<List<YouTubeVideo>> onCreateLoader(final int id, final Bundle args) {
                return new YouTubeVideosLoader(context, query);
            }

            @Override
            public void onLoadFinished(Loader<List<YouTubeVideo>> loader, List<YouTubeVideo> data) {
                if (data == null)
                    return;
                videosFoundListView.smoothScrollToPosition(0);
                searchResultsList.clear();
                searchResultsList.addAll(data);
                videoListAdapter.notifyDataSetChanged();
                loadingProgressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onLoaderReset(Loader<List<YouTubeVideo>> loader) {
                searchResultsList.clear();
                searchResultsList.addAll(Collections.<YouTubeVideo>emptyList());
                videoListAdapter.notifyDataSetChanged();
            }
        }).forceLoad();
    }

    @Override
    public void onResume() {
        super.onResume();
        //Notice that the data changed to reflect the results of favorite operation on other tabs
        //他のタブでのfavoriteの操作の結果を反映させるためにデータが変化したことをお知らせ
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
    public void onAddClicked(YouTubeVideo video) {
        onFavoritesSelected.onAddSelected(video); // pass event to MainActivity
    }


    @Override
    public void onItemClick(YouTubeVideo video) {
        //Recently added lists are playlistselected
        //最近見たリスト追加はplaylistselectedでやる！
        itemSelected.onPlaylistSelected(searchResultsList, searchResultsList.indexOf(video));
    }

    @Override
    public void onDeleteClicked(YouTubeVideo video) {

    }

    @Override
    public void onDeleteClicked(YouTubePlaylist playlist) {

    }
}
