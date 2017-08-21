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
import android.app.Activity;
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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.Toast;

import com.facebook.network.connectionclass.ConnectionClassManager;
import com.facebook.network.connectionclass.ConnectionQuality;
import com.facebook.network.connectionclass.DeviceBandwidthSampler;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.Scopes;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.client.util.Strings;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.smedic.tubtub.database.YouTubeSqlDb;
import com.smedic.tubtub.fragments.FavoritesFragment;
import com.smedic.tubtub.fragments.PlaylistsFragment;
import com.smedic.tubtub.fragments.RecentlyWatchedFragment;
import com.smedic.tubtub.fragments.SearchFragment;
import com.smedic.tubtub.interfaces.OnFavoritesSelected;
import com.smedic.tubtub.interfaces.OnItemSelected;
import com.smedic.tubtub.model.ItemType;
import com.smedic.tubtub.model.YouTubePlaylist;
import com.smedic.tubtub.model.YouTubeVideo;
import com.smedic.tubtub.utils.Config;
import com.smedic.tubtub.utils.NetworkConf;
import com.smedic.tubtub.youtube.SuggestionsLoader;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static com.google.api.client.http.HttpMethods.HEAD;
import static com.smedic.tubtub.R.layout.suggestions;
import static com.smedic.tubtub.YouTubeFragment.setVideoId;
import static com.smedic.tubtub.utils.Auth.SCOPES;
import static com.smedic.tubtub.youtube.YouTubeSingleton.getCredential;
import static com.smedic.tubtub.youtube.YouTubeSingleton.getYouTubeWithCredentials;

/**
 * Activity that manages fragments and action bar
 */
public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,
        OnItemSelected, OnFavoritesSelected,SearchFragment.onMovieChangeListner,SurfaceHolder.Callback, MediaController.MediaPlayerControl {
    public static Handler mainHandler=new Handler();

    public Context getMainContext() {
        return mainContext;
    }

    private   final Context mainContext=this;

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
    static final int REQUEST_CODE_TOKEN_AUTH=1006;

    private int initialColor = 0xffff0040;
    private int initialColors[] = new int[2];

    private SearchFragment searchFragment;
    private RecentlyWatchedFragment recentlyPlayedFragment;
    private FavoritesFragment favoritesFragment;

    public final String YouTubeFragment="YouTubeFragment";

    private String movieUrl;
    private SurfaceHolder mHolder;
    private SurfaceView mPreview;
    private MediaPlayer mMediaPlayer = null;
    private MediaController mMediaController;

    public void setMovieUrl(String movieUrl){
        this.movieUrl=movieUrl;
    }
    public SurfaceHolder getmHolder(){
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
        if(mPreview==null){
            Log.d("kandabashi","Surface is null!");
        }
        mHolder = mPreview.getHolder();
        mHolder.addCallback(this);

        // MediaPlayerを利用する
        mMediaPlayer = new MediaPlayer();
        // MediaControllerを利用する
        mMediaController = new MediaController(this);
        mMediaController.setMediaPlayer(this);
        mMediaController.setAnchorView(mPreview);
 /*Mediaplayer関係ここまで*/



/*
        Fragment youtubeFragment = new YouTubeFragment();
        // Compatibility packageを使うとFragmentActivityを継承するので
        // Fragment.Activity.getFragmentManager()は使えないので
        // 代わりにFragmentActivity.getSupportFragmentManager()を使う

        // FragmentTransaction ft = getFragmentManager().beginTransaction();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();


        //第一引数：ViewGroupのid 第二引数：Fragmentを継承したクラスのインスタンス 第三引数：タグ
        ft.add(R.id.fragment, youtubeFragment, YouTubeFragment).commit();
*/
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
        Log.d("kandabashi","onResume");
        super.onResume();
        // allow to continue playing media in the background.
        // バックグラウンド再生を許可する
        requestVisibleBehind(true);
    }

    public boolean onDestroy(MediaPlayer mp, int what, int extra) {
        Log.d("kandabashi" ,"onDestroy");
        if (mp != null) {
            mp.release();
            mp = null;/*元は全部mp*/
        }
        return false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder paramSurfaceHolder) {
        Log.d("kandabashi", "surfaceCreated");
        /*mediaplayer関係*/
        // URLの先にある動画を再生する
        Uri mediaPath = Uri.parse("https://r2---sn-5n5ip-ioql.googlevideo.com/videoplayback?dur=179.258&itag=22&pl=20&ip=61.213.93.242&key=yt6&mt=1503274550&ms=au&source=youtube&mv=m&id=o-AFscPxausAVy59Ia7Pn_f2HR13bk6l_aE4985VSbQkCc&expire=1503296260&mm=31&mn=sn-5n5ip-ioql&mime=video%2Fmp4&signature=87E786D6024E2745B6BB1F94FD7D037D4E679871.E1F25CF5432AF486F3EAE1EF4D540230D9DB01F7&lmt=1503053602061290&ratebypass=yes&ipbits=0&requiressl=yes&ei=pCaaWayaHZyJgAPg2bDAAQ&initcwndbps=2857500&sparams=dur%2Cei%2Cid%2Cinitcwndbps%2Cip%2Cipbits%2Citag%2Clmt%2Cmime%2Cmm%2Cmn%2Cms%2Cmv%2Cpl%2Cratebypass%2Crequiressl%2Csource%2Cexpire0"/*movieUrl*/);
        try {
            Log.d("kandabashi", "surfaceCreated-1");
            mMediaPlayer=new MediaPlayer();
            Log.d("kandabashi", "surfaceCreated-2");
            mMediaPlayer.setDataSource(this, mediaPath);
            Log.d("kandabashi", "surfaceCreated-3");
            mPreview=(SurfaceView)findViewById(R.id.surface);
            mHolder=mPreview.getHolder();
            Log.d("kandabashi", "surfaceCreated-3.5");
            mMediaPlayer.setDisplay(mHolder/*paramSurfaceHolder*/);
            //mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            Log.d("kandabashi", "surfaceCreated-4");
            mMediaPlayer.prepare();
            Log.d("kandabashi", "surfaceCreated-5");
            mMediaPlayer.start();
        } catch (IllegalArgumentException e) {
            Log.d("kakndabashi","surfaceCreated-IllegalArgumentException"+e.getMessage());
            e.printStackTrace();
        } catch (IllegalStateException e) {
            Log.d("kakndabashi","surfaceCreated-IllegalStateException"+e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            Log.d("kakndabashi","surfaceCreated-IOException"+e.getMessage());
            e.printStackTrace();
        }
      /*mediaplayer関係ここまで*/

    }

    @Override
    public void surfaceChanged(SurfaceHolder paramSurfaceHolder, int paramInt1,
                               int paramInt2, int paramInt3) {
        Log.d("kandabashi", "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder paramSurfaceHolder) {
        Log.d("kandabashi", "surfaceDestroyed");
        //mMediaPlayer.setDisplay(null);

        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    // ここから先はMediaController向け --------------------------
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, "KeyCode:"+ event.getKeyCode());
        if (event.getKeyCode() != KeyEvent.KEYCODE_BACK) {
            if (!mMediaController.isShowing()){
                mMediaController.show();
            } else {
                mMediaController.hide();
            }
        }
        return super.dispatchKeyEvent(event);
    }
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
                    Log.d("kandabashi","onActivityResult account");
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
                                        "oauth2:" +  " "  +"https://www.googleapis.com/auth/youtube" +" "+"https://www.googleapis.com/auth/youtube.readonly"+" "+"https://www.googleapis.com/auth/youtube.upload"+" "+"https://www.googleapis.com/auth/youtubepartner-channel-audit");
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(mainContext,"ログイン成功:\n"+getCredential().getSelectedAccountName(),Toast.LENGTH_LONG).show();
                                    }
                                });
                            } catch (IOException transientEx) {
                                // Network or server error, try later
                                Log.e("kandabashi" , transientEx.toString());
                            } catch (UserRecoverableAuthException e) {
                                // Recover (with e.getIntent())
                                Log.e("kandabashi", e.toString());
                                Intent recover = e.getIntent();
                                startActivityForResult(recover, REQUEST_CODE_TOKEN_AUTH);
                            } catch (GoogleAuthException authEx) {
                                // The call is not ever expected to succeed
                                // assuming you have already verified that
                                // Google Play services is installed.
                                Log.e("kandabashi", getCredential().getSelectedAccountName()+":"+authEx.toString());
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
        }else if(requestCode== REQUEST_CODE_TOKEN_AUTH){
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

    /*再生中の動画を変更するためのメゾッド*/
    public void movieChange(String videoId){
        /*Log.d("kandabashi","MainActivity-movieChange");
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(YouTubeFragment);
        if(fragment!=null&&fragment instanceof YouTubeFragment){
            ( (YouTubeFragment)fragment).setVideoId(videoId);

            Fragment youtubeFragment = new YouTubeFragment();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            //第一引数：ViewGroupのid 第二引数：Fragmentを継承したクラスのインスタンス 第三引数：タグ
            ft.replace(R.id.fragment, youtubeFragment, YouTubeFragment);
            ft.addToBackStack(null);
            ft.commit();

        }*/
    }
    /*品質を考えてる?*/
    /*
    private YtFile getBestStream(SparseArray<YtFile> ytFiles) {
        ConnectionQuality connectionQuality = ConnectionQuality.MODERATE;
        connectionQuality = ConnectionClassManager.getInstance().getCurrentBandwidthQuality();
        int[] itags = new int[]{251, 141, 140, 17};

        if (connectionQuality != null && connectionQuality != ConnectionQuality.UNKNOWN) {
            switch (connectionQuality) {
                case POOR:
                    itags = new int[]{17, 140, 251, 141};
                    break;
                case MODERATE:
                    itags = new int[]{251, 141, 140, 17};
                    break;
                case GOOD:
                case EXCELLENT:
                    itags = new int[]{141, 251, 140, 17};
                    break;
            }
        }

        if (ytFiles.get(itags[0]) != null) {
            return ytFiles.get(itags[0]);
        } else if (ytFiles.get(itags[1]) != null) {
            return ytFiles.get(itags[1]);
        } else if (ytFiles.get(itags[2]) != null) {
            return ytFiles.get(itags[2]);
        }
        return ytFiles.get(itags[3]);
    }
    DeviceBandwidthSampler deviceBandwidthSampler = DeviceBandwidthSampler.getInstance();*/

    /*●●ここを動画再生の動作にすればいい？●●*/
    @Override
    public void onVideoSelected(final YouTubeVideo video) {

        if (!networkConf.isNetworkAvailable()) {
            networkConf.createNetErrorDialog();
            return;
        }

        String youtubeLink=Config.YOUTUBE_BASE_URL+video.getId();
        //deviceBandwidthSampler.startSampling();
        new YouTubeExtractor(this) {
            @Override
            protected void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta videoMeta) {
                Log.d("kandabashi","onExtractionComplete");
                if (ytFiles == null) {
                    // Something went wrong we got no urls. Always check this.
                    Toast.makeText(YTApplication.getAppContext(), R.string.failed_playback,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (ytFiles != null) {
                    int itag = 22;
                    String downloadUrl = ytFiles.get(itag).getUrl();
                    setMovieUrl(downloadUrl);
                    Log.d("kandabashi","URL:"+downloadUrl);
                }
                //deviceBandwidthSampler.stopSampling();
                //YtFile ytFile = getBestStream(ytFiles);
                /*try {
                    if (mMediaPlayer != null) {
                        mMediaPlayer.reset();
                        mMediaPlayer.setDataSource(ytFile.getUrl());
                        //mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mMediaPlayer.setDisplay(mHolder);
                        mMediaPlayer.prepare();
                        mMediaPlayer.start();

                        Toast.makeText(YTApplication.getAppContext(), video.getTitle(), Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException io) {
                    io.printStackTrace();
                }*/
            }
        }.execute(youtubeLink);




        Log.d("kandabashi","onVideoSelected");
       /* try {
            String ytInfoUrl = "http://www.youtube.com/get_video_info?video_id=" + video.getId() + "&eurl="
                    + URLEncoder.encode("https://youtube.googleapis.com/v/" + video.getId(), "UTF-8");
            setMovieUrl(/*Config.YOUTUBE_BASE_URL + video.getId()*//*ytInfoUrl);*/
        /*}catch (Exception e){

            Log.d("kandabashi","MainActivity-onVideoSelected-error:"+e.getMessage());
        }*/
       surfaceCreated(mHolder);
        /*BackgroundAudioService classへ向けたintent*/
     //   Intent serviceIntent = new Intent(this, BackgroundAudioService.class);
        /*再生をセット？*/
       // serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
        /*ビデオ単体をセット*/
      //  serviceIntent.putExtra(Config.YOUTUBE_TYPE, ItemType.YOUTUBE_MEDIA_TYPE_VIDEO);
        /*引数になってる目当てのビデオをintentに詰める*/
       // serviceIntent.putExtra(Config.YOUTUBE_TYPE_VIDEO, video);

        //startService(serviceIntent);
   }

    /*プレイリストをintentに詰めて送るやつ*/
    @Override
    public void onPlaylistSelected(List<YouTubeVideo> playlist, int position) {
        if (!networkConf.isNetworkAvailable()) {
            networkConf.createNetErrorDialog();
            return;
        }
        Intent serviceIntent = new Intent(this, BackgroundAudioService.class);
        serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
        serviceIntent.putExtra(Config.YOUTUBE_TYPE, ItemType.YOUTUBE_MEDIA_TYPE_PLAYLIST);
        serviceIntent.putExtra(Config.YOUTUBE_TYPE_PLAYLIST, (ArrayList) playlist);
        serviceIntent.putExtra(Config.YOUTUBE_TYPE_PLAYLIST_VIDEO_POS, position);
        startService(serviceIntent);
    }

    /*お気に入りfragmentにビデオを追加したり削除したり*/
    @Override
    public void onFavoritesSelected(YouTubeVideo video, boolean isChecked) {
        if (isChecked) {
            favoritesFragment.addToFavoritesList(video);
        } else {
            favoritesFragment.removeFromFavorites(video);
        }
    }

    /*fragment追加？*/
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
        }else if(id==R.id.log_in){
            String[] perms = {Manifest.permission.GET_ACCOUNTS, Manifest.permission.READ_PHONE_STATE};
            if (!EasyPermissions.hasPermissions(this, perms)) {
                EasyPermissions.requestPermissions(this, "YouTubeアカウントにlog inするには連絡先と電話の許可が必要です。\n許可してから再度log inし直してください。",
                        PERMISSIONS, perms);
            }else{
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
}