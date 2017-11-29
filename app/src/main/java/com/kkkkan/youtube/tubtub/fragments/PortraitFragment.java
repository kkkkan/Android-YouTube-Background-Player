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


import android.Manifest;
import android.app.Activity;
import android.app.SearchManager;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
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
import com.kkkkan.youtube.tubtub.Settings;
import com.kkkkan.youtube.tubtub.adapters.PlaylistsAdapter;
import com.kkkkan.youtube.tubtub.database.YouTubeSqlDb;
import com.kkkkan.youtube.tubtub.interfaces.OnFavoritesSelected;
import com.kkkkan.youtube.tubtub.interfaces.SurfaceHolderListener;
import com.kkkkan.youtube.tubtub.interfaces.TitlebarListener;
import com.kkkkan.youtube.tubtub.interfaces.ViewPagerListener;
import com.kkkkan.youtube.tubtub.model.YouTubePlaylist;
import com.kkkkan.youtube.tubtub.model.YouTubeVideo;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

import static com.kkkkan.youtube.R.layout.suggestions;
import static com.kkkkan.youtube.tubtub.youtube.YouTubeSingleton.getCredential;
import static com.kkkkan.youtube.tubtub.youtube.YouTubeSingleton.getYouTubeWithCredentials;

/**
 * A simple {@link Fragment} subclass.
 */
public class PortraitFragment extends Fragment implements OnFavoritesSelected, PlaylistsAdapter.OnDetailClickListener, SurfaceHolder.Callback, ViewPagerListener {
    final private static String TAG = "PortraitFragment";
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private TitlebarListener titlebarListener;
    private SearchFragment searchFragment;
    private RecentlyWatchedFragment recentlyPlayedFragment;
    private FavoritesFragment favoritesFragment;
    private CheckBox repeatOneBox;
    private CheckBox repeatPlaylistBox;
    private SurfaceView surfaceView;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    private static final int PERMISSIONS = 1;
    private Handler handler = new Handler();

    //For movie title
    //動画タイトル用
    private TextView titleView;
    private MainActivityViewModel viewModel;


    private int[] tabIcons = {
            R.drawable.ic_action_heart,
            R.drawable.ic_recently_wached,
            R.drawable.ic_search,
            R.drawable.ic_action_playlist
    };


