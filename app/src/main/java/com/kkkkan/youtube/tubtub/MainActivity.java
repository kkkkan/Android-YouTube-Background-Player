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
package com.kkkkan.youtube.tubtub;

import android.Manifest;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.MatrixCursor;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.PlaylistSnippet;
import com.google.api.services.youtube.model.PlaylistStatus;
import com.google.api.services.youtube.model.ResourceId;
import com.kkkkan.youtube.BuildConfig;
import com.kkkkan.youtube.R;
import com.kkkkan.youtube.tubtub.adapters.PlaylistsAdapter;
import com.kkkkan.youtube.tubtub.database.YouTubeSqlDb;
import com.kkkkan.youtube.tubtub.fragments.FavoritesFragment;
import com.kkkkan.youtube.tubtub.fragments.LandscapeFragment;
import com.kkkkan.youtube.tubtub.fragments.PlaylistDetailFragment;
import com.kkkkan.youtube.tubtub.fragments.PlaylistTitleFragment;
import com.kkkkan.youtube.tubtub.fragments.PlaylistsFragment;
import com.kkkkan.youtube.tubtub.fragments.RecentlyWatchedFragment;
import com.kkkkan.youtube.tubtub.fragments.SearchFragment;
import com.kkkkan.youtube.tubtub.interfaces.OnFavoritesSelected;
import com.kkkkan.youtube.tubtub.interfaces.OnItemSelected;
import com.kkkkan.youtube.tubtub.interfaces.TitlebarListener;
import com.kkkkan.youtube.tubtub.model.YouTubePlaylist;
import com.kkkkan.youtube.tubtub.model.YouTubeVideo;
import com.kkkkan.youtube.tubtub.utils.Config;
import com.kkkkan.youtube.tubtub.utils.NetworkConf;
import com.kkkkan.youtube.tubtub.youtube.SuggestionsLoader;
import com.kkkkan.youtube.tubtub.youtube.YouTubeShareVideoGetLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static com.kkkkan.youtube.R.layout.suggestions;
import static com.kkkkan.youtube.tubtub.youtube.YouTubeSingleton.getCredential;
import static com.kkkkan.youtube.tubtub.youtube.YouTubeSingleton.getYouTubeWithCredentials;

/**
 * Activity that manages fragments and action bar
 */
