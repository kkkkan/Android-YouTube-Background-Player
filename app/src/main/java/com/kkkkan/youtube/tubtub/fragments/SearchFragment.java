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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RadioGroup;

import com.kkkkan.youtube.R;
import com.kkkkan.youtube.tubtub.MainActivity;
import com.kkkkan.youtube.tubtub.adapters.PlaylistsAdapter;
import com.kkkkan.youtube.tubtub.adapters.VideosAdapter;
import com.kkkkan.youtube.tubtub.interfaces.ItemEventsListener;
import com.kkkkan.youtube.tubtub.interfaces.OnFavoritesSelected;
import com.kkkkan.youtube.tubtub.interfaces.OnItemSelected;
import com.kkkkan.youtube.tubtub.model.YouTubePlaylist;
import com.kkkkan.youtube.tubtub.model.YouTubeVideo;
import com.kkkkan.youtube.tubtub.utils.Config;
import com.kkkkan.youtube.tubtub.utils.NetworkConf;
import com.kkkkan.youtube.tubtub.utils.PlaylistsCache;
import com.kkkkan.youtube.tubtub.youtube.VideosLoaderMethods;
import com.kkkkan.youtube.tubtub.youtube.YouTubePlaylistVideosLoader;
import com.kkkkan.youtube.tubtub.youtube.YouTubeVideosLoader;

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

public class SearchFragment extends BaseFragment {
    private static final String TAG = "SearchFragment";
    private final int tag = PlaylistsCache.tag;
    private RecyclerView videosFoundListView;
    private List<YouTubeVideo> searchResultsVideoList;
    private List<YouTubePlaylist> searchResultsPlaylistList;
    private VideosAdapter videoListAdapter;
    private PlaylistsAdapter playlistsAdapter;
    private NetworkConf networkConf;
    private Context context;
    private OnItemSelected itemSelected;
    private OnFavoritesSelected onFavoritesSelected;
    private RadioGroup radioGroup;
    private ProgressDialog progressDialog;
    /**
     * videoの検索結果をクリックしたときの動きのためのItemEventsListener
     */
    private ItemEventsListener<YouTubeVideo> videoItemEventsListener = new ItemEventsListener<YouTubeVideo>() {
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
            itemSelected.onPlaylistSelected(searchResultsVideoList, searchResultsVideoList.indexOf(video));
        }

        @Override
        public void onDeleteClicked(YouTubeVideo video) {

        }