    /**
     * When making a new instance of PortraitFragment make sure to make with this mezzo
     * <p>
     * PortraitFragmentの新しいインスタンスを作るときは必ず
     * このメゾッドで作ること
     * <p>
     * MainActivity#onCreate()の中で必ず呼んでいるので再生成時もリスナーがnullになることはなし
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_portrait, container, false);
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


        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                return onOptionsItemSelected(menuItem);
            }
        });
        //setSupportActionBar(toolbar);

        //Do not turn back to action bar
        //アクションバーに戻るボタンをつけない
        //getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        //Set the fragment held by viewPage to 3
        //viewPageで保持するfragmentを三枚に設定
        viewPager = (ViewPager) view.findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(3);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) view.findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        setupTabIcons();

        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkBoxUpdata();
    }

    /**
     * お気に入りfragmentにビデオを追加したり削除したり
     */
    @Override
    public void onFavoritesSelected(YouTubeVideo video, boolean isChecked) {
        if (isChecked) {
            favoritesFragment.addToFavoritesList(video);
        } else {
            favoritesFragment.removeFromFavorites(video);
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

    /*プレイリスト詳細を見るためのリスナーの中身実装*/
    public void onDetailClick(YouTubePlaylist playlist) {
        Log.d(TAG, "playlist-detail-checked!!!\n\n");
    /*ビデオ一覧表示Fragment追加用*/
        PlaylistDetailFragment playlistDetailFragment = PlaylistDetailFragment.newInstance();
        playlistDetailFragment.setPlaylist(playlist);

    /*プレイリストタイトル表示Fragment用*/
        PlaylistTitleFragment playlistTitleFragment = new PlaylistTitleFragment();
        playlistTitleFragment.setPlaylistTitle(playlist.getTitle());

        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.add(R.id.frame_layout, playlistDetailFragment);
        ft.add(R.id.frame_layout_tab, playlistTitleFragment);
       /*このままだと下のviewpageが見えていて且つタッチできてしまうので対策*
       playlistdetailのdestroyで、可視化＆タッチ有効化
        */
        viewPager.setVisibility(View.INVISIBLE);
        viewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
    /*tabも見えるし触れちゃうのでoffにする。
    * playlistTitleFragmentのdestroyで可視化＆タッチ有効化*/
        tabLayout.setVisibility(View.INVISIBLE);
        tabLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
    /*複数addしてもcommit()呼び出しまでを一つとしてスタックに入れてくれる。*/
        ft.addToBackStack(null);
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {

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
        } else if (id == R.id.action_clear_list) {
            YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).deleteAll();
            recentlyPlayedFragment.clearRecentlyPlayedList();
            return true;
        } else if (id == R.id.action_search) {
            MenuItemCompat.expandActionView(item);
            return true;
        } else if (id == R.id.log_in) {
            String[] perms = {Manifest.permission.GET_ACCOUNTS, Manifest.permission.READ_PHONE_STATE};
            if (!EasyPermissions.hasPermissions(getActivity(), perms)) {
                EasyPermissions.requestPermissions(this, getString(R.string.permissions_request),
                        PERMISSIONS, perms);
            } else {
                startActivityForResult(
                        getCredential().newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Options menu in action bar
     *
     * @param menu
     * @return
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        /*メニューバー追加*/
        inflater.inflate(R.menu.menu_main, menu);

        /*探すボタンで検索できるよう機能の実装*/
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        }

        /*第一引数：context,第二引数:レイアウトid、三：Cursur,四：カラム名（配列）、五：viewid,六：flag*/
        //suggestions
        final CursorAdapter suggestionAdapter = new SimpleCursorAdapter(getActivity(),
                suggestions,
                null,
                new String[]{SearchManager.SUGGEST_COLUMN_TEXT_1},
                new int[]{android.R.id.text1},
                0);
        final List<String> suggestions = new ArrayList<>();

        /*setSuggestionsAdaper:独自のadapterをセットする*/
        searchView.setSuggestionsAdapter(suggestionAdapter);


        /*リスナー設定クリックされたら*/
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            /*suggestionをスクロールしたとき？*/
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            /*スクロールを選んだ時検索*/
            @Override
            public boolean onSuggestionClick(int position) {
                searchView.setQuery(suggestions.get(position), false);
                searchView.clearFocus();

                Intent suggestionIntent = new Intent(Intent.ACTION_SEARCH);
                suggestionIntent.putExtra(SearchManager.QUERY, suggestions.get(position));
                handleIntent(suggestionIntent);

                return true;
            }
        });
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEARCH.equals(action)) {
            //検索の時
            String query = intent.getStringExtra(SearchManager.QUERY);
            //スムーズスクロールありでfragmenを2に変更
            viewPager.setCurrentItem(2, true); //switch to search fragment

            if (searchFragment != null) {
                searchFragment.searchQuery(query);
            }
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

    @Override
    public ViewPager getViewPager() {
        return viewPager;
    }

    @Override
    public TabLayout getTabLayout() {
        return tabLayout;
    }


    /**
     * Setups icons for 3 tabs
     */
    private void setupTabIcons() {
        tabLayout.getTabAt(0).setIcon(tabIcons[0]);
        tabLayout.getTabAt(1).setIcon(tabIcons[1]);
        tabLayout.getTabAt(2).setIcon(tabIcons[2]);
        tabLayout.getTabAt(3).setIcon(tabIcons[3]);
    }

    /**
     * Setups viewPager for switching between pages according to the selected tab
     *
     * @param viewPager
     */
    private void setupViewPager(ViewPager viewPager) {

        ViewPagerAdapter adapter = new ViewPagerAdapter(getChildFragmentManager());

        searchFragment = SearchFragment.newInstance();
        recentlyPlayedFragment = RecentlyWatchedFragment.newInstance();
        favoritesFragment = FavoritesFragment.newInstance();
        PlaylistsFragment playlistsFragment = PlaylistsFragment.newInstance();

        adapter.addFragment(favoritesFragment, null);//0
        adapter.addFragment(recentlyPlayedFragment, null);//1
        adapter.addFragment(searchFragment, null);//2
        adapter.addFragment(playlistsFragment, null);//3

        viewPager.setAdapter(adapter);
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
