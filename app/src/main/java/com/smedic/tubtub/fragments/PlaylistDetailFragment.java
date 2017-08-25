package com.smedic.tubtub.fragments;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.google.api.services.youtube.YouTube;
import com.smedic.tubtub.MainActivity;
import com.smedic.tubtub.R;
import com.smedic.tubtub.adapters.PlaylistDetailAdapter;
import com.smedic.tubtub.adapters.VideosAdapter;
import com.smedic.tubtub.database.YouTubeSqlDb;
import com.smedic.tubtub.interfaces.ItemEventsListener;
import com.smedic.tubtub.interfaces.OnFavoritesSelected;
import com.smedic.tubtub.interfaces.OnItemSelected;
import com.smedic.tubtub.model.YouTubePlaylist;
import com.smedic.tubtub.model.YouTubeVideo;
import com.smedic.tubtub.utils.Config;
import com.smedic.tubtub.utils.NetworkConf;
import com.smedic.tubtub.youtube.YouTubePlaylistVideosLoader;
import com.smedic.tubtub.youtube.YouTubeSingleton;
import com.smedic.tubtub.youtube.YouTubeVideosLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class PlaylistDetailFragment extends BaseFragment implements ItemEventsListener<YouTubeVideo> {

        private static final String TAG = "SMEDIC search frag";
        private RecyclerView detailFoundListView;
        static private List<YouTubeVideo> playlistDetailList;
        private PlaylistDetailAdapter detailListAdapter;
        private ProgressBar loadingProgressBar;
        private NetworkConf networkConf;
        private Context context;
        private OnItemSelected itemSelected;
        private OnFavoritesSelected onFavoritesSelected;
        private YouTube youTubeWithCredential;
        private String playlistId;



    public void setPlaylistId(String playlistId) {
        this.playlistId = playlistId;
    }

    private  void acquirePlaylistVideos(final String playlistId) {
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
                    Log.d("kandabashi","PlaylistsFragment.acquirePlaylistVideos.onLoadFinished-empty");
                    return ;
                }
                playlistDetailList=data;
                 /*データ変更したことをお知らせする。*/
                detailListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLoaderReset(Loader<List<YouTubeVideo>> loader) {
                Log.d("kandabashi","PlaylistsFragment.acquirePlaylistVideos.onLoaderReset");
                playlistDetailList.clear();
                playlistDetailList.addAll(Collections.<YouTubeVideo>emptyList());
            }
        }).forceLoad();
    }

    public PlaylistDetailFragment() {
        // Required empty public constructor
    }

    public static PlaylistDetailFragment newInstance() {
        return new PlaylistDetailFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playlistDetailList = new ArrayList<>();
        networkConf = new NetworkConf(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_list, container, false);
        detailFoundListView = (RecyclerView) v.findViewById(R.id.fragment_list_items);
        detailFoundListView.setLayoutManager(new LinearLayoutManager(context));
        loadingProgressBar = (ProgressBar) v.findViewById(R.id.fragment_progress_bar);
        detailListAdapter = new PlaylistDetailAdapter(context, playlistDetailList);
        detailListAdapter.setOnItemEventsListener(this);
        detailFoundListView.setAdapter(detailListAdapter);
        youTubeWithCredential = YouTubeSingleton.getYouTubeWithCredentials();

        //disable swipe to refresh for this tab
        v.findViewById(R.id.swipe_to_refresh).setEnabled(false);
        return v;
    }


    @Override
    public void onResume() {
        super.onResume();
        playlistDetailList.clear();
        Log.d("kandabashi","PlaylistDetailFragment-onResume");
        /*playlistDetailListにデータ詰める。*/
        acquirePlaylistVideos(playlistId);

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
                detailFoundListView.smoothScrollToPosition(0);
                playlistDetailList.clear();
                playlistDetailList.addAll(data);
                detailListAdapter.notifyDataSetChanged();
                loadingProgressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onLoaderReset(Loader<List<YouTubeVideo>> loader) {
                playlistDetailList.clear();
                playlistDetailList.addAll(Collections.<YouTubeVideo>emptyList());
                detailListAdapter.notifyDataSetChanged();
            }
        }).forceLoad();
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
        /*最近見たリスト追加はplaylistselectedでやる！*/
        itemSelected.onPlaylistSelected(playlistDetailList, playlistDetailList.indexOf(video));
    }
}