public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,
        OnItemSelected, OnFavoritesSelected, SurfaceHolder.Callback, PlaylistsAdapter.OnDetailClickListener, TitlebarListener, MediaController.MediaPlayerControl {
    public static Handler mainHandler = new Handler();
    static private MediaPlayerService service;

    public Context getMainContext() {
        return mainContext;
    }

    private final Context mainContext = this;

    private static final String TAG = "MainActivity";
    private static final String LandscapeFragmentTAG = "LandscapeFragmentTAG";
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    private static final int PERMISSIONS = 1;
    public static final String PREF_ACCOUNT_NAME = "accountName";


    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    static final int REQUEST_CODE_TOKEN_AUTH = 1006;

    private SearchFragment searchFragment;
    private RecentlyWatchedFragment recentlyPlayedFragment;
    private FavoritesFragment favoritesFragment;


    //private String videoUrl;
    //private SurfaceHolder mHolder;
    private SurfaceView mPreview;
    private MediaController mMediaController;
    private AlertDialog.Builder mListDlg;
    private AlertDialog.Builder mTitleDlg;
    private ProgressDialog mProgressDialog;
    private CheckBox repeatOneBox;
    private CheckBox repeatPlaylistBox;

    //For movie title
    //動画タイトル用
    private TextView mTextView;


    private int[] tabIcons = {
            R.drawable.ic_action_heart,
            R.drawable.ic_recently_wached,
            R.drawable.ic_search,
            R.drawable.ic_action_playlist
    };

    private NetworkConf networkConf;

    private MainActivityViewModel viewModel;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MainActivity.service = ((MediaPlayerService.MediaPlayerBinder) service).getService(viewModel);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            unbindService(this);
            MainActivity.service = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel.class);
        viewModel.getLoadingState().observe(this, new Observer<MainActivityViewModel.LoadingState>() {
            @Override
            public void onChanged(@Nullable MainActivityViewModel.LoadingState loadingState) {
                if (loadingState == null) {
                    return;
                }
                switch (loadingState) {
                    case StartLoading:
                        setProgressDialogShow();
                        break;
                    case StopLoading:
                        setProgressDialogDismiss();
                        break;
                    case Error:
                        Toast.makeText(mainContext, "ビデオが読み込めませんでした。次のビデオを再生します。", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
        viewModel.getVideoTitle().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                mTextView.setText(s);
            }
        });


        startService(new Intent(this, MediaPlayerService.class));
        //serviceをmediaControllerにセットするので必ずそこより先にbindServiceすること！！
        bindService(new Intent(this, MediaPlayerService.class), connection, BIND_AUTO_CREATE);


        //Turn off screen saver
        //スクリーンセーバをオフにする
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFormat(PixelFormat.TRANSPARENT);

        //Setting for one repeat check box on the title bar
        //タイトルバーの1リピートチェックボックスについての設定
        repeatOneBox = (CheckBox) findViewById(R.id.repeat_one_box);
        repeatOneBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                repeatOneCheckListener();
                checkBoxUpdata();
            }
        });
        //Settings for the playlist repeat checkbox on the title bar
        //タイトルバーのプレイリストリピートチェックボックスについての設定
        repeatPlaylistBox = (CheckBox) findViewById(R.id.repeat_playlist_box);
        repeatPlaylistBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                repeatPlaylistCheckListener();
                checkBoxUpdata();
            }
        });


        mTextView = (TextView) findViewById(R.id.title_view);
        mListDlg = new AlertDialog.Builder(this);
        mTitleDlg = new AlertDialog.Builder(this);
        mProgressDialog = new ProgressDialog(this);


        YouTubeSqlDb.getInstance().init(this);


        //MediaController settings
        // MediaControllerの設定
        mMediaController = new MediaController(this);
        mMediaController.setMediaPlayer(this);
        //Back button · Forward button
        //戻るボタン・進むボタン
        mMediaController.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //next button clicked
                //I made it possible to go around
                //一周できるようにした
                service.nextPlay();
                Log.d(TAG, " mMediaController.setPrevNextListeners");


            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //previous button clicked
                //I made it possible to go around
                //一周できるようにした
                service.prevPlay();
                Log.d(TAG, " mMediaController.setPrevNextListeners");
            }
        });

        //When you start from landscape screen mPreview etc is not made with onCreate (),
        // it drops in the process of obscuring mPreview
        //横画面から始めた時にmPreview等をonCreate()で作っておかないと
        // mPreviewを見え無くしたりする処理のところで落ちる
        makeSurfaceViewAndMediaControllerSetting();


        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Do not turn back to action bar
        //アクションバーに戻るボタンをつけない
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        //Set the fragment held by viewPage to 3
        //viewPageで保持するfragmentを三枚に設定
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(3);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        setupTabIcons();


        networkConf = new NetworkConf(this);
        Configuration config = getResources().getConfiguration();
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //At the time of horizontal screen
            //横画面の時
            viewChangeWhenLandscape();
        }

        requestPermissions();

        //When opened by sharing from another application
        //他のアプリからの共有で開かれた場合
        if (savedInstanceState == null) {
            //It is not time to regenerate
            //再生成時ではない
            Intent getIntent = getIntent();
            String action = getIntent.getAction();
            String type = getIntent.getType();
            Log.d(TAG, "Intent:\n" + action + "\n" + type);
            if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
                //When opened from sharing by another application
                //他のアプリによる共有から開かれたとき
                shareHandle(getIntent);
            }
        }


    }


    public void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

    }

    @SuppressLint("NewApi")
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        //ここで改めてmPreviewを作り直さないと、バック画面→ロック→アプリに戻ると落ちる
        makeSurfaceViewAndMediaControllerSetting();

        // allow to continue playing media in the background.
        // バックグラウンド再生を許可する
        requestVisibleBehind(true);

        //チェックボックスの画像を設定に合わせる
        checkBoxUpdata();

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }


    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //SingletonMediaPlayer.instance.getMediaPlayer().release();
        //mNotificationManagerCompat.cancel(notificationId);
        //サービスへのbind切る
        service.unbindService(connection);
    }

    /**
     * mPreviewを作り直す
     * それに伴いMediaplayerの投影先とmHolderも作り直し
     */
    private void makeSurfaceViewAndMediaControllerSetting() {
        mPreview = (SurfaceView) findViewById(R.id.surface);
        if (mPreview == null) {
            //通常ありえない
            Log.d("TAG", "SurfaceView is null!");
        } else {
            //Change the vertical and horizontal lengths of the playback space of the movie according to the screen size of the smartphone
            //スマホの画面サイズに合わせて動画の再生スペースの縦横の長さを変化させる
            mPreview.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    resizeSurfaceView();
                }
            });
        }
        mPreview.getHolder().addCallback(this);

         /*mMediaController表示のためのtouchlistener*/
        mPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN && v instanceof SurfaceView) {
                    if (!mMediaController.isShowing()) {
                        mMediaController.show();
                    } else {
                        mMediaController.hide();
                    }
                }
                return true;
            }
        });

        //mediaControllerの画面に新しく作ったsurfaceviewをセット
        mMediaController.setAnchorView(mPreview);
    }

    @Override
    public void surfaceCreated(SurfaceHolder paramSurfaceHolder) {
        Log.d(TAG, "surfaceCreated");
    }


    @Override
    public void surfaceChanged(SurfaceHolder paramSurfaceHolder, int paramInt1,
                               int paramInt2, int paramInt3) {
        Log.d(TAG, "surfaceChanged");
        Configuration config = getResources().getConfiguration();
        if(service!=null) {
            service.setDisplay(mPreview.getHolder());
        }
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //横画面だったらどちらにしろ投影先はLandscapeFragment上のsurfaceなので
            // mediaplayerの設定変更系はしない
            return;
        }
        //MediaPlayer、MediaControllerの投影先を変更
        changeSurfaceHolderAndTitlebar(mPreview.getHolder(), mPreview, mTextView);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder paramSurfaceHolder) {
        Log.d(TAG, "surfaceDestroyed");
        try {
            changeSurfaceHolderAndTitlebar(null, null, mTextView);
        } catch (IllegalStateException e) {
            //バックボタンでアプリを落としたとき用
            //このままonDestroy()まで呼ばれるのでここはスルー
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(PERMISSIONS)
    private void requestPermissions() {
        String[] perms = {Manifest.permission.GET_ACCOUNTS, Manifest.permission.READ_PHONE_STATE};
        /*permissionがあれば*/
        if (EasyPermissions.hasPermissions(this, perms)) {
            // Already have permission, do the thing
            /*ビルド環境が低かったりするとスルーされてきてしまうのでここでもう一度チェック?*/
            if (EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS)) {
                /*自アプリのみ書き込み可能で開いてアカウントネームを拾ってくる*/
                String accountName = getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
                /*アカウントひとつだけあったら*/
                if (accountName != null) {
                    /*アカウントをセット*/
                    getCredential().setSelectedAccountName(accountName);
                } else {
                    /*無ければコールバックありでアカウントを選ばせる*/
                    // Start a dialog from which the user can choose an account
                    startActivityForResult(
                            getCredential().newChooseAccountIntent(),
                            REQUEST_ACCOUNT_PICKER);
                }
            } else {
                // Request the GET_ACCOUNTS permission via a user dialog
                EasyPermissions.requestPermissions(
                        this,
                        /*"This app needs to access your Google account (via Contacts)."*/"このアプリでYouTubeアカウントと連携するためにはグーグルアカウントが必要です。",
                        REQUEST_PERMISSION_GET_ACCOUNTS,
                        Manifest.permission.GET_ACCOUNTS);
            }
        } else {
            final String[] finalperms = perms;
            new AlertDialog.Builder(this).setCancelable(false).setMessage(getString(R.string.all_permissions_request)).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Do not have permissions, request them now
                    EasyPermissions.requestPermissions(MainActivity.this, getString(R.string.all_permissions_request),
                            PERMISSIONS, finalperms);
                }
            }).show();

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ACCOUNT_PICKER) {
            if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                if (accountName != null) {
                    Log.d(TAG, "onActivityResult account");
                    /*SharedPreference:アプリの設定データをデバイス内に保存するための仕組み。*/
                    SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(PREF_ACCOUNT_NAME, accountName);
                    editor.apply();
                    getCredential().setSelectedAccountName(accountName);

                    //ログインする
                    AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
                        @Override
                        protected String doInBackground(Void... params) {
                            String token = null;
                            try {
                                /*youtube:YouTube アカウントの管理
                                * youtube.readonly:YouTube アカウントの表示
                                * youtube.upload:YouTube の動画のアップロード、YouTube の動画の管理
                                * youtubepartner-channel-audio:channel リソース内の auditDetails のリトリーブ*/
                                token = GoogleAuthUtil.getToken(
                                        MainActivity.this,
                                        getCredential().getSelectedAccountName(),
                                        "oauth2:" + " " + "https://www.googleapis.com/auth/youtube" + " " + "https://www.googleapis.com/auth/youtube.readonly" + " " + "https://www.googleapis.com/auth/youtube.upload" + " " + "https://www.googleapis.com/auth/youtubepartner-channel-audit");
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(mainContext, "ログイン成功:\n" + getCredential().getSelectedAccountName(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            } catch (IOException transientEx) {
                                // Network or server error, try later
                                Log.e(TAG, transientEx.toString());
                            } catch (UserRecoverableAuthException e) {
                                // Recover (with e.getIntent())
                                Log.e(TAG, e.toString());
                                Intent recover = e.getIntent();
                                startActivityForResult(recover, REQUEST_CODE_TOKEN_AUTH);
                            } catch (GoogleAuthException authEx) {
                                // The call is not ever expected to succeed
                                // assuming you have already verified that
                                // Google Play services is installed.
                                Log.e(TAG, getCredential().getSelectedAccountName() + ":" + authEx.toString());
                            }
                            return token;
                        }

                        @Override
                        protected void onPostExecute(String token) {
                            /*tokenの文字列の表示*/
                            Log.i(TAG, "Access token retrieved:" + token);
                        }

                    };
                    task.execute();
                }
            }
        } else if (requestCode == REQUEST_CODE_TOKEN_AUTH) {
            startActivityForResult(
                    getCredential().newChooseAccountIntent(),
                    REQUEST_ACCOUNT_PICKER);
        }
    }

    /*許可ダイアログの結果を受ける*/
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * Override super.onNewIntent() so that calls to getIntent() will return the
     * latest intent that was used to start this Activity rather than the first
     * intent.
     */
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent");
        setIntent(intent);

        handleIntent(intent);
    }

    /**
     * Handle search intent and queries YouTube for videos
     *
     * @param intent
     */
    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();
        Log.d(TAG, "Intent:\n" + action + "\n" + type);
        if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
            //When opened from sharing by another application
            //他のアプリによる共有から開かれたとき
            shareHandle(intent);
        } else if (Intent.ACTION_SEARCH.equals(action)) {
            //検索の時
            String query = intent.getStringExtra(SearchManager.QUERY);
            //スムーズスクロールありでfragmenを2に変更
            viewPager.setCurrentItem(2, true); //switch to search fragment

            if (searchFragment != null) {
                searchFragment.searchQuery(query);
            }
        }
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

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        searchFragment = SearchFragment.newInstance();
        recentlyPlayedFragment = RecentlyWatchedFragment.newInstance();
        favoritesFragment = FavoritesFragment.newInstance();
        PlaylistsFragment playlistsFragment = PlaylistsFragment.newInstance();

        adapter.addFragment(favoritesFragment, null);/*0*/
        adapter.addFragment(recentlyPlayedFragment, null);/*1*/
        adapter.addFragment(searchFragment, null);/*2*/
        adapter.addFragment(playlistsFragment, null);/*3*/

        viewPager.setAdapter(adapter);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:");
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied: ");
    }


    /**
     * 選んだビデオをmediaplayerで再生するためのurlをvideoUrlにセットし、バックグラウンド再生かフォアグランド再生かによって
     * 次の制御をvideoCreate()に振る
     */
    @Override
    public void onPlaylistSelected(List<YouTubeVideo> playlist, final int position) {
        service.onPlaylistSelected(playlist, position);
    }


    /**
     * progressDialog表示
     */
    public void setProgressDialogShow() {
        /*進捗状況は表示しない*/
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage("Loading...");
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                service.setPlaylistSelectedCancelFlag(true);
            }
        });
        mProgressDialog.show();

    }

    public void setProgressDialogDismiss() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
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
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mainContext, "プレイリスト作成に失敗しました。", Toast.LENGTH_LONG).show();
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
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mainContext, finalToastText, Toast.LENGTH_LONG).show();
                    }
                });

            }

        }).start();
    }

    /*プレイリストに追加したり、プレイリストを新規作成したうえで追加したり*/
    @Override
    public void onAddSelected(final YouTubeVideo video) {
        Log.d(TAG, "onAddSelected");
        /*video タイトル取得*/
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
                        final EditText titleEdit = new EditText(mainContext);
                        mTitleDlg.setTitle("新規プレイリスト名入力");
                        mTitleDlg.setView(titleEdit);
                        mTitleDlg.setPositiveButton("非公開で作成", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final String title = titleEdit.getText().toString();
                                if (title.length() == 0) {
                                    Toast.makeText(mainContext, "プレイリスト名は空白は認められません。", Toast.LENGTH_LONG).show();
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
                                    Toast.makeText(mainContext, "プレイリスト名は空白は認められません。", Toast.LENGTH_LONG).show();
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

    /*プレイリスト詳細を見るためのリスナーの中身実装*/
    public void onDetailClick(YouTubePlaylist playlist) {
        Log.d(TAG, "playlist-detail-checked!!!\n\n");
    /*ビデオ一覧表示Fragment追加用*/
        PlaylistDetailFragment playlistDetailFragment = PlaylistDetailFragment.newInstance();
        playlistDetailFragment.setPlaylist(playlist);

    /*プレイリストタイトル表示Fragment用*/
        PlaylistTitleFragment playlistTitleFragment = new PlaylistTitleFragment();
        playlistTitleFragment.setPlaylistTitle(playlist.getTitle());

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
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


    /**
     * Options menu in action bar
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        /*メニューバー追加*/
        getMenuInflater().inflate(R.menu.menu_main, menu);

        /*探すボタンで検索できるよう機能の実装*/
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }

        /*第一引数：context,第二引数:レイアウトid、三：Cursur,四：カラム名（配列）、五：viewid,六：flag*/
        //suggestions
        final CursorAdapter suggestionAdapter = new SimpleCursorAdapter(this,
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

        /*検索窓の文字が変わるたび呼び出される？*/
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false; //if true, no new intent is started
            }

            /*検索ワードが変わったら*/
            @Override
            public boolean onQueryTextChange(final String query) {
                // check network connection. If not available, do not query.
                // this also disables onSuggestionClick triggering
                /*二文字以上入力されてたら*/
                if (query.length() > 2) { //make suggestions after 3rd letter
                    if (networkConf.isNetworkAvailable()) {

                        getSupportLoaderManager().restartLoader(Config.SuggestionsLoaderId, null, new LoaderManager.LoaderCallbacks<List<String>>() {
                            @Override
                            public Loader<List<String>> onCreateLoader(final int id, final Bundle args) {
                                return new SuggestionsLoader(getApplicationContext(), query);
                            }

                            @Override
                            public void onLoadFinished(Loader<List<String>> loader, List<String> data) {
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
                        return true;
                    }
                }
                return false;
            }
        });

        return true;
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

            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
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
            if (!EasyPermissions.hasPermissions(this, perms)) {
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


    public ViewPager getViewPager() {
        return viewPager;
    }


    public TabLayout getTabLayout() {
        return tabLayout;
    }

    /**
     * 今のmediaplayerの移している先を返す
     *
     * @return
     */
    public SurfaceHolder getmHolder() {
        return service.getmHolder();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged");
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //横画面になったら
            viewChangeWhenLandscape();
        } else {
            //縦画面になったら
            viewChangeWhenPortrait();
        }

    }

    /**
     * UI上のビデオの描写先とタイトルバーが変更された時に
     * それを反映させるために呼ばれる関数
     */
    public void changeSurfaceHolderAndTitlebar(SurfaceHolder holder, SurfaceView surfaceView, TextView textView) {
        boolean setDisplaySuccess = false;
        if (service != null) {
            setDisplaySuccess = service.setDisplay(holder);
        }
        if (!setDisplaySuccess) {
            Log.d(TAG, "!setDisplaySuccess");
            return;
        }

        mTextView = textView;
        //縦画面の時はsurfaceViewを取得しなおすたびにmMediaController.setAnchorView()しているのでここでセットする必要はないが
        // 横画面の時はfragment内ではmMediaController.setAnchorView()が出来ないのでそれ用
        mMediaController.setAnchorView(surfaceView);
        //mMediaController表示のためのtouchlistener
        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN && v instanceof SurfaceView) {
                    if (!mMediaController.isShowing()) {
                        mMediaController.show();
                    } else {
                        mMediaController.hide();
                    }
                }
                return true;
            }
        });

        //ビデオ途中で縦横変わるときだけはviewmodelだけじゃちゃんと移行先のタイトルバーに題名つけるのできなかったのでとりあえずこちらから取りに行く
        //きれいじゃないのでいつか直したい
        mTextView.setText(service.getVideoTitle());
    }

    /**
     * 横画面時の処理
     */
    private void viewChangeWhenLandscape() {
        Log.d(TAG, " viewChangeWhenLandscape() ");
        //このままだと下のviewpageが見えていて且つタッチできてしまうので対策*
        viewPager.setVisibility(View.INVISIBLE);
        viewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        //tabも見えるし触れちゃうのでoffにする。
        tabLayout.setVisibility(View.INVISIBLE);
        tabLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        //アクションバーも
        toolbar.setVisibility(View.INVISIBLE);
        toolbar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        //動画再生画面も
        mPreview.setVisibility(View.INVISIBLE);
        mPreview.setEnabled(false);
        //タイトルバーも
        mTextView.setVisibility(View.INVISIBLE);

        //フラグメント追加
        Fragment fragment = LandscapeFragment.getNewLandscapeFragment(this);
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.parent_layout, fragment, LandscapeFragmentTAG);
        transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }


    /**
     * 縦画面になったときの処理
     */
    private void viewChangeWhenPortrait() {
        Log.d(TAG, "viewChangeWhenPortrait()");
        //フラグメント削除
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(LandscapeFragmentTAG);
        //最初から縦の時はnull
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(fragment)
                    //onPause()やonStop()直後に呼ばれるonSaveInstanceState()ではFragmentの状態も含めた画面情報を保存するため、それ以降のタイミングでFragmentを操作するとおかしくなる
                    //しかし画面状態が失われてもいいからコミットしたいのでcommitAllowingStateLoss()
                    .commitAllowingStateLoss();
        }
        //見え無くしたり触れなくしたものを直す
        viewPager.setVisibility(View.VISIBLE);
        viewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        //tabも見えるし触れちゃうのでoffにする。
        tabLayout.setVisibility(View.VISIBLE);
        tabLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        //アクションバーも
        toolbar.setVisibility(View.VISIBLE);
        toolbar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        //mPrevieの取得しなおし
        mPreview = (SurfaceView) findViewById(R.id.surface);
        mPreview.setVisibility(View.VISIBLE);
        mPreview.setEnabled(true);
        //Change the vertical and horizontal lengths of the playback space of the movie according to the screen size of the smartphone
        //スマホの画面サイズに合わせて動画の再生スペースの縦横の長さを変化させる
        mPreview.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                resizeSurfaceView();
            }
        });


        mTextView = (TextView) findViewById(R.id.title_view);
        mTextView.setVisibility(View.VISIBLE);

        //設定とcheckboxの表示合わせる
        checkBoxUpdata();

        changeSurfaceHolderAndTitlebar(mPreview.getHolder(), mPreview, mTextView);
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


    @Override
    public void repeatOneCheckListener() {
        Log.d(TAG, "repeatOneCheckListener()");
        Settings settings = Settings.getInstance();
        Settings.RepeatOne repeatOne = settings.getRepeatOne();
        switch (repeatOne) {
            case ON:
                settings.setRepeatOne(Settings.RepeatOne.OFF);
                break;
            case OFF:
                settings.setRepeatOne(Settings.RepeatOne.ON);
                break;
        }
    }

    @Override
    public void repeatPlaylistCheckListener() {
        Log.d(TAG, "repeatPlaylistCheckListener()");
        Settings settings = Settings.getInstance();
        Settings.RepeatPlaylist repeatPlaylist = settings.getRepeatPlaylist();
        switch (repeatPlaylist) {
            case ON:
                settings.setRepeatPlaylist(Settings.RepeatPlaylist.OFF);
                break;
            case OFF:
                settings.setRepeatPlaylist(Settings.RepeatPlaylist.ON);
                break;
        }
    }

    @Override
    public void lockCheckListener() {
        Settings settings = Settings.getInstance();
        Settings.ScreenLock screenLock = settings.getScreenLock();
        switch (screenLock) {
            case ON:
                //画面回転解禁
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                settings.setScreenLock(Settings.ScreenLock.OFF);
                break;
            case OFF:
                //画面横固定
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                settings.setScreenLock(Settings.ScreenLock.ON);
                break;
        }

    }

    /**
     * 他のアプリからの共有でアプリが開かれたとき
     * のハンドリングメゾッド
     *
     * @param intent
     */
    private void shareHandle(Intent intent) {
        Log.d(TAG, "shareHandle()");
        Bundle extras = intent.getExtras();
        if (extras != null) {
            CharSequence ext = extras.getCharSequence(Intent.EXTRA_TEXT);
            if (ext != null) {
                final String StrUrl = String.valueOf(ext);
                Log.d(TAG, StrUrl);

                getSupportLoaderManager().restartLoader(Config.YouTubeShareVideoGetLoader, null, new LoaderManager.LoaderCallbacks<YouTubeVideo>() {
                    @Override
                    public Loader<YouTubeVideo> onCreateLoader(int id, Bundle args) {
                        return new YouTubeShareVideoGetLoader(getApplicationContext(), StrUrl);
                    }

                    @Override
                    public void onLoadFinished(Loader<YouTubeVideo> loader, YouTubeVideo data) {
                        if (data == null) {
                            Toast.makeText(MainActivity.this, "このビデオは開けません", Toast.LENGTH_LONG).show();
                            return;
                        }
                        List<YouTubeVideo> video = new ArrayList<YouTubeVideo>();
                        video.add(data);
                        onPlaylistSelected(video, 0);
                    }

                    @Override
                    public void onLoaderReset(Loader<YouTubeVideo> loader) {

                    }
                }).forceLoad();

            }
        }
    }

    /**
     * Change the vertical and horizontal lengths of the playback space of the movie according to the screen size of the smartphone
     * <p>
     * surfaceViewの大きさを整えるメゾッド
     * スマホの画面サイズに合わせて動画の再生スペースの縦横の長さを変化させる
     */
    private void resizeSurfaceView() {
        if (mPreview.isEnabled()) {
            ViewGroup.LayoutParams svlp = mPreview.getLayoutParams();
            int width = mPreview.getWidth();
            svlp.height = width / 16 * 9;
            mPreview.setLayoutParams(svlp);
        }
    }


    /**
     * 以下、mediacontroller用のinterface
     * mediacontrollerにserviceを直接セットするとなぜか落ちるので
     * いちどactivityにバインドさせてからserviceに渡す
     */

    @Override
    public void start() {
        service.start();
    }

    @Override
    public void pause() {
        service.pause();
    }

    @Override
    public int getDuration() {
        return service.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return service.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        service.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return service.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return service.getAudioSessionId();
    }

    @Override
    public boolean canPause() {
        return service.canPause();
    }

    @Override
    public boolean canSeekBackward() {
        return service.canSeekBackward();
    }

    @Override
    public boolean canSeekForward() {
        return service.canSeekBackward();
    }

    @Override
    public int getAudioSessionId() {
        return service.getAudioSessionId();
    }


}