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
import android.arch.lifecycle.Observer;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.kkkkan.youtube.R;
import com.kkkkan.youtube.tubtub.fragments.LandscapeFragment;
import com.kkkkan.youtube.tubtub.fragments.PortraitFragment;
import com.kkkkan.youtube.tubtub.interfaces.CurrentPositionChanger;
import com.kkkkan.youtube.tubtub.interfaces.LoginHandler;
import com.kkkkan.youtube.tubtub.interfaces.OnItemSelected;
import com.kkkkan.youtube.tubtub.interfaces.SurfaceHolderListener;
import com.kkkkan.youtube.tubtub.interfaces.TitlebarListener;
import com.kkkkan.youtube.tubtub.model.YouTubeVideo;
import com.kkkkan.youtube.tubtub.utils.Config;
import com.kkkkan.youtube.tubtub.utils.NetworkConf;
import com.kkkkan.youtube.tubtub.utils.PlaylistsCache;
import com.kkkkan.youtube.tubtub.utils.Settings;
import com.kkkkan.youtube.tubtub.youtube.YouTubeShareVideoGetLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static com.kkkkan.youtube.tubtub.youtube.YouTubeSingleton.getCredential;

/**
 * Activity that manages fragments and action bar
 */
public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,
        OnItemSelected, TitlebarListener, MediaController.MediaPlayerControl, SurfaceHolderListener, LoginHandler, CurrentPositionChanger {
    public static Handler mainHandler = new Handler();
    private MediaPlayerService service;

    private final Context mainContext = this;

    private static final String TAG = "MainActivity";
    private static final String LandscapeFragmentTAG = "LandscapeFragmentTAG";
    private static final String PortraitFragmentTAG = "PortraitFragmentTAG";

    private static final int PERMISSIONS = 1;
    public static final String PREF_ACCOUNT_NAME = "accountName";


    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_CODE_TOKEN_AUTH = 1006;

    private MediaController mMediaController;
    private ProgressDialog mProgressDialog;

    private NetworkConf networkConf;

    private Toast toast;

    private boolean isConnect = false;
    private ServiceConnection connection = new ServiceConnection() {
        /**
         * bindがされたときに呼び出される。
         * serviceが強制終了されたときは、
         * 強制終了時のコールバック関数であるonServiceDisconnected内でunbind()を呼ばない限りactivityとserviceのbindは生きているので
         * serviceが再起動したときにonServiceConnectedが呼ばれる。
         * @param name
         * @param service
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MainActivity.this.service = ((MediaPlayerService.MediaPlayerBinder) service).getService();
            isConnect = true;
        }

        /**
         * serviceがクラッシュされたり強制終了されたりなど、サービスへの接続が予期せず切断されたときにandroidシステムにより呼び出される。
         * unbind()をactivityにされた際には呼び出されない。
         * @param name
         */
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected(ComponentName name)");
            isConnect = false;
            //unbindService(this);
            MainActivity.this.service = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //serviceにbindする際に渡すコネクション
        MediaPlayerService.getLoadingState().observe(MainActivity.this, new Observer<MediaPlayerService.LoadingState>() {
            @Override
            public void onChanged(@Nullable MediaPlayerService.LoadingState loadingState) {
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
                        if (toast != null) {
                            toast.cancel();
                        }
                        toast = Toast.makeText(mainContext, getString(R.string.video_read_fail), Toast.LENGTH_SHORT);
                        toast.show();
                        break;
                }
            }
        });


        startService(new Intent(this, MediaPlayerService.class));
        bindService(new Intent(this, MediaPlayerService.class), connection, BIND_AUTO_CREATE);

        //Turn off screen saver
        //スクリーンセーバをオフにする
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFormat(PixelFormat.TRANSPARENT);

        mProgressDialog = new ProgressDialog(this);

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
                //Intent inttent=new Intent(service, NextReceiver.class);
                //sendBroadcast(inttent);
                service.nextPlay();
                Log.d(TAG, " mMediaController.setPrevNextListeners");


            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //previous button clicked
                //I made it possible to go around
                //一周できるようにした
                //Intent inttent=new Intent(service, PrevReceiver.class);
                //sendBroadcast(inttent);
                service.prevPlay();
                Log.d(TAG, " mMediaController.setPrevNextListeners");
            }
        });


        networkConf = new NetworkConf(this);
        Configuration config = getResources().getConfiguration();
        switch (config.orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                viewChangeWhenPortrait();
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                //At the time of horizontal screen
                //横画面の時
                viewChangeWhenLandscape();
                break;
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
        // allow to continue playing media in the background.
        // バックグラウンド再生を許可する
        requestVisibleBehind(true);
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
        Log.d(TAG, "onDestroy()");
        //SingletonMediaPlayer.instance.getMediaPlayer().release();
        //mNotificationManagerCompat.cancel(notificationId);
        //サービスへのbind切る
        if (isConnect) {
            Log.d(TAG, "service.unbindService");
            unbindService(connection);
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
        if (EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS)) {
                /*自アプリのみ書き込み可能で開いてアカウントネームを拾ってくる*/
            String accountName = getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
                /*アカウントひとつだけあったら*/
            if (accountName != null) {
                    /*アカウントをセット*/
                getCredential().setSelectedAccountName(accountName);
                return;
            }
        }
        checkPermissionAndLoginGoogleAccount();
    }

    @Override
    public void checkPermissionAndLoginGoogleAccount() {
        String[] perms = {Manifest.permission.GET_ACCOUNTS, Manifest.permission.READ_PHONE_STATE};
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, getString(R.string.all_permissions_request),
                    PERMISSIONS, perms);
        } else {
            startActivityForResult(
                    getCredential().newChooseAccountIntent(),
                    REQUEST_ACCOUNT_PICKER);
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

                    //LoginTaskのためのコールバック関数作成
                    LoginTask.LoginTaskListener loginTaskListener = new LoginTask.LoginTaskListener() {
                        @Override
                        public String loginListener() {
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
                                        if (toast != null) {
                                            toast.cancel();
                                        }
                                        toast = Toast.makeText(mainContext, "ログイン成功:\n" + getCredential().getSelectedAccountName(), Toast.LENGTH_LONG);
                                        toast.show();
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
                    };
                    //LoginTask作成
                    LoginTask loginTask = new LoginTask(loginTaskListener);
                    //ログインする
                    loginTask.execute();
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
        }
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
        if (service != null) {
            service.newPlaylistSelected(playlist, position);
        }
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


    @Override
    public void changeSurfaceHolder(SurfaceHolder holder, SurfaceView surfaceView) {
        boolean setDisplaySuccess = false;
        if (service != null) {
            setDisplaySuccess = service.setDisplay(holder);
        }
        if (!setDisplaySuccess) {
            Log.d(TAG, "!setDisplaySuccess");
            return;
        }
        //縦画面→横画面になったときなど用
        //MediaControllerを付けなおし
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
    }

    /**
     * surfaceDestroyedで呼び出す用の,
     * surfaceHolder解放する用のメゾッド
     *
     * @param holder
     */
    @Override
    public void releaseSurfaceHolder(SurfaceHolder holder) {
        //アプリ起動直後に画面向きを変えるとserver==nullでここにくる
        if (service != null) {
            service.releaseDisplay(holder);
        }
    }


    /**
     * 横画面時の処理
     */
    private void viewChangeWhenLandscape() {
        Log.d(TAG, " viewChangeWhenLandscape() ");
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();
        //フラグメント追加
        Fragment fragment = LandscapeFragment.getNewLandscapeFragment(this);
        transaction.replace(R.id.parent_layout, fragment, LandscapeFragmentTAG);
        //transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }


    /**
     * 縦画面になったときの処理
     */
    private void viewChangeWhenPortrait() {
        Log.d(TAG, "viewChangeWhenPortrait()");
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();
        Fragment fragment = PortraitFragment.getNewPortraitFragment(this);
        transaction.replace(R.id.parent_layout, fragment, PortraitFragmentTAG);
        transaction.commitAllowingStateLoss();
    }


    /**
     * 縦画面のときプレイリスト詳細を表示しているときは
     * バックボタンでプレイリスト詳細を出す前の状態にする
     */
    @Override
    public void onBackPressed() {
        Configuration config = getResources().getConfiguration();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(PortraitFragmentTAG);
        if (config.orientation == Configuration.ORIENTATION_PORTRAIT && fragment != null) {
            android.support.v4.app.FragmentManager fragmentManager = fragment.getChildFragmentManager();
            int popCount = fragmentManager.getBackStackEntryCount();
            if (popCount != 0) {
                fragmentManager.popBackStack();
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    public void shuffleCheckListener() {
        Settings settings = Settings.getInstance();
        Settings.Shuffle shuffle = settings.getShuffle();
        switch (shuffle) {
            case ON:
                Log.d(TAG, "settings.setShuffle(Settings.Shuffle.OFF);");
                settings.setShuffle(Settings.Shuffle.OFF);
                break;
            case OFF:
                Log.d(TAG, "settings.setShuffle(Settings.Shuffle.ON);");
                settings.setShuffle(Settings.Shuffle.ON);
                break;
        }
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
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
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
                            if (toast != null) {
                                toast.cancel();
                            }
                            toast = Toast.makeText(MainActivity.this, "このビデオは開けません", Toast.LENGTH_LONG);
                            toast.show();
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

    @Override
    public void changeCurrentPosition(int newPosition) {
        if (service != null) {
            service.playlistHandle(newPosition);
        }
    }


    /**
     * 以下、mediacontroller用のinterface
     * mediacontrollerにserviceを直接セットすると落ちるので
     * いちどactivityにバインドさせてからserviceに渡す
     * <p>
     * バインドが確立するまではservice=nullであることに注意。
     */

    @Override
    public void start() {
        if (service != null && PlaylistsCache.Instance.getPlayingListSize() != 0) {
            service.start();
        }
    }

    @Override
    public void pause() {
        if (service != null) {
            service.pause();
        }
    }

    @Override
    public int getDuration() {
        //まだ何も再生するビデオがセットされて無かったら0
        return service == null || PlaylistsCache.Instance.getPlayingListSize() == 0 ? 0 : service.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        //まだ何も再生するビデオがセットされて無かったら0
        return service == null || PlaylistsCache.Instance.getPlayingListSize() == 0 ? 0 : service.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        if (service != null && PlaylistsCache.Instance.getPlayingListSize() != 0) {
            service.seekTo(pos);
        }
    }

    @Override
    public boolean isPlaying() {
        return service != null && service.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return service != null ? service.getAudioSessionId() : 0;
    }

    @Override
    public boolean canPause() {
        return service != null && service.canPause();
    }

    @Override
    public boolean canSeekBackward() {
        return service != null && service.canSeekBackward();
    }

    @Override
    public boolean canSeekForward() {
        return service != null && service.canSeekBackward();
    }

    @Override
    public int getAudioSessionId() {
        return service != null ? service.getAudioSessionId() : 0;
    }

    /**
     * ログインする時用のAsyncTask
     * そのまま使うときにnew AsyncTask()して使うとメモリーリークのwarningが出るのでそれを避けるために
     * static inner クラス化
     */
    static private class LoginTask extends AsyncTask<Void, Void, String> {
        private LoginTaskListener loginTaskListener;

        interface LoginTaskListener {
            String loginListener();
        }

        LoginTask(LoginTaskListener loginTaskListener) {
            this.loginTaskListener = loginTaskListener;
        }

        @Override
        protected String doInBackground(Void... voids) {
            return loginTaskListener.loginListener();
        }

        @Override
        protected void onPostExecute(String token) {
            //tokenの文字列の表示
            Log.i(TAG, "Access token retrieved:" + token);
        }
    }
}