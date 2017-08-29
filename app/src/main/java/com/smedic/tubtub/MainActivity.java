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
package com.smedic.tubtub;

import android.Manifest;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.MatrixCursor;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
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
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
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
import com.smedic.tubtub.adapters.PlaylistsAdapter;
import com.smedic.tubtub.database.YouTubeSqlDb;
import com.smedic.tubtub.fragments.BlankFragment;
import com.smedic.tubtub.fragments.FavoritesFragment;
import com.smedic.tubtub.fragments.PlaylistDetailFragment;
import com.smedic.tubtub.fragments.PlaylistsFragment;
import com.smedic.tubtub.fragments.RecentlyWatchedFragment;
import com.smedic.tubtub.fragments.SearchFragment;
import com.smedic.tubtub.interfaces.OnFavoritesSelected;
import com.smedic.tubtub.interfaces.OnItemSelected;
import com.smedic.tubtub.model.YouTubePlaylist;
import com.smedic.tubtub.model.YouTubeVideo;
import com.smedic.tubtub.utils.Config;
import com.smedic.tubtub.utils.NetworkConf;
import com.smedic.tubtub.youtube.SuggestionsLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static com.smedic.tubtub.R.layout.suggestions;
import static com.smedic.tubtub.youtube.YouTubeSingleton.getCredential;
import static com.smedic.tubtub.youtube.YouTubeSingleton.getYouTubeWithCredentials;

/**
 * Activity that manages fragments and action bar
 */
public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,
        OnItemSelected, OnFavoritesSelected,SurfaceHolder.Callback, MediaController.MediaPlayerControl,PlaylistsAdapter.OnDetailClickListener {
    public static Handler mainHandler = new Handler();

    public Context getMainContext() {
        return mainContext;
    }

    private final Context mainContext = this;

    private static final String TAG = "SMEDIC MAIN ACTIVITY";
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    private static final int PERMISSIONS = 1;
    private static final String PREF_BACKGROUND_COLOR = "BACKGROUND_COLOR";
    private static final String PREF_TEXT_COLOR = "TEXT_COLOR";
    public static final String PREF_ACCOUNT_NAME = "accountName";

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    static final int REQUEST_CODE_TOKEN_AUTH = 1006;

    private int initialColor = 0xffff0040;
    private int initialColors[] = new int[2];

    private SearchFragment searchFragment;
    private RecentlyWatchedFragment recentlyPlayedFragment;
    private FavoritesFragment favoritesFragment;

    public final String YouTubeFragment = "YouTubeFragment";

    private String videoUrl;
    private String audioUrl;
    private SurfaceHolder mHolder;
    private SurfaceView mPreview;
    private MediaPlayer mMediaPlayer = null;
    private MediaPlayer mAudioMediaPlayer = null;
    private MediaController mMediaController;
    private AlertDialog.Builder mListDlg;
    private AlertDialog.Builder mTitleDlg;
    private ProgressDialog mProgressDialog;

    /*動画タイトル用*/
    private TextView mTextView;
    private String VideoTitle;



    /*途中から再生のための再生位置入れとく変数*/
    private int MediaStartTime=0;
    public int getMediaStartTime() {
        return MediaStartTime;
    }
    public void setMediaStartTime(int mediaStartTime) {
        MediaStartTime = mediaStartTime;
    }

   /*フラグたち*/
    private boolean HOME_BUTTON_PAUSE = false;
    private int COMPLETION_COUNT = 0;
    private boolean START_INITIAL=true;




    public SurfaceHolder getmHolder() {
        return this.mHolder;
    }

    private int[] tabIcons = {
            R.drawable.ic_action_heart,
            R.drawable.ic_recently_wached,
            R.drawable.ic_search,
            R.drawable.ic_action_playlist
    };

    private NetworkConf networkConf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
/*mediaplayer関係開始*/

        // スクリーンセーバをオフにする
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFormat(PixelFormat.TRANSPARENT);

        mPreview = (SurfaceView) findViewById(R.id.surface);
        if (mPreview == null) {
            Log.d("kandabashi", "Surface is null!");
        }
        mHolder = mPreview.getHolder();
        mHolder.addCallback(this);

        // MediaPlayerを利用する
        mMediaPlayer = new MediaPlayer();
        mAudioMediaPlayer = new MediaPlayer();
        // MediaControllerを利用する
        mMediaController = new MediaController(this);
        mMediaController.setMediaPlayer(this);
        mMediaController.setAnchorView(mPreview);

        /*mMediaController表示のためのtouchlistener*/
        mPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d("kandabashi", "onTouch");
                boolean r = v instanceof SurfaceView;
                boolean y = mMediaPlayer != null;
                Log.d("kandabashi", String.valueOf(r) + String.valueOf(y));
                if (event.getAction() == MotionEvent.ACTION_DOWN && v instanceof SurfaceView && mMediaPlayer != null) {
                    if (!mMediaController.isShowing()) {
                        mMediaController.show();
                    } else {
                        mMediaController.hide();
                    }
                }
                return true;
            }
        });

        mTextView=(TextView)findViewById(R.id.title_view);
        mListDlg=new AlertDialog.Builder(this);
        mTitleDlg=new AlertDialog.Builder(this);
        mProgressDialog = new ProgressDialog(this);

 /*Mediaplayer関係ここまで*/


        YouTubeSqlDb.getInstance().init(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        /*アクションバーに戻るボタンをつけない*/
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        /*viewPageで保持するfragmentを三枚に設定*/
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(3);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        networkConf = new NetworkConf(this);

        setupTabIcons();
        loadColor();

        requestPermissions();
    }

    /*mediaplayer関係*/
    @SuppressLint("NewApi")
    protected void onResume() {
        Log.d("kandabashi", "onResume");
        super.onResume();
        // allow to continue playing media in the background.
        // バックグラウンド再生を許可する
        requestVisibleBehind(true);
        //HOME_BUTTON_PAUSEをfalse,COMPLETION_COUNTを0に
        HOME_BUTTON_PAUSE = false;
        COMPLETION_COUNT = 0;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("kandabashi", "onPause");
    }

    public boolean onDestroy(MediaPlayer mp, int what, int extra) {
        Log.d("kandabashi", "onDestroy");
        if (mp != null) {
            mp.release();
            mp = null;/*元は全部mp*/
        }
        return false;
    }

    public void onStart() {
        super.onStart();
        Log.d("kandabashi", "onStart");

    }

    public void onRestart() {
        super.onRestart();
        Log.d("kandabashi", "onRestart");
    }

    public void onStop() {
        super.onStop();
        Log.d("kandabashi", "onStop");
    }



    @Override
    public void surfaceCreated(SurfaceHolder paramSurfaceHolder) {
        Log.d("kandabashi", "surfaceCreated");
        Log.d("kandabashi","START_INITIAL:"+String.valueOf(START_INITIAL)+"\nmMediaplayer.isPlying:"+String.valueOf(mMediaPlayer.isPlaying()));
        if((!START_INITIAL)){
            setMediaStartTime(mMediaPlayer.getCurrentPosition());
        }
        START_INITIAL=false;
        mMediaPlayer.reset();
        /*mediaplayer関係*/
        // URLの先にある動画を再生する
        if (videoUrl != null) {
            Uri mediaPath = Uri.parse(videoUrl);
            try {
                /*音声だけ再生が動いてるときはまずそれを止める。→2重再生を防ぐため*/
                /*ホームボタン→画面off→画面on→アプリに戻るをしたときにここに制御が来る。*/
                if (mAudioMediaPlayer.isPlaying()) {
                    Log.d("kandabashi", "mAudioMediaPlayer.isPlaying-mAudioMediaPlayer.getCurrentPosition()");
                    Log.d("kandabashi", "mAudioMediaPlayer.isPlaying-mAudioMediaPlayer.getCurrentPosition()"+mAudioMediaPlayer.getCurrentPosition());
                    setMediaStartTime(mAudioMediaPlayer.getCurrentPosition());
                }
                mAudioMediaPlayer.reset();
                Log.d("kandabashi", "surfaceCreated-1");
                mMediaPlayer.setDataSource(this, mediaPath);
                Log.d("kandabashi", "surfaceCreated-2");
                mMediaPlayer.setDisplay(paramSurfaceHolder);
                if(VideoTitle!=null) {
                    mTextView.setText(VideoTitle);
                }
             /*prepareに時間かかることを想定し直接startせずにLister使う*/
                mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        Log.d("kandabashi", "onPrepared");
                        mp.seekTo(getMediaStartTime());
                        mp.start();
                        setMediaStartTime(0);
                        /*読み込み中ダイアログ消す*/
                        mProgressDialog.dismiss();
                    }
                });
               // mMediaPlayer.setOnPreparedListener(this);
                Log.d("kandabashi", "surfaceCreated-3");
                mMediaPlayer.prepare();
                //setMediaStartTime(0);
                Log.d("kandabashi", "surfaceCreated-4");
            } catch (IllegalArgumentException e) {
                Log.d("kakndabashi", "surfaceCreated-IllegalArgumentException" + e.getMessage());
                e.printStackTrace();
            } catch (IllegalStateException e) {
                Log.d("kakndabashi", "surfaceCreated-IllegalStateException" + e.getMessage());
                e.printStackTrace();
            } catch (IOException e) {
                Log.d("kakndabashi", "surfaceCreated-IOException" + e.getMessage());
                e.printStackTrace();
            }
      /*mediaplayer関係ここまで*/
        }

    }


    /*Homeボタンでバックグラウンド再生の時は音声だけのメディアプレイヤー使っちゃう*/
    public void audioCreated() {
        Log.d("kandabashi", "audioCreated");
        mMediaPlayer.reset();
        mAudioMediaPlayer.reset();
        if (audioUrl != null) {
            Uri mediaPath = Uri.parse(audioUrl);
            try {
                mAudioMediaPlayer.setDataSource(this, mediaPath);
                Log.d("kandabashi", "audioCreated-1");
                mAudioMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
             /*prepareに時間かかることを想定し直接startせずにLister使う*/
                mAudioMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        Log.d("kandabashi", "onPrepared");
                        mp.start();
                        setMediaStartTime(0);
                    }
                });
                Log.d("kandabashi", "audioCreated-2");
                mAudioMediaPlayer.prepare();
                Log.d("kandabashi", "audioCreated-3");
            } catch (IllegalArgumentException e) {
                Log.d("kakndabashi", "audioCreated-IllegalArgumentException:" + e.getMessage());
                e.printStackTrace();
            } catch (IllegalStateException e) {
                Log.d("kakndabashi", "audioCreated-IllegalStateException:" + e.getMessage());
                e.printStackTrace();
            } catch (IOException e) {
                Log.d("kakndabashi", "audioCreated-IOException:" + e.getMessage());
                e.printStackTrace();
            }
        }

    }


    @Override
    public void surfaceChanged(SurfaceHolder paramSurfaceHolder, int paramInt1,
                               int paramInt2, int paramInt3) {
        Log.d("kandabashi", "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder paramSurfaceHolder) {
        Log.d("kandabashi", "surfaceDestroyed");
        HOME_BUTTON_PAUSE = true;
    }

    // ここから先はMediaController向け --------------------------

    @Override
    public void start() {
        mMediaPlayer.start();
    }

    @Override
    public void pause() {
        mMediaPlayer.pause();
    }

    @Override
    public int getDuration() {
        return mMediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        mMediaPlayer.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    /*mediaplayer関係ここまで*/

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
            /*ビルド環境が低かったりするとスルーされてきてしまうのでここでもう一度チェック？*/
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
                        "This app needs to access your Google account (via Contacts).",
                        REQUEST_PERMISSION_GET_ACCOUNTS,
                        Manifest.permission.GET_ACCOUNTS);
            }
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.all_permissions_request),
                    PERMISSIONS, perms);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ACCOUNT_PICKER) {
            if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                if (accountName != null) {
                    Log.d("kandabashi", "onActivityResult account");
                    /*SharedPreference:アプリの設定データをデバイス内に保存するための仕組み。*/
                    SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(PREF_ACCOUNT_NAME, accountName);
                    editor.apply();
                    getCredential().setSelectedAccountName(accountName);



                    /*ログインする*/
                    AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
                        @Override
                        protected String doInBackground(Void... params) {
                            String token = null;
                            try {

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
                                Log.e("kandabashi", transientEx.toString());
                            } catch (UserRecoverableAuthException e) {
                                // Recover (with e.getIntent())
                                Log.e("kandabashi", e.toString());
                                Intent recover = e.getIntent();
                                startActivityForResult(recover, REQUEST_CODE_TOKEN_AUTH);
                            } catch (GoogleAuthException authEx) {
                                // The call is not ever expected to succeed
                                // assuming you have already verified that
                                // Google Play services is installed.
                                Log.e("kandabashi", getCredential().getSelectedAccountName() + ":" + authEx.toString());
                            }
                            return token;
                        }

                        @Override
                        protected void onPostExecute(String token) {
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
        setIntent(intent);

        handleIntent(intent);
    }

    /**
     * Handle search intent and queries YouTube for videos
     *
     * @param intent
     */
    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);

            /*スムーズスクロールありでfragmenを2に変更*/
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
        PlaylistDetailFragment playlistDetailFragment=PlaylistDetailFragment.newInstance();

        adapter.addFragment(favoritesFragment, null);/*0*/
        adapter.addFragment(recentlyPlayedFragment, null);/*1*/
        adapter.addFragment(searchFragment, null);/*2*/
        adapter.addFragment(playlistsFragment, null);/*3*/
        //adapter.addFragment(playlistDetailFragment,null);
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


    @Override
    public void onPlaylistSelected(List<YouTubeVideo> playlist, final int position) {
        Log.d("kandabashi", "onPlaylistSelected");
        /*読み込み中ダイアログ表示*/
        /*進捗状況は表示しない*/
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage("Loading...");
        mProgressDialog.show();

        if (!networkConf.isNetworkAvailable()) {
            networkConf.createNetErrorDialog();
            return;
        }

        final List<YouTubeVideo> playList = playlist;
        final YouTubeVideo video = ((ArrayList<YouTubeVideo>) playlist).get(position);
        final int currentSongIndex = position;
        /*surfaceDetroy時に自動的に2回連続でonplaylistselected呼ばれるっぽい*/
        /*homeボタンによるバックグラウンド再生ではない又はhomeボタンによるバックグラウンド再生で再生中の曲が終わって呼ばれた時*/
        if (!HOME_BUTTON_PAUSE || (COMPLETION_COUNT != 1 && COMPLETION_COUNT != 2)) {
            START_INITIAL=true;
            String youtubeLink = Config.YOUTUBE_BASE_URL + video.getId();
            new YouTubeExtractor(this) {
                @Override
                protected void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta videoMeta) {
                    Log.d("kandabashi", "onExtractionComplete");
                    if (ytFiles == null) {
                        // Something went wrong we got no urls. Always check this.
                        Toast.makeText(YTApplication.getAppContext(), R.string.failed_playback,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                /*Videoでは形式を変えて360p（ノーマル画質）で試す。アプリを軽くするため高画質は非対応にした。これ以上落とすと音声が含まれなくなっちゃう。*/
                    int[] itagVideo = {18, 134, 243};
                    int[] itagAudio = {251, 141, 140, 17};
                    if (ytFiles != null) {
                        int tagVideo = 0;
                        int tagAudio = 0;
                        for (int i : itagVideo) {
                                if (ytFiles.get(i) != null) {
                                    tagVideo = i;
                                    break;
                                }
                            }
                            for (int i : itagAudio) {
                                if (ytFiles.get(i) != null) {
                                    tagAudio = i;
                                    break;
                                }
                            }


                        Log.d("kandabashi","video name:"+video.getTitle()+"\ntagAudio:"+String.valueOf(tagAudio)+"\ntagVideo"+String.valueOf(tagVideo));
                        if (tagVideo != 0 && tagAudio!=0) {
                        /*最近見たリストに追加*/
                            YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).create(video);

                            Log.d("kandabashi", "ytFile-not-null-tagVideo:" + String.valueOf(tagVideo));
                            Log.d("kandabashi", "ytFile-not-null-tagAudio:" + String.valueOf(tagAudio));
                            String videoDownloadUrl = ytFiles.get(tagVideo).getUrl();
                            String audioDownloadUrl=ytFiles.get(tagAudio).getUrl();
                            Log.d("kandabashi", "VideoURL:" + videoDownloadUrl);
                            Log.d("kandabashi", "audioURL:" + audioDownloadUrl);
                    /*resetとかここでやらないとダメ（非同期処理だから外でやると追いつかない）*/
                    /*ポーズから戻ったときのためMovieUrlも変えとく*/
                            videoUrl=videoDownloadUrl;
                            audioUrl=audioDownloadUrl;
                            VideoTitle=video.getTitle();
                            START_INITIAL=true;
                            if (!HOME_BUTTON_PAUSE) {
                                Log.d("kandabashi","mMediaPlayer.isPlaying:"+String.valueOf(mMediaPlayer.isPlaying()));
                                surfaceCreated(mHolder);
                            } else {
                                audioCreated();
                            }
                        } else if (currentSongIndex + 1 < playList.size()) {
                            Log.d("kandabashi", "ytFile-null-next:" + video.getId());
                            Toast.makeText(mainContext, "このビデオは読み込めません。次のビデオを再生します。", Toast.LENGTH_LONG).show();
                            onPlaylistSelected(playList, (currentSongIndex + 1) % playList.size());
                        }

                    }

                }
            }.execute(youtubeLink);
        }else{
            Log.d("kandabashi","COMPLETION_COUNT:"+String.valueOf(COMPLETION_COUNT));
            return;
        }

 /*終了後次の曲に行くためのリスナー*/
        /*再帰呼び出しになってしまってる？*/
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (currentSongIndex + 1 < playList.size()) {
                    Log.d("kandabashi", " mMediaController.setOnCompletionListener");
                    /*ホームボタンを押すと最初に2回無条件にmediaPlayer.oncompletion呼ばれる。そのための対策*/
                    if (HOME_BUTTON_PAUSE) {
                        /*COMPLETION_COUNTが0～2の時は増やす必要があるがその後は変化させる必要がない。*/
                        if (COMPLETION_COUNT < 3) {
                            ++COMPLETION_COUNT;
                        }
                    }
                    onPlaylistSelected(playList, (currentSongIndex + 1));
                }else{
                    START_INITIAL=true;
                }
            }
        });

        /*homeボタンによるバックグラウンド再生中、再生中の曲が終わったときに次の曲に行くためのリスナー*/
        mAudioMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (currentSongIndex + 1 < playList.size()) {
                    Log.d("kandabashi", " mMediaController.setOnCompletionListener");
                    onPlaylistSelected(playList, (currentSongIndex + 1));
                }else{
                    START_INITIAL=true;
                    setMediaStartTime(0);
                }
            }
        });

        /*戻るボタン・進むボタン*/
        mMediaController.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //next button clicked
                //if (currentSongIndex + 1 < playList.size()) {
                /*一周できるようにした*/
                Log.d("kandabashi", " mMediaController.setPrevNextListeners");
                START_INITIAL=true;
                onPlaylistSelected(playList, (currentSongIndex + 1) % playList.size());
                // }
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //previous button clicked
                // if (currentSongIndex - 1 > 0) {
                /*一周できるようにした*/
                Log.d("kandabashi", " mMediaController.setPrevNextListeners");
                START_INITIAL=true;
                onPlaylistSelected(playList, (currentSongIndex - 1 + playList.size()) % playList.size());
                // }
            }
        });
    }

    /*お気に入りfragmentにビデオを追加したり削除したり*/
    @Override
    public void onFavoritesSelected(YouTubeVideo video, boolean isChecked){
        if (isChecked) {
            favoritesFragment.addToFavoritesList(video);
        } else {
            favoritesFragment.removeFromFavorites(video);
        }
    }

    /*新規プレイリスト作成してvideo追加*/
    private void AddPlaylist(final String title, final String privacyStatus,final YouTubeVideo video) {
        final List<String> errorCatcher=new ArrayList<>();
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
                    String playlistid=youtubeWithCredential.playlists().insert("snippet,status", playlist).execute().getId();
                    AddVideoToPlayList(playlistid,video,false);
                } catch (Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mainContext, "プレイリスト作成に失敗しました。", Toast.LENGTH_LONG).show();
                            errorCatcher.add("error");
                        }
                    });

                }
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mainContext,privacyStatus+"リスト "+title+" を新規作成し\n"+video.getTitle()+" を追加しました。",Toast.LENGTH_LONG).show();
                    }
                });

            }
        }).start();

    }

    /*プレイリストにビデオ追加*/
    private void AddVideoToPlayList (final String playlistId, final YouTubeVideo video, final boolean resultShow){
        final List<String> errorChacher=new ArrayList<String>();
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


                try {
                    getYouTubeWithCredentials().playlistItems().insert("snippet", playlistItem).execute();
                } catch (Exception e) {
                    Log.d("kandabashi", "AddPlaylist Error:" + e.getMessage());
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mainContext, "追加に失敗しました。", Toast.LENGTH_LONG).show();
                        }
                    });
                }
                if(resultShow){
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mainContext," 追加に成功しました。", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();

    }
    /*プレイリストに追加したり*/
    @Override
    public void onAddSelected(final YouTubeVideo video){
        Log.d("kandabashi","onAddSelected");
        /*video タイトル取得*/
        final String videoTitle=video.getTitle();

        /*プレイリストを取得*/
        final ArrayList<YouTubePlaylist> allPlaylist=YouTubeSqlDb.getInstance().playlists().readAll();
        Log.d("kandabashi","AddPlaylistDialog-1-which:"+String.valueOf(allPlaylist.size()));

        /*プレイリストタイトルを取得*/
        final CharSequence[] playlists=new CharSequence[allPlaylist.size()+1];
        int i=0;
        for(YouTubePlaylist p:allPlaylist){
           playlists[i++]=p.getTitle()+"("+String.valueOf(p.getNumberOfVideos())+")";
        }
        playlists[i++]="新規作成して追加";

        /*チェックされたやつの番号を入れておく。*/
        final ArrayList<Integer> checkedItems = new ArrayList<Integer>();
        checkedItems.add(0);


        mListDlg.setTitle("プレイリスト追加：\n"+videoTitle);

        mListDlg.setSingleChoiceItems(playlists,0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkedItems.clear();
                checkedItems.add(which);
            }
        });
        mListDlg.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d("kandabashi", "AddPlaylistDialog-1-which:" + String.valueOf(which));
                if (!checkedItems.isEmpty()) {
                    if (checkedItems.get(0) == (playlists.length - 1)) {
                        //新しいプレイリスト作って追加
                        final EditText titleEdit=new EditText(mainContext);
                        mTitleDlg.setTitle("新規プレイリスト名入力");
                        mTitleDlg.setView(titleEdit);
                        mTitleDlg.setPositiveButton("非公開で作成", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final String title=titleEdit.getText().toString();
                                if(title.length()==0){
                                    Toast.makeText(mainContext,"プレイリスト名は空白は認められません。",Toast.LENGTH_LONG).show();
                                }else {
                                    AddPlaylist(title, "private", video);
                                }
                            }
                        });
                        mTitleDlg.setNeutralButton("公開で作成", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final String title=titleEdit.getText().toString();
                                if(title.length()==0){
                                    Toast.makeText(mainContext,"プレイリスト名は空白は認められません。",Toast.LENGTH_LONG).show();
                                }else {
                                    AddPlaylist(title, "public", video);
                                }
                            }
                        });

                        mTitleDlg.setNegativeButton("cancel",null);
                        mTitleDlg.show();


                    } else {
                       AddVideoToPlayList(allPlaylist.get(checkedItems.get(0)).getId(),video,true);
                           Toast.makeText(mainContext,"プレイリスト "+allPlaylist.get(checkedItems.get(0)).getTitle()+" に "+videoTitle+" を追加しました。",Toast.LENGTH_LONG).show();

                    }
                }
            }
        });

        mListDlg.setNegativeButton("キャンセル",null);

        mListDlg.create().show();
    }

    /*プレイリスト詳細を見るためのリスナーの中身実装*/
public void onDetailClick(){

}
    /**
     * Class which provides adapter for fragment pager
     */
    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
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

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }

    }


    /**/

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

                        getSupportLoaderManager().restartLoader(4, null, new LoaderManager.LoaderCallbacks<List<String>>() {
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
            alertDialog.setIcon(R.mipmap.ic_launcher);

            alertDialog.setMessage(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME + "\n\n" +
                    getString(R.string.email) + "\n\n" +
                    getString(R.string.date) + "\n");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok),
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
        } else if (id == R.id.action_color_picker) {
            /* Show color picker dialog */
            ColorPickerDialogBuilder
                    .with(this)
                    .setTitle(getString(R.string.choose_colors))
                    .initialColor(initialColor)
                    .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                    .setPickerCount(2)
                    .initialColors(initialColors)
                    .density(12)
                    .setOnColorSelectedListener(new OnColorSelectedListener() {
                        @Override
                        public void onColorSelected(int selectedColor) {
                        }
                    })
                    .setPositiveButton(getString(R.string.ok), new ColorPickerClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                            //changeBackgroundColor(selectedColor);
                            if (allColors != null) {
                                setColors(allColors[0], allColors[1]);
                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .showColorEdit(true)
                    .build()
                    .show();
        } else if (id == R.id.log_in) {
            String[] perms = {Manifest.permission.GET_ACCOUNTS, Manifest.permission.READ_PHONE_STATE};
            if (!EasyPermissions.hasPermissions(this, perms)) {
                EasyPermissions.requestPermissions(this, "YouTubeアカウントにlog inするには連絡先と電話の許可が必要です。\n許可してから再度log inし直してください。",
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
     * Loads app theme color saved in preferences
     */
    private void loadColor() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        int backgroundColor = sp.getInt(PREF_BACKGROUND_COLOR, -1);
        int textColor = sp.getInt(PREF_TEXT_COLOR, -1);

        if (backgroundColor != -1 && textColor != -1) {
            setColors(backgroundColor, textColor);
        } else {
            initialColors = new int[]{
                    ContextCompat.getColor(this, R.color.colorPrimary),
                    ContextCompat.getColor(this, R.color.textColorPrimary)};
        }
    }

    /**
     * Save app theme color in preferences
     */
    private void setColors(int backgroundColor, int textColor) {

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(backgroundColor);
        toolbar.setTitleTextColor(textColor);
        TabLayout tabs = (TabLayout) findViewById(R.id.tabs);
        tabs.setBackgroundColor(backgroundColor);
        tabs.setTabTextColors(textColor, textColor);
        setStatusBarColor(backgroundColor);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.edit().putInt(PREF_BACKGROUND_COLOR, backgroundColor).apply();
        sp.edit().putInt(PREF_TEXT_COLOR, textColor).apply();

        initialColors[0] = backgroundColor;
        initialColors[1] = textColor;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setStatusBarColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(color);
        }
    }

   public void onDetailClick(String playlistId) {
       Log.d("kandabashi", "playlist-detail-checked!!!\n\n");
       PlaylistDetailFragment playlistDetailFragment=new PlaylistDetailFragment().newInstance();
       playlistDetailFragment.setPlaylistId(playlistId);
      //FavoritesFragment playlistDetailFragment=new FavoritesFragment();
      //BlankFragment playlistDetailFragment = new BlankFragment();

       FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
       ft.add(R.id.frame_layout,playlistDetailFragment);
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
       ft.addToBackStack(null);
       ft.commit();

   }

    public ViewPager getViewPager() {
        return viewPager;
    }
}