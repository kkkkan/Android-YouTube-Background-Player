package com.smedic.tubtub.fragments;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.ResourceId;
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
        static private ArrayList<YouTubeVideo> playlistDetailList;
        private PlaylistDetailAdapter detailListAdapter;
        private ProgressBar loadingProgressBar;
        private NetworkConf networkConf;
        private Context context;
        private OnItemSelected itemSelected;
        private OnFavoritesSelected onFavoritesSelected;
        private YouTube youTubeWithCredential;
        private SwipeRefreshLayout swipeToRefresh;
        private YouTubePlaylist playlist;
        final private Handler mainHandler=((MainActivity)getActivity()).mainHandler;




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
                    /*更新中のクルクルを止める*/
                    if(swipeToRefresh.isRefreshing()){
                        swipeToRefresh.setRefreshing(false);
                    }
                    return ;
                }

                 /*データ変更したことをお知らせする。*/
                playlistDetailList.clear();
                playlistDetailList.addAll(data);
                detailListAdapter.notifyDataSetChanged();
                 /*更新中のクルクルを止める*/
                if(swipeToRefresh.isRefreshing()){
                    swipeToRefresh.setRefreshing(false);
                }
                for (YouTubeVideo video : playlistDetailList) {
                    Log.d(TAG, "onLoadFinished: >>> " + video.getTitle());
                }
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
        //networkConf = new NetworkConf(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_list, container, false);
        detailFoundListView = (RecyclerView) v.findViewById(R.id.fragment_list_items);
        detailFoundListView.setLayoutManager(new LinearLayoutManager(context));
       // loadingProgressBar = (ProgressBar) v.findViewById(R.id.fragment_progress_bar);
        detailListAdapter = new PlaylistDetailAdapter(context, playlistDetailList);
        detailListAdapter.setOnItemEventsListener(this);
        detailFoundListView.setAdapter(detailListAdapter);
        youTubeWithCredential = YouTubeSingleton.getYouTubeWithCredentials();

        /*swipeで更新*/
        swipeToRefresh = (SwipeRefreshLayout) v.findViewById(R.id.swipe_to_refresh);
        swipeToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(TAG,"onRefresh");
                acquirePlaylistVideos(playlist.getId());
            }
        });

        return v;
    }


    @Override
    public void onResume() {
        super.onResume();
        playlistDetailList.clear();
        Log.d("kandabashi","PlaylistDetailFragment-onResume");
        /*playlistDetailListにデータ詰める。*/
        acquirePlaylistVideos(playlist.getId());
       //detailListAdapter.notifyDataSetChanged();

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


    @Override
    public void onShareClicked(String itemId) {
        share(Config.SHARE_VIDEO_URL + itemId);
    }

    @Override
    public void onFavoriteClicked(YouTubeVideo video, boolean isChecked) {
        onFavoritesSelected.onFavoritesSelected(video, isChecked); // pass event to MainActivity
    }

    /*プレイリストから曲を除く*/
    @Override
    public void onAddClicked(final YouTubeVideo video){

        AlertDialog.Builder dialog=new AlertDialog.Builder(getContext());
        dialog.setTitle("削除").setMessage(video.getTitle()+" を\nプレイリスト"+playlist.getTitle()+"から削除しますか？")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                    /*playlistItemId(playlistidとvideoidの両方の情報が入ったid）を取得*/
                            YouTube.PlaylistItems.List playlistItemRequest = youTubeWithCredential.playlistItems().list("id");
                            playlistItemRequest.setPlaylistId(playlist.getId());
                            playlistItemRequest.setVideoId(video.getId());
                            List<PlaylistItem> playlistItemResult =playlistItemRequest.execute().getItems();
                            int index=playlistItemResult.size();
                            String id=playlistItemResult.get(index-1).getId();
                            youTubeWithCredential.playlistItems().delete(id).execute();
                        }catch (Exception e){
                            Log.d(TAG,"PlaylistDetailFragment-onAddClicked-delete-error-:"+e.getMessage());
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getContext(), "削除に失敗しました。", Toast.LENGTH_LONG).show();
                                    return;
                                }
                            });
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


    @Override
    public void onItemClick(YouTubeVideo video) {
        /*最近見たリスト追加はplaylistselectedでやる！*/
        itemSelected.onPlaylistSelected(playlistDetailList, playlistDetailList.indexOf(video));
    }

    /*viewPager見えるようにし、タッチイベントも復活させる*/
    public void onDestroy(){
        super.onDestroy();
        ViewPager viewPager=((MainActivity)getActivity()).getViewPager();
        viewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        viewPager.setVisibility(View.VISIBLE);
    }



    public void setPlaylist(YouTubePlaylist playlist) {
        this.playlist = playlist;
    }
}


