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


import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.kkkkan.youtube.R;
import com.kkkkan.youtube.tubtub.MainActivity;
import com.kkkkan.youtube.tubtub.adapters.PlaylistDetailAdapter;
import com.kkkkan.youtube.tubtub.interfaces.ItemEventsListener;
import com.kkkkan.youtube.tubtub.interfaces.OnFavoritesSelected;
import com.kkkkan.youtube.tubtub.interfaces.OnItemSelected;
import com.kkkkan.youtube.tubtub.model.YouTubePlaylist;
import com.kkkkan.youtube.tubtub.model.YouTubeVideo;
import com.kkkkan.youtube.tubtub.utils.Config;
import com.kkkkan.youtube.tubtub.youtube.YouTubePlaylistVideosLoader;
import com.kkkkan.youtube.tubtub.youtube.YouTubeSingleton;

import java.util.ArrayList;
import java.util.List;


/**
 * 想定されている使い方
 * MainActivityにAttachされる
 * AttachされるactivityがOnItemSelectedのinstanceである
 * parentFragmetntがOnFavoritesSelectedのinstanceである
 * <p>
 * <p>
 * Have a Recycleview
 * adapter:PlaylistDetailAdapter
 * data type: YoutubeVideo
 * <p>
 * Recycleviewを持つ
 * adapter:PlaylistDetailAdapter
 * dataの型:YoutubeVideo
 * <p>
 * A simple {@link Fragment} subclass.
 */

public class PlaylistDetailFragment extends BaseFragment implements ItemEventsListener<YouTubeVideo> {

    private static final String TAG = "PlaylistDetailFragment";
    static private ArrayList<YouTubeVideo> playlistDetailList;
    final private Handler mainHandler = ((MainActivity) getActivity()).mainHandler;
    private RecyclerView detailFoundListView;
    private PlaylistDetailAdapter detailListAdapter;
    private Context context;
    private OnItemSelected itemSelected;
    private OnFavoritesSelected onFavoritesSelected;
    private YouTube youTubeWithCredential;
    private SwipeRefreshLayout swipeToRefresh;
    private YouTubePlaylist playlist;
    private int deleteVideoIndex;
    private ProgressDialog progressDialog;

    private String playlistTitle;

    public PlaylistDetailFragment() {
        // Required empty public constructor
    }

    public static PlaylistDetailFragment newInstance(String playlistTitle) {
        PlaylistDetailFragment fragment = new PlaylistDetailFragment();
        fragment.playlistTitle = playlistTitle;
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        this.context = context;
        if (context instanceof OnItemSelected) {
            itemSelected = (OnItemSelected) context;
        }
        Fragment fragment = getParentFragment();
        if (fragment instanceof OnFavoritesSelected) {
            onFavoritesSelected = (OnFavoritesSelected) fragment;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playlistDetailList = new ArrayList<>();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_playlist_detail_root, container, false);
        detailFoundListView = (RecyclerView) v.findViewById(R.id.fragment_list_items);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        detailFoundListView.setLayoutManager(linearLayoutManager);
        //区切り線追加
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(detailFoundListView.getContext(),
                linearLayoutManager.getOrientation());
        detailFoundListView.addItemDecoration(dividerItemDecoration);

        detailListAdapter = new PlaylistDetailAdapter(context, playlistDetailList);
        detailListAdapter.setOnItemEventsListener(this);
        detailFoundListView.setAdapter(detailListAdapter);
        youTubeWithCredential = YouTubeSingleton.getYouTubeWithCredentials();
        progressDialog = new ProgressDialog(getActivity());



        /*swipeで更新*/
        swipeToRefresh = (SwipeRefreshLayout) v.findViewById(R.id.swipe_to_refresh);
        swipeToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(TAG, "onRefresh");
                acquirePlaylistVideos(playlist);
            }
        });

        TextView textView = (TextView) v.findViewById(R.id.title_view);
        textView.setText(playlistTitle);

        playlistDetailList.clear();

        //I will show a dialog of Loading ....
        //Loading…のダイアログ出す。
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Loading…");
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
        //Dialog display
        //ダイアログ表示
        progressDialog.show();
        //Stuff data into playlistDetailList.
        //playlistDetailListにデータ詰める。
        acquirePlaylistVideos(playlist);

        return v;
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "PlaylistDetailFragment-onResume");
    }


    //Make it viewPager visible, and restore touch events
    //viewPager見えるようにし、タッチイベントも復活させる
    @Override
    public void onDestroyView() {
        super.onDestroyView();

    }

    public void onDestroy() {
        super.onDestroy();

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


    @Override
    public void onAddClicked(final YouTubeVideo video) {
        //do nothing
        //何もしない
    }

    private void acquirePlaylistVideos(final YouTubePlaylist playlist) {
        Log.d(TAG, "acquirePlaylistVideos");
        getLoaderManager().restartLoader(Config.YouTubePlaylistDetailLoaderId, null, new LoaderManager.LoaderCallbacks<List<YouTubeVideo>>() {
            //このフラグがないとこのfragmentに戻ったときなぜか onLoadFinishedが呼ばれてしまうことがある
            boolean loaderRunning = false;

            @Override
            public Loader<List<YouTubeVideo>> onCreateLoader(final int id, final Bundle args) {
                Log.d(TAG, "PlaylistsFragment.acquirePlaylistVideos.onCreateLoader-id:" + playlist.getTitle());
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
                    Log.d(TAG, "PlaylistsFragment.acquirePlaylistVideos.onLoadFinished-empty");
                    //If you get out Loading ...
                    //もしLoading…出てたら消す
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    //Stop the update while updating
                    //更新中のクルクルを止める
                    if (swipeToRefresh.isRefreshing()) {
                        swipeToRefresh.setRefreshing(false);
                    }
                    return;
                }

                //We inform you that we have changed the data.
                //データ変更したことをお知らせする。
                playlistDetailList.clear();
                playlistDetailList.addAll(data);
                detailListAdapter.notifyDataSetChanged();
                //If you get out Loading ...
                //もしLoading…出てたら消す。
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                //Stop the update while updating
                //更新中のクルクルを止める
                if (swipeToRefresh.isRefreshing()) {
                    swipeToRefresh.setRefreshing(false);
                }
                for (YouTubeVideo video : playlistDetailList) {
                    //Log.d(TAG, "onLoadFinished: >>> " + video.getTitle());
                }
            }

            @Override
            public void onLoaderReset(Loader<List<YouTubeVideo>> loader) {
                Log.d(TAG, "PlaylistsFragment.acquirePlaylistVideos.onLoaderReset");
                //If you get out Loading ...
                //もしLoading…出てたら消す。
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                //Stop the update while updating
                //更新中のクルクルを止める*/
                if (swipeToRefresh.isRefreshing()) {
                    swipeToRefresh.setRefreshing(false);
                }
            }
        }).forceLoad();
    }

    @Override
    public void onItemClick(YouTubeVideo video) {
        //Recently added lists are playlistselected
        //最近見たリスト追加はplaylistselectedでやる
        itemSelected.onPlaylistSelected(playlistDetailList, playlistDetailList.indexOf(video));
    }

    /**
     * Remove songs from playlist
     * <p>
     * プレイリストから曲を除く
     */
    @Override
    public void onDeleteClicked(final YouTubeVideo video) {
        //Give a confirmation dialog of deletion.
        //削除の確認のダイアログを出す。
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle("削除").setMessage(video.getTitle() + " を\nプレイリスト" + playlist.getTitle() + "から削除しますか？")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    //Get playlistItemId (id with information of both playlistid and videoid)
                                    //playlistItemId(playlistidとvideoidの両方の情報が入ったid）を取得
                                    YouTube.PlaylistItems.List playlistItemRequest = youTubeWithCredential.playlistItems().list("id,snippet");
                                    playlistItemRequest.setPlaylistId(playlist.getId());
                                    playlistItemRequest.setVideoId(video.getId());
                                    List<PlaylistItem> playlistItemResult = playlistItemRequest.execute().getItems();
                                    //In the playlist including deleted video, the position of the video below the first delete video is not captured correctly (probably API bug),
                                    // so if the same deleted video is included in one playlist it can not be deleted It's going down.
                                    //deleted video の含まれるプレイリストでは、最初のdelete video以下のvideoはpositionが正しく取ってこれない（恐らくAPIバグ）ので、
                                    //同じdeleted videoが一つのプレイリストに含まれていた場合削除ができない仕様になってる。
                                    String id = null;
                                    if (playlistItemResult.size() > 1) {
                                        for (PlaylistItem p : playlistItemResult) {
                                            if (p.getSnippet().getPosition() == deleteVideoIndex) {
                                                id = p.getId();
                                                break;
                                            }
                                        }
                                    } else {
                                        id = playlistItemResult.get(0).getId();
                                    }
                                    if (id == null || id.isEmpty()) {
                                        throw new Exception("ID IS EMPTY");
                                    }
                                    youTubeWithCredential.playlistItems().delete(id).execute();

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

    @Override
    public void onDeleteClicked(YouTubePlaylist playlist) {

    }


    public void setPlaylist(YouTubePlaylist playlist) {
        this.playlist = playlist;
    }

    public void setDeleteVideoIndex(int deleteVideoIndex) {
        this.deleteVideoIndex = deleteVideoIndex;
    }
}