        @Override
        public void onDeleteClicked(YouTubePlaylist playlist) {

        }
    };
    /**
     * playlistの検索結果をクリックしたときの動きのためのItemEventsListener
     */
    private ItemEventsListener<YouTubePlaylist> playlistItemEventsListener = new ItemEventsListener<YouTubePlaylist>() {
        @Override
        public void onShareClicked(String itemId) {
            share(Config.SHARE_PLAYLIST_URL + itemId);
        }

        @Override
        public void onFavoriteClicked(YouTubeVideo video, boolean isChecked) {

        }

        @Override
        public void onAddClicked(YouTubeVideo video) {

        }

        @Override
        public void onItemClick(YouTubePlaylist playlist) {
            //results are in onVideosReceived callback method
            Log.d(TAG, "onItemClick");
            String id = playlist.getId();
            Log.d(TAG, "PlaylistsFragment-onItemClicked-id:" + id);
            acquirePlaylistVideos(playlist);
        }

        @Override
        public void onDeleteClicked(YouTubeVideo video) {

        }

        @Override
        public void onDeleteClicked(YouTubePlaylist playlist) {

        }
    };

    public SearchFragment() {
        // Required empty public constructor
        PlaylistsCache.tag++;
    }

    public static SearchFragment newInstance() {
        SearchFragment fragment = new SearchFragment();
        Log.d(TAG, "newInstance()");
        Log.d(TAG, "tag is : " + String.valueOf(fragment.tag));
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        super.onAttach(context);
        Log.d(TAG, "onAttach");
        if (context instanceof MainActivity) {
            this.context = context;
            itemSelected = (MainActivity) context;
        }
        Fragment fragment = getParentFragment();
        if (fragment instanceof OnFavoritesSelected) {
            onFavoritesSelected = (OnFavoritesSelected) fragment;
        }
        networkConf = new NetworkConf((Activity) context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        searchResultsVideoList = PlaylistsCache.Instance.getSearchResultsVideoList();
        searchResultsPlaylistList = PlaylistsCache.Instance.getSearchResultsPlaylistList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        super.onCreateView(inflater, container, savedInstanceState);
        Log.d(TAG, "onCreateView");
        Log.d(TAG, "tag is : " + String.valueOf(tag));
        View v = inflater.inflate(R.layout.fragment_search, container, false);
        videosFoundListView = (RecyclerView) v.findViewById(R.id.fragment_list_items);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        videosFoundListView.setLayoutManager(linearLayoutManager);
        //区切り線追加
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(videosFoundListView.getContext(),
                linearLayoutManager.getOrientation());
        videosFoundListView.addItemDecoration(dividerItemDecoration);

        //Searching…ダイアログのための設定
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Searching…");
        //Make the color of the back of the dialog transparent.
        //ダイアログの裏の色を透明にする。
        progressDialog.getWindow().setFlags(0, WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        //Make the place where the dialog will appear in the middle of playlistDetailFragment.
        //ダイアログの出す場所をplaylistDetailFragmentの真ん中にする。
        //Get screen height
        //画面の高さを取得
        Point size = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(size);
        int windowHeight = size.y;

        //Get the height of playlistDetailFragment
        //playlistDetailFragmentの高さを取得
        // FrameLayout frameLayout = (FrameLayout) getView().findViewById(R.id.frame_layout);
        int framelayoutHeight = v.getHeight();

        //Set in the middle of playlistDetailFragment
        //playlistDetailFragmentの真ん中にセット
        //WindowManager.LayoutParams.y px specified in the screen is displayed shifted downward from the center of the screen.
        // WindowManager.LayoutParams.yに指定したpx分画面真ん中から下にずらされて表示される。
        WindowManager.LayoutParams layoutParams = progressDialog.getWindow().getAttributes();
        layoutParams.y = (windowHeight - framelayoutHeight) / 2;
        progressDialog.getWindow().setAttributes(layoutParams);

        //検索結果（ビデオ）を表示するための設定
        videoListAdapter = new VideosAdapter(context, searchResultsVideoList);
        videoListAdapter.setOnItemEventsListener(videoItemEventsListener);
        //検索結果（プレイリスト）を表示するための設定
        playlistsAdapter = new PlaylistsAdapter(context, searchResultsPlaylistList, false);
        playlistsAdapter.setOnDetailClickListener((PlaylistsAdapter.OnDetailClickListener) getParentFragment());
        playlistsAdapter.setOnItemEventsListener(playlistItemEventsListener);


        radioGroup = (RadioGroup) v.findViewById(R.id.radio_group);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                setAdapter();
                notifyDataSetChanged();
            }
        });

        setAdapter();

        //disable swipe to refresh for this tab
        v.findViewById(R.id.swipe_to_refresh).setEnabled(false);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        //Notice that the data changed to reflect the results of favorite operation on other tabs
        //他のタブでのfavoriteの操作の結果を反映させるためにデータが変化したことをお知らせ
        notifyDataSetChanged();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach");
        this.context = null;
        this.itemSelected = null;
        this.onFavoritesSelected = null;
    }

    private void setAdapter() {
        switch (radioGroup.getCheckedRadioButtonId()) {
            case R.id.video:
                videosFoundListView.setAdapter(videoListAdapter);
                break;
            case R.id.playlist:
                videosFoundListView.setAdapter(playlistsAdapter);
                break;
        }
    }

    private void notifyDataSetChanged() {
        Log.d(TAG, "notifyDataSetChanged()");
        switch (radioGroup.getCheckedRadioButtonId()) {
            case R.id.video:
                videoListAdapter.notifyDataSetChanged();
                break;
            case R.id.playlist:
                playlistsAdapter.notifyDataSetChanged();
                break;
        }
    }

    /**
     * Search for query on youTube by using YouTube Data API V3
     *
     * @param query
     */
    public void searchQuery(final String query) {
        Log.d(TAG, "searchQuery : " + query);
        //check network connectivity
        //When searching, if you are not connected to the network, issue an error.
        //検索するにあたって、ネットワークにつながってなかったらerrorを出す。
        if (!networkConf.isNetworkAvailable()) {
            networkConf.createNetErrorDialog();
            return;
        }

        getLoaderManager().restartLoader(Config.YouTubeVideosLoaderId, null, new LoaderManager.LoaderCallbacks<Pair<VideosLoaderMethods.SearchResultVideo, VideosLoaderMethods.SearchResultPlaylist>>() {
            @Override
            public Loader<Pair<VideosLoaderMethods.SearchResultVideo, VideosLoaderMethods.SearchResultPlaylist>> onCreateLoader(final int id, final Bundle args) {
                showProgressDialog();
                return new YouTubeVideosLoader(context, query);
            }

            @Override
            public void onLoadFinished(Loader<Pair<VideosLoaderMethods.SearchResultVideo, VideosLoaderMethods.SearchResultPlaylist>> loader, Pair<VideosLoaderMethods.SearchResultVideo, VideosLoaderMethods.SearchResultPlaylist> data) {
                Log.d(TAG, "onLoadFinished");
                if (data == null)
                    return;
                PlaylistsCache.Instance.setSearchResultsList(data);
                videosFoundListView.smoothScrollToPosition(0);
                searchResultsVideoList.clear();
                searchResultsVideoList.addAll(data.first.getResultVideos());
                searchResultsPlaylistList.clear();
                searchResultsPlaylistList.addAll(data.second.getResultPlaylists());
                notifyDataSetChanged();
                hideProgressDialog();
            }

            @Override
            public void onLoaderReset(Loader<Pair<VideosLoaderMethods.SearchResultVideo, VideosLoaderMethods.SearchResultPlaylist>> loader) {
                searchResultsVideoList.clear();
                searchResultsVideoList.addAll(Collections.<YouTubeVideo>emptyList());
                searchResultsPlaylistList.clear();
                searchResultsPlaylistList.addAll(Collections.<YouTubePlaylist>emptyList());
                notifyDataSetChanged();
            }
        }).forceLoad();
    }

    private void acquirePlaylistVideos(final YouTubePlaylist playlist) {
        Log.d(TAG, "acquirePlaylistVideos");
        getLoaderManager().restartLoader(Config.YouTubePlaylistDetailLoaderId, null, new LoaderManager.LoaderCallbacks<List<YouTubeVideo>>() {
            //このフラグがないとこのfragmentに戻ったときなぜか onLoadFinishedが呼ばれてしまうことがある
            boolean loaderRunning = false;

            @Override
            public Loader<List<YouTubeVideo>> onCreateLoader(final int id, final Bundle args) {
                Log.d(TAG, "PlaylistsFragment.acquirePlaylistVideos.onCreateLoader-id:" + playlist.getId());
                loaderRunning = true;
                return new YouTubePlaylistVideosLoader(context, playlist);
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
                itemSelected.onPlaylistSelected(data, 0);
            }

            @Override
            public void onLoaderReset(Loader<List<YouTubeVideo>> loader) {
                Log.d(TAG, "PlaylistsFragment.acquirePlaylistVideos.onLoaderReset");

            }
        }).forceLoad();
    }

    /**
     * Searching…のダイアログを出す
     */
    private void showProgressDialog() {
        progressDialog.show();
    }

    /**
     * Searching…のダイアログを消す
     */
    private void hideProgressDialog() {
        if (progressDialog.isShowing()) {
            progressDialog.hide();
        }
    }


}
