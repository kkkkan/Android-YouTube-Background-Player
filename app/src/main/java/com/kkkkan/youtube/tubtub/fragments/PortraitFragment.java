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
import android.app.SearchManager;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.MatrixCursor;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.PlaylistSnippet;
import com.google.api.services.youtube.model.PlaylistStatus;
import com.google.api.services.youtube.model.ResourceId;
import com.kkkkan.youtube.BuildConfig;
import com.kkkkan.youtube.R;
import com.kkkkan.youtube.tubtub.MainActivityViewModel;
import com.kkkkan.youtube.tubtub.adapters.PlaylistsAdapter;
import com.kkkkan.youtube.tubtub.database.YouTubeSqlDb;
import com.kkkkan.youtube.tubtub.interfaces.LoginHandler;
import com.kkkkan.youtube.tubtub.interfaces.OnFavoritesSelected;
import com.kkkkan.youtube.tubtub.interfaces.SurfaceHolderListener;
import com.kkkkan.youtube.tubtub.interfaces.TitlebarListener;
import com.kkkkan.youtube.tubtub.model.YouTubePlaylist;
import com.kkkkan.youtube.tubtub.model.YouTubeVideo;
import com.kkkkan.youtube.tubtub.utils.Config;
import com.kkkkan.youtube.tubtub.utils.NetworkConf;
import com.kkkkan.youtube.tubtub.utils.PlaylistsCash;
import com.kkkkan.youtube.tubtub.utils.Settings;
import com.kkkkan.youtube.tubtub.utils.VideoQualitys;
import com.kkkkan.youtube.tubtub.youtube.SuggestionsLoader;
import com.kkkkan.youtube.tubtub.youtube.YouTubeVideosLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.kkkkan.youtube.R.layout.suggestions;
import static com.kkkkan.youtube.tubtub.youtube.YouTubeSingleton.getYouTubeWithCredentials;

/**
 * A simple {@link Fragment} subclass.
 */
public class PortraitFragment extends Fragment implements OnFavoritesSelected, PlaylistsAdapter.OnDetailClickListener, SurfaceHolder.Callback {
    final private static String TAG = "PortraitFragment";

    final private static String tabLayoutFragmentTAG = "tabLayoutFragmentTAG";
    final private static String playlistDetailFragmentTAG = "playlistDetailFragmentTAG";
    final private static String nowPlayingListFragmentTAG = "nowPlayingListFragmentTAG";


    final private static String tabLayoutFragmentBackstackTAG = "tabLayoutFragmentBackstackTAG";
    final private static String playlistDetailBackstackTAG = "playlistDetailBackstackTAG";
    final private static String nowplayingFragmentBackstackTAG = "nowplayingFragmentBackstackTAG";

    private Toolbar toolbar;
    private TitlebarListener titlebarListener;
    private CheckBox repeatOneBox;
    private CheckBox repeatPlaylistBox;
    private CheckBox nowPlayingListBox;
    private SurfaceView surfaceView;
    private Handler handler = new Handler();

    //For movie title
    //動画タイトル用
    private TextView titleView;
    private MainActivityViewModel viewModel;

    /**
     * When making a new instance of PortraitFragment make sure to make with this mezzo
     * <p>
     * PortraitFragmentの新しいインスタンスを作るときは必ず
     * このメゾッドで作ること
     * <p>
     * 再生成時も必ずこのメゾッドを呼んで新しいインスタンス作るようにしているのでviewModelなども入れてしまう。
     * <p>
     */
    static public PortraitFragment getNewPortraitFragment(TitlebarListener titlebarListener, MainActivityViewModel viewModel) {
        PortraitFragment fragment = new PortraitFragment();
        fragment.titlebarListener = titlebarListener;
        fragment.viewModel = viewModel;
        return fragment;
    }

    public PortraitFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onattach");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        super.onCreateView(inflater,container,savedInstanceState);
        final View view = inflater.inflate(R.layout.fragment_portrait, container, false);
        if (savedInstanceState == null) {
            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            TabLayoutFragment fragment = TabLayoutFragment.newInstance();
            ft.add(R.id.tab_and_viewpager, fragment, tabLayoutFragmentTAG);
            ft.commit();
        }
        titleView = (TextView) view.findViewById(R.id.title_view);
        viewModel.getVideoTitle().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                titleView.setText(s);
            }
        });
        surfaceView = (SurfaceView) view.findViewById(R.id.surface);
        surfaceView.getHolder().addCallback(this);
        //Change the vertical and horizontal lengths of the playback space of the movie according to the screen size of the smartphone
        //スマホの画面サイズに合わせて動画の再生スペースの縦横の長さを変化させる
        surfaceView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (surfaceView.isEnabled()) {
                    ViewGroup.LayoutParams svlp = surfaceView.getLayoutParams();
                    int width = surfaceView.getWidth();
                    svlp.height = width / 16 * 9;
                    surfaceView.setLayoutParams(svlp);
                }
            }
        });
        //Setting for one repeat check box on the title bar
        //タイトルバーの1リピートチェックボックスについての設定
        repeatOneBox = (CheckBox) view.findViewById(R.id.repeat_one_box);
        repeatOneBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                titlebarListener.repeatOneCheckListener();
                checkBoxUpdata();
            }
        });
        //Settings for the playlist repeat checkbox on the title bar
        //タイトルバーのプレイリストリピートチェックボックスについての設定
        repeatPlaylistBox = (CheckBox) view.findViewById(R.id.repeat_playlist_box);
        repeatPlaylistBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                titlebarListener.repeatPlaylistCheckListener();
                checkBoxUpdata();
            }
        });

        //タイトルバーの現在再生中リストのチェックボックスについての設定
        nowPlayingListBox = (CheckBox) view.findViewById(R.id.now_playlinglist_show_button);
        nowPlayingListBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                nowPlayingListBoxHandler(isChecked);
            }
        });

        //nowPlayingListBoxのタッチ面積を増やす
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int dp = 4;
                int px = (int) (view.getResources().getDisplayMetrics().density * dp);
                Rect delegateArea = new Rect();
                nowPlayingListBox.getHitRect(delegateArea);
                delegateArea.top -= px;
                delegateArea.left -= px;
                delegateArea.right += px;
                delegateArea.bottom += px;
                ((View) nowPlayingListBox.getParent()).setTouchDelegate(new TouchDelegate(delegateArea, nowPlayingListBox));
            }
        });
        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setTitle(getString(R.string.app_name));
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                return onOptionsItemSelected(menuItem);
            }
        });

        final SearchView searchView = (SearchView) toolbar.getMenu().findItem(R.id.action_search).getActionView();


        final CursorAdapter suggestionAdapter = new SimpleCursorAdapter(getActivity(),
                suggestions,
                null,
                new String[]{SearchManager.SUGGEST_COLUMN_TEXT_1},
                new int[]{android.R.id.text1},
                0);
        final List<String> suggestions = new ArrayList<>();

        //setSuggestionsAdaper:独自のadapterをセットする
        searchView.setSuggestionsAdapter(suggestionAdapter);

        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            //suggestionをスクロールしたとき
            @Override
            public boolean onSuggestionSelect(int position) {
                Log.d(TAG, "onSuggestionSelect");
                return false;
            }

            //スクロールを選んだ時検索
            @Override
            public boolean onSuggestionClick(int position) {
                Log.d(TAG, "onSuggestionClick");
                startSearch(searchView, suggestions.get(position));
                return false;
            }
        });

        //検索窓の文字が変わるたび呼び出される
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG, "onQueryTextSubmit");
                startSearch(searchView, s);
                return false; //if true, no new intent is started
            }


            /*検索ワードが変わったら*/
            @Override
            public boolean onQueryTextChange(final String query) {
                Log.d(TAG, "onQueryTextChange");
                // check network connection. If not available, do not query.
                // this also disables onSuggestionClick triggering
                //二文字以上入力されてたら
                if (query.length() > 1) { //make suggestions after 3rd letter
                    Log.d(TAG, "onQueryTextChange:query.length() > 1");
                    if (new NetworkConf(getActivity()).isNetworkAvailable()) {
                        Log.d(TAG, "onQueryTextChange:NetworkConf(getActivity()).isNetworkAvailable()");
                        getActivity().getSupportLoaderManager().restartLoader(Config.SuggestionsLoaderId, null, new LoaderManager.LoaderCallbacks<List<String>>() {
                            @Override
                            public Loader<List<String>> onCreateLoader(final int id, final Bundle args) {
                                return new SuggestionsLoader(getActivity().getApplicationContext(), query);
                            }

                            @Override
                            public void onLoadFinished(Loader<List<String>> loader, List<String> data) {
                                Log.d(TAG, "onQueryTextChange:onLoadFinished");
                                if (data == null)
                                    return;
                                suggestions.clear();
                                suggestions.addAll(data);
                                String[] columns = {
                                        BaseColumns._ID,
                                        SearchManager.SUGGEST_COLUMN_TEXT_1
                                };
                                MatrixCursor cursor = new MatrixCursor(columns);

                                for (int i = 0; i < data.size(); i++) {
                                    String[] tmp = {Integer.toString(i), data.get(i)};
                                    cursor.addRow(tmp);
                                }
                                suggestionAdapter.swapCursor(cursor);
                            }

                            @Override
                            public void onLoaderReset(Loader<List<String>> loader) {
                                suggestions.clear();
                                suggestions.addAll(Collections.<String>emptyList());
                            }
                        }).forceLoad();
                        return false;
                    }
                }
                Log.d(TAG, "onQueryTextChange:query.length() = 0");
                return false;
            }


        });
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        checkBoxUpdata();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach");
    }

    /**
     * お気に入りfragmentにビデオを追加したり削除したり
     */
    @Override
    public void onFavoritesSelected(YouTubeVideo video, boolean isChecked) {
        if (isChecked) {
            FavoritesFragment.addToFavoritesList(video);
        } else {
            Fragment fragment = getChildFragmentManager().findFragmentByTag(tabLayoutFragmentTAG);
            if (fragment instanceof TabLayoutFragment) {
                //今表示がviewPagerの乗っているfragmentだったらviewpager上のfavoritefragmentにおしらせ
                ((TabLayoutFragment) fragment).removeFromFavorites(video);
            } else {
                YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE).deleteByVideoId(video.getId());
            }
        }
    }


    /**
     * プレイリストに追加したり、プレイリストを新規作成したうえで追加したり
     */
    @Override
    public void onAddSelected(final YouTubeVideo video) {
        Log.d(TAG, "onAddSelected");
        final AlertDialog.Builder mListDlg = new AlertDialog.Builder(getActivity());
        final AlertDialog.Builder mTitleDlg = new AlertDialog.Builder(getActivity());
        //video タイトル取得
        final String videoTitle = video.getTitle();

        /*プレイリストを取得*/
        final ArrayList<YouTubePlaylist> allPlaylist = YouTubeSqlDb.getInstance().playlists().readAll();
        Log.d(TAG, "AddPlaylistDialog-1-which:" + String.valueOf(allPlaylist.size()));

        /*プレイリストタイトルを取得*/
        final CharSequence[] playlists = new CharSequence[allPlaylist.size() + 1];
        int i = 0;
        for (YouTubePlaylist p : allPlaylist) {
            playlists[i++] = p.getTitle() + "(" + String.valueOf(p.getNumberOfVideos()) + ")";
        }
        playlists[i++] = "新規作成して追加";

        /*チェックされたやつの番号を入れておく。*/
        final ArrayList<Integer> checkedItems = new ArrayList<Integer>();
        checkedItems.add(0);

/*ダイアログ表示のための準備*/
        mListDlg.setTitle("プレイリスト追加：\n" + videoTitle);

        mListDlg.setSingleChoiceItems(playlists, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkedItems.clear();
                checkedItems.add(which);
            }
        });
        mListDlg.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "AddPlaylistDialog-1-which:" + String.valueOf(which));
                if (!checkedItems.isEmpty()) {
                    if (checkedItems.get(0) == (playlists.length - 1)) {
                        //新しいプレイリスト作って追加
                        final EditText titleEdit = new EditText(getActivity());
                        mTitleDlg.setTitle("新規プレイリスト名入力");
                        mTitleDlg.setView(titleEdit);
                        mTitleDlg.setPositiveButton("非公開で作成", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final String title = titleEdit.getText().toString();
                                if (title.length() == 0) {
                                    Toast.makeText(getActivity(), "プレイリスト名は空白は認められません。", Toast.LENGTH_LONG).show();
                                } else {
                                    AddPlaylist(title, "private", video);
                                }
                            }
                        });
                        mTitleDlg.setNeutralButton("公開で作成", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final String title = titleEdit.getText().toString();
                                if (title.length() == 0) {
                                    Toast.makeText(getActivity(), "プレイリスト名は空白は認められません。", Toast.LENGTH_LONG).show();
                                } else {
                                    AddPlaylist(title, "public", video);
                                }
                            }
                        });

                        mTitleDlg.setNegativeButton("cancel", null);
                        mTitleDlg.show();


                    } else {
                        AddVideoToPlayList(allPlaylist.get(checkedItems.get(0)).getId(), video, true, "", "");
                    }
                }
            }
        });

        mListDlg.setNegativeButton("キャンセル", null);

        mListDlg.create().show();
    }

    /**
     * 新規プレイリスト作成する。
     * 作成が終わったら、AddVideoToPlayList()に作ったプレイリストと追加したいビデオを渡して追加する。
     */
    private void AddPlaylist(final String title, final String privacyStatus, final YouTubeVideo video) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                YouTube youtubeWithCredential = getYouTubeWithCredentials();

                /*プレイリストを作成*/
                PlaylistSnippet playlistSnippet = new PlaylistSnippet();
                playlistSnippet.setTitle(title);

                PlaylistStatus status = new PlaylistStatus();
                status.setPrivacyStatus(privacyStatus);

                Playlist playlist = new Playlist();
                playlist.setSnippet(playlistSnippet);
                playlist.setStatus(status);


                try {
                    String playlistid = youtubeWithCredential.playlists().insert("snippet,status", playlist).execute().getId();
                    /*作ったプレイリストにvideoを追加*/
                    AddVideoToPlayList(playlistid, video, false, title, privacyStatus);
                } catch (Exception e) {
                    /*プレイリストの新規作成に失敗したとき*/
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), "プレイリスト作成に失敗しました。", Toast.LENGTH_LONG).show();
                        }
                    });

                }
            }
        }).start();

    }


    /**
     * プレイリストにビデオ追加
     * 引数:boolean resultShow:プレイリストの新規作成してそこに追加するときはfalseが渡ってくる。
     */
    private void AddVideoToPlayList(final String playlistId, final YouTubeVideo video, final boolean resultShow, final String title, final String privacyStatus) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                //プレイリストに追加

                /*渡すjsonのresourceidを作成*/
                ResourceId resourceId = new ResourceId();
                resourceId.setVideoId(video.getId());
                resourceId.setKind("youtube#video");

                /*渡すjsonのsnippetを作成*/
                PlaylistItemSnippet snippet = new PlaylistItemSnippet();
                snippet.setPlaylistId(playlistId);
                snippet.setResourceId(resourceId);

                /*実際にinsertするplaylistitemを作成 */
                PlaylistItem playlistItem = new PlaylistItem();
                playlistItem.setSnippet(snippet);

                String toastText = "";
                try {
                    getYouTubeWithCredentials().playlistItems().insert("snippet", playlistItem).execute();
                    if (resultShow) {
                        toastText = " 追加に成功しました。";
                    } else {
                     /*新規作成およびビデオ追加ともに成功した場合*/
                        toastText = privacyStatus + "リスト\n" + title + " を新規作成し\n" + video.getTitle() + " を追加しました。";
                    }
                } catch (Exception e) {
                    Log.d(TAG, "AddPlaylist Error:" + e.getMessage());
                    if (resultShow) {
                        toastText = "追加に失敗しました。";
                    } else {
                        /*プレイリスト新規作成には成功したがビデオ追加に失敗した場合*/
                        toastText = "新規" + privacyStatus + "リスト\n" + title + " の作成には成功しましたがビデオの追加に失敗しました。";
                    }

                }
                final String finalToastText = toastText;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), finalToastText, Toast.LENGTH_LONG).show();
                    }
                });

            }

        }).start();
    }

    /**
     * プレイリスト詳細を見るためのリスナーの中身実装
     *
     * @param playlist
     */
    public void onDetailClick(YouTubePlaylist playlist) {
        Log.d(TAG, "playlist-detail-checked!!!\n\n");
        //ビデオ一覧表示Fragment追加用
        PlaylistDetailFragment playlistDetailFragment = PlaylistDetailFragment.newInstance(playlist.getTitle());
        playlistDetailFragment.setPlaylist(playlist);


        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.replace(R.id.tab_and_viewpager, playlistDetailFragment, playlistDetailFragmentTAG);

        ft.addToBackStack(playlistDetailBackstackTAG);
        ft.commit();
    }

    /**
     * Handles selected item from action bar
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        Log.d(TAG, "onOptionsItemSelected");
        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_about:
                Log.d(TAG, "onOptionsItemSelected:about");
                AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
                alertDialog.setTitle(getString(R.string.myName));
                alertDialog.setIcon(R.drawable.dbwan);

                alertDialog.setMessage(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME + "\n\n" +
                        getString(R.string.email) + "\n\n" +
                        getString(R.string.date) + "\n");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
                return true;

            case R.id.action_clear_list:
                Log.d(TAG, "onOptionsItemSelected:clear");
                new AlertDialog.Builder(getActivity()).setMessage(getString(R.string.ask_delete_all_recetry_list)).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).deleteAll();
                        Fragment fragment = getChildFragmentManager().findFragmentByTag(tabLayoutFragmentTAG);
                        if (fragment instanceof TabLayoutFragment) {
                            ((TabLayoutFragment) fragment).clearRecentlyPlayedList();
                        }
                    }
                }).setNegativeButton("cancel", null).show();
                return true;

            case R.id.log_in:
                Log.d(TAG, "onOptionsItemSelected:login");
                Activity activity = getActivity();
                if (activity instanceof LoginHandler) {
                    ((LoginHandler) activity).checkPermissionAndLoginGoogleAccount();
                }
                return true;

            case R.id.video_quality:
                Log.d(TAG, "onOptionsItemSelected:video quality");
                setVideoQuality();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private void startSearch(SearchView searchView, String s) {
       // searchView.setQuery(s, false);
        searchView.clearFocus();

        searchView.onActionViewCollapsed();
        toolbar.setSubtitle(s);
        searchQuery(s);
    }

    /**
     * Search for query on youTube by using YouTube Data API V3
     *
     * @param query
     */
    private  void searchQuery(final String query) {
        Log.d(TAG, "searchQuery : " + query);
        //check network connectivity
        //When searching, if you are not connected to the network, issue an error.
        //検索するにあたって、ネットワークにつながってなかったらerrorを出す。
        NetworkConf networkConf=new NetworkConf(getActivity());
        if (!networkConf.isNetworkAvailable()) {
            networkConf.createNetErrorDialog();
            return;
        }
        //loadingProgressBar.setVisibility(View.VISIBLE);

        getLoaderManager().restartLoader(Config.YouTubeVideosLoaderId, null, new LoaderManager.LoaderCallbacks<List<YouTubeVideo>>() {
            @Override
            public Loader<List<YouTubeVideo>> onCreateLoader(final int id, final Bundle args) {
                return new YouTubeVideosLoader(getActivity(), query);
            }

            @Override
            public void onLoadFinished(Loader<List<YouTubeVideo>> loader, List<YouTubeVideo> data) {
                Log.d(TAG, "onLoadFinished");
                if (data == null)
                    return;
                Log.d(TAG, "onLoadFinished : data != null");
                PlaylistsCash.Instance.setSearchResultsList(data);
                handleSearchData(data);
            }

            @Override
            public void onLoaderReset(Loader<List<YouTubeVideo>> loader) {
                List<YouTubeVideo> newList=new ArrayList<>();
                PlaylistsCash.Instance.setSearchResultsList(newList);
                handleSearchData(newList);
            }
        }).forceLoad();
    }
    private void handleSearchData(List<YouTubeVideo> data) {
        Log.d(TAG, "handleSearchData");
        FragmentManager fragmentManager = getChildFragmentManager();
        if(fragmentManager.getBackStackEntryCount()!=0) {
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        Fragment fragment=fragmentManager.findFragmentByTag(tabLayoutFragmentTAG);
        if(fragment instanceof TabLayoutFragment){
            ((TabLayoutFragment) fragment).handleSearch(data);
        }

    }


    @Override
    public void surfaceCreated(SurfaceHolder paramSurfaceHolder) {
        Log.d(TAG, "surfaceCreated");
    }


    @Override
    public void surfaceChanged(SurfaceHolder paramSurfaceHolder, int paramInt1,
                               int paramInt2, int paramInt3) {
        Log.d(TAG, "surfaceChanged");
        Activity activity = getActivity();
        if (activity instanceof SurfaceHolderListener) {
            ((SurfaceHolderListener) activity).changeSurfaceHolder(surfaceView.getHolder(), surfaceView);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder paramSurfaceHolder) {
        Log.d(TAG, "surfaceDestroyed");
        Activity activity = getActivity();
        if (activity instanceof SurfaceHolderListener) {
            ((SurfaceHolderListener) activity).releaseSurfaceHolder(paramSurfaceHolder);
        }
    }

    /**
     * チェックボックスの画像を設定に合わせるメゾッド
     */
    private void checkBoxUpdata() {
        Settings settings = Settings.getInstance();
        //一曲リピートか否かに合わせてチェックボックス画面変更
        boolean repeatOne = false;
        switch (settings.getRepeatOne()) {
            case ON:
                repeatOne = true;
                break;
            case OFF:
                repeatOne = false;
        }
        repeatOneBox.setChecked(repeatOne);
        //プレイリストリピートか否かに合わせてチェックボックス画面変更
        boolean repeatPlaylist = false;
        switch (settings.getRepeatPlaylist()) {
            case ON:
                repeatPlaylist = true;
                break;
            case OFF:
                repeatPlaylist = false;
        }
        repeatPlaylistBox.setChecked(repeatPlaylist);
    }

    /**
     * ダイアログを出し、ユーザーにビデオ再生の画質を決めさせるメゾッド
     */
    private void setVideoQuality() {
        final Activity activity = getActivity();
        final AlertDialog.Builder mListDlg = new AlertDialog.Builder(activity);
        final AlertDialog.Builder mDlg = new AlertDialog.Builder(activity);

        final SharedPreferences sharedPreferences = getActivity().getSharedPreferences(VideoQualitys.VideoQualityPreferenceFileName, Context.MODE_PRIVATE);
        int nowQuality = sharedPreferences.getInt(VideoQualitys.VideoQualityPreferenceKey, VideoQualitys.videoQualityNormal);

        //チェックされたやつの番号を入れておく。
        final ArrayList<Integer> checkedItems = new ArrayList<Integer>();

        //デフォルトは標準画質
        checkedItems.add(nowQuality);

        //ダイアログ表示のための準備
        mListDlg.setTitle("再生画質設定");
        mListDlg.setSingleChoiceItems(VideoQualitys.getVideoQualityChoices(), nowQuality, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkedItems.clear();
                checkedItems.add(which);
            }
        });
        mListDlg.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!checkedItems.isEmpty()) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt(VideoQualitys.VideoQualityPreferenceKey, checkedItems.get(0));
                    boolean result = editor.commit();
                    if (result) {
                        mDlg.setMessage(getString(R.string.video_quality_setting_success)).setPositiveButton("OK", null).show();
                    } else {
                        mDlg.setMessage(getString(R.string.video_quality_setting_fail)).setPositiveButton("OK", null).show();
                    }

                }
            }
        });

        mListDlg.setNegativeButton("キャンセル", null);

        mListDlg.create().show();
    }

    /**
     * 今再生中のプレイリストの表示のフラグメントを出すかしまうかのハンドリングメゾッド
     *
     * @param isChecked
     */
    private void nowPlayingListBoxHandler(boolean isChecked) {
        Log.d(TAG, "nowPlayingListBoxHandler : " + String.valueOf(isChecked));
        FragmentManager fragmentManager = getChildFragmentManager();

        if (isChecked) {
            //今再生中のビデオ一覧のためのフラグメントを作成
            NowPlayingListFragment nowPlayingListFragment = new NowPlayingListFragment();
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.replace(R.id.tab_and_viewpager, nowPlayingListFragment, nowPlayingListFragmentTAG);
            //このままだと下のviewpageが見えていて且つタッチできてしまうので対策
            //playlistdetailのdestroyで、可視化＆タッチ有効化
            //viewPager.setVisibility(View.INVISIBLE);
            /*viewPager.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });*/
            //tabも見えるし触れちゃうのでoffにする。
            // playlistTitleFragmentのdestroyで可視化＆タッチ有効化
            //tabLayout.setVisibility(View.INVISIBLE);
            /*tabLayout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });*/
            ft.addToBackStack(nowplayingFragmentBackstackTAG);
            ft.commit();

        } else {
            Fragment fragment = fragmentManager.findFragmentByTag(nowPlayingListFragmentTAG);
            Log.d(TAG, "nowPlayingListFragmentTAG : " + String.valueOf(fragment != null));
            if (fragment != null) {
                //FragmentTransaction ft = fragmentManager.beginTransaction();
                //ft.remove(fragment);
                fragmentManager.popBackStack();
                //ft.commit();
            }
        }
    }

    /**
     * Class which provides adapter for fragment pager
     */
    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        private ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        private void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }

    }


}
