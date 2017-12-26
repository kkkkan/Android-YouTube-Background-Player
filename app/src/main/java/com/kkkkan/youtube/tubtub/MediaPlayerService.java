package com.kkkkan.youtube.tubtub;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.arch.lifecycle.MutableLiveData;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.widget.MediaController;
import android.widget.RemoteViews;

import com.crashlytics.android.Crashlytics;
import com.kkkkan.youtube.R;
import com.kkkkan.youtube.tubtub.BroadcastReceiver.NextReceiver;
import com.kkkkan.youtube.tubtub.BroadcastReceiver.PauseStartReceiver;
import com.kkkkan.youtube.tubtub.BroadcastReceiver.PrevReceiver;
import com.kkkkan.youtube.tubtub.database.YouTubeSqlDb;
import com.kkkkan.youtube.tubtub.model.YouTubeVideo;
import com.kkkkan.youtube.tubtub.utils.Config;
import com.kkkkan.youtube.tubtub.utils.PlaylistsCache;
import com.kkkkan.youtube.tubtub.utils.Settings;
import com.kkkkan.youtube.tubtub.utils.VideoQualities;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

import static android.media.MediaPlayer.MEDIA_ERROR_IO;
import static android.media.MediaPlayer.MEDIA_INFO_UNKNOWN;
import static com.kkkkan.youtube.tubtub.utils.Settings.RepeatPlaylist.ON;

/**
 * Created by ka1n9 on 2017/11/24.
 */

public class MediaPlayerService extends Service implements MediaController.MediaPlayerControl {
    static final private String TAG = "MediaPlayerService";
    private final IBinder binder = new MediaPlayerBinder();
    private final MediaPlayer mediaPlayer = new MediaPlayer();
    public RemoteViews mRemoteViews;
    public NotificationCompat.Builder mNotificationCompatBuilder;
    public NotificationManagerCompat mNotificationManagerCompat;
    static private int notificationId = 1;
    private boolean playlistSelectedCancelFlag = false;
    private SurfaceHolder mHolder;

    public enum LoadingState {
        StartLoading,
        StopLoading,
        Error
    }

    private final MutableLiveData<LoadingState> loadingState = new MutableLiveData<>();
    private final MutableLiveData<String> videoTitle = new MutableLiveData<>();

    public MutableLiveData<LoadingState> getLoadingState() {
        return loadingState;
    }

    public MutableLiveData<String> getVideoTitle() {
        return videoTitle;
    }

    private void setStateStartLoading() {
        loadingState.setValue(LoadingState.StartLoading);
    }

    public void setStateStopLoading() {
        loadingState.setValue(LoadingState.StopLoading);
    }

    private void setStateError() {
        loadingState.setValue(LoadingState.Error);
    }

    private String videoUrl;


    public class MediaPlayerBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    public void setPlaylistSelectedCancelFlag(boolean playlistSelectedCancelFlag) {
        this.playlistSelectedCancelFlag = playlistSelectedCancelFlag;
    }

    /**
     * 引数のsurfaceHolderがnullだったらfalseを返す
     *
     * @param holder
     * @return
     */
    public boolean setDisplay(SurfaceHolder holder) {
        mHolder = holder;
        if (holder == null) {
            //基本的にあり得ない
            Log.d(TAG, "changeSurfaceHolderAndTitlebar#\nholder==null");
            return false;
        }
        try {
            mediaPlayer.setDisplay(holder);
        } catch (IllegalArgumentException e) {
            //surfaceの解放や生成の処理が前後してしまい、うまくいっておらず
            //既に解放済みのsurfaceのholderが来てしまったとき
            Log.d(TAG, "changeSurfaceHolderAndTitlebar#IllegalArgumentException\n" + e.getMessage());
            mHolder = null;
            mediaPlayer.setDisplay(null);
        }
        return true;
    }

    /**
     * 引数のsurfaceHolderと今mediaPlayerの投影先になっているsurfaceHolderが同じインスタンスだったら
     * 解放するメゾッド
     *
     * @param holder
     */
    public void releaseDisplay(SurfaceHolder holder) {
        if (holder == mHolder) {
            mHolder = null;
            mediaPlayer.setDisplay(null);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Notification settings
        //Notificationの設定


        //In the notification field · lock screen so that you can see the information on the video currently being played,
        // let's go to the application with a notification tap.
        //通知欄・ロック画面で今再生中のビデオの情報を見れるよう,に、通知タップでアプリに行けるようにします。
        mRemoteViews = new RemoteViews(getApplicationContext().getPackageName(), R.layout.notification_layout);
        //Fly to the app with thumbnail touch
        //サムネイルタッチでアプリに飛ぶ
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.video_thumbnail, pendingIntent);

        //pause_start Touch pause / start
        //pause_startタッチでpause/start
        intent = new Intent(this, PauseStartReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.pause_start, pendingIntent);

        //Go to the previous video with prev touch
        //prevタッチで前のビデオに行く
        intent = new Intent(this, PrevReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.prev, pendingIntent);

        //Go to next video with next touch
        //nextタッチで次のビデオに行く
        intent = new Intent(this, NextReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.next, pendingIntent);


        mRemoteViews.setTextColor(R.id.title_view, Color.BLACK);
        mRemoteViews.setTextColor(R.id.video_duration, Color.BLACK);


        mNotificationCompatBuilder = new NotificationCompat.Builder(getApplicationContext());
        mNotificationCompatBuilder.setContent(mRemoteViews);
        mNotificationCompatBuilder.setSmallIcon(R.drawable.ic_action_playlist);
        //mNotificationCompatBuilder.setContentIntent(null);
        mNotificationManagerCompat = NotificationManagerCompat.from(this);


        //PauseStartReceiverからのブロードキャスト受け取れるように
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PauseStartReceiver.ACTION);
        registerReceiver(pauseStartBroadcastReceiver, intentFilter);

        //PrevReceiverからのブロードキャスト受け取れるように
        intentFilter = new IntentFilter();
        intentFilter.addAction(PrevReceiver.ACTION);
        registerReceiver(prevBroadcastReceiver, intentFilter);

        //NextReceiverからのブロードキャスト受け取れるように
        intentFilter = new IntentFilter();
        intentFilter.addAction(NextReceiver.ACTION);
        registerReceiver(nextBroadcastReceiver, intentFilter);

        //Listener to go to the next song after the end
        //終了後次の曲に行くためのリスナー
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d(TAG, " mMediaController.setOnCompletionListener");
                if (PlaylistsCache.Instance.getPlayingListSize() == 0) {
                    //Originally it should not be playlist == null,
                    // but because there was something that was fallen by null reference with playlist.size ()
                    //本来ならplaylist==nullとなることは無いはずだが、
                    // playlist.size()でnull参照で落ちたことがあったので対策
                    Log.d(TAG, "\nplaylist is null!\n");
                    return;
                }
                handleNextVideo();
            }
        });

        //For MediaPlayer setting · screen, set it with onResume ()
        //Because it may fall when coming from the back to the fore if you do not get surfaceView every time with onResume ().
        // MediaPlayerの設定・画面についてはonResume()で設定する。
        // onResume()でいちいちsurfaceViewを取得し直さないとバックからフォアに来るときに落ちることがあるため。
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.d(TAG, "onError:\nwhat:" + String.valueOf(what)
                        + "\n extra:" + String.valueOf(extra));
                if (what == MEDIA_INFO_UNKNOWN && extra == MEDIA_ERROR_IO) {
                    //MediaPlayer.MEDIA_INFO_UNKNOWN==1
                    //MadiaPlayer.MEDIA_ERROR_IO==-1004

                    //Depending on the model (FUJITSU Arrows M 03: android 6.0.1),
                    // it is coming here that one video is running for 10 to 20 minutes
                    //機種によって(FUJITSU Arrows M03:android 6.0.1)は1つのビデオを10～20分程度流しているとここに来るぽい

                    //It was OK with SO 01G (android 5.0.2)
                    //SO 01G(android 5.0.2)では大丈夫だった

                    //A phenomenon peculiar to android 6?
                    //android 6特有の現象？

                    //Solved by split streaming setting with videoCreate ()?
                    //videoCreate()で分割ストリーミング設定することにより解決？

                    //call setOnComplateListener
                    //setOnComplateListener呼ぶ
                    return false;
                }
                if (what == MEDIA_INFO_UNKNOWN && extra == -2147483648) {
                    //-2147483648= MediaPlayer.MEDIA_ERROR_SYSTEM
                    //何故か認識してもらえなかったのでマジックナンバーで
                    Log.d(TAG, "setOnErrorListener : system error");
                }
                if (what == -38 && extra == 0) {
                    //連続で次のビデオを押されたときに起きやすい
                    //具体的なwhatの内容などはなぞ（onErrorの仕様にはwhat=-38もextra=0もない）
                    Log.d(TAG, "setOnErrorListener : error");
                }
                playSameVideoAgain();
                //do not call setOnComplateListener
                //setOnComplateListener呼ばない
                return true;
            }
        });

       /* //Depending on the model handling info to prevent log.W from being output a lot at every other second in the form of (SONY SO - 01 G:androd 5.0.2),
        // MediaPlayer: info / warning (702, 0)
        //機種によっては(SONY SO-01G:android 5.0.2)、MediaPlayer: info/warning (702, 0)といった形でLog.Wが1秒おきで
        // たくさん出力されるのを防ぐためにinfoをハンドリング
        mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                Log.d(TAG,"setOnInfoListener:onInfo:\nwhat=="+String.valueOf(what)+"\nextra=="+String.valueOf(extra));
                if(what==MEDIA_INFO_BUFFERING_END&&extra==0){
                    //MEDIA_INFO_BUFFERING_END==702
                    //extra == 0 is a mystery
                    //extra==0は謎

                    //A notification indicating that playback of MediaPlayer has started since a certain amount of buffer has accumulated
                    //It seems that you do not need to do anything in particular, and it is annoying to fill the Log and return false
                    //バッファが一定量溜まったのでMediaPlayerの再生を開始したことを示す通知
                    //特になにもしなくてもいいようであり、Logを埋め尽くしてうっとうしいのでfalseを返す
                    return false;
                }
                //Others
                //その他
                //Give Log.W
                //Log.Wを出す
                return true;
            }
        });
        */

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        stopSelf();
    }

    private BroadcastReceiver pauseStartBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "pauseStartBroadcastReceiver");
            if (!mediaPlayer.isPlaying()) {
                mRemoteViews.setImageViewResource(R.id.pause_start, R.drawable.ic_pause_black_24dp);
                //mNotificationManagerCompat.notify(notificationId, mNotificationCompatBuilder.build());
                startForeground(notificationId, mNotificationCompatBuilder.build());
                mediaPlayer.start();
            } else {
                mRemoteViews.setImageViewResource(R.id.pause_start, R.drawable.ic_play_arrow_black_24dp);
                mNotificationManagerCompat.notify(notificationId, mNotificationCompatBuilder.build());
                stopForeground(false);
                mediaPlayer.pause();
            }

        }
    };


    private BroadcastReceiver prevBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "prevBroadcastReceiver");
            int currentVideoIndex = PlaylistsCache.Instance.getCurrentVideoIndex();
            playlistHandle((currentVideoIndex - 1 + PlaylistsCache.Instance.getPlayingListSize()) % PlaylistsCache.Instance.getPlayingListSize());
        }

    };

    private BroadcastReceiver nextBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "nextStartBroadcastReceiver");
            int currentVideoIndex = PlaylistsCache.Instance.getCurrentVideoIndex();
            playlistHandle((currentVideoIndex + 1) % PlaylistsCache.Instance.getPlayingListSize());
        }
    };


    /**
     * 検索画面や、プレイリスト詳細表示画面や履歴などでビデオを選んで再生始めた時に呼ばれるメゾッド。
     * その時点でのリストが渡る。（履歴などは動的に変わるため、再生を始めた時の状態を別に保持しておきたいため）
     */
    public void newPlaylistSelected(List<YouTubeVideo> playlist, int position) {
        Log.d(TAG, "newPlaylistSelected");
        if (playlistSelectedCancelFlag) {
            //もしユーザーがビデオ読み込みやめるよう操作していたら
            //Flagをfalseにする
            playlistSelectedCancelFlag = false;
            //一応Loading…出てたら消す
            setStateStopLoading();
            return;
        }
        PlaylistsCache.Instance.setNewList(playlist);
        if (Settings.getInstance().getShuffle() == Settings.Shuffle.ON) {
            //shuffleモードの時は渡ってきたpositionはnormalListにおける選択したvideoのpositionなので
            // shuffleListでのそのvideoのpositionをplaylistHandle()に渡すpositonに代入する。
            List<YouTubeVideo> list = PlaylistsCache.Instance.getShuffleList();
            position = list.indexOf(playlist.get(position));
        }
        playlistHandle(position);
    }

    /**
     * 選んだビデオをmediaplayerで再生するためのurlをvideoUrlにセットし,
     * 次の制御をvideoCreate()に振る
     *
     * @param position
     */
    public void playlistHandle(final int position) {
        Log.d(TAG, "playlistHandle");
        if (playlistSelectedCancelFlag) {
            //もしユーザーがビデオ読み込みやめるよう操作していたら
            //Flagをfalseにする
            playlistSelectedCancelFlag = false;
            //一応Loading…出てたら消す
            setStateStopLoading();
            return;
        }

        PlaylistsCache.Instance.setCurrentVideoIndex(position);
        Log.d(TAG, "position is " + String.valueOf(position));
        YouTubeVideo v = null;
        switch (Settings.getInstance().getShuffle()) {
            case ON:
                v = PlaylistsCache.Instance.getShuffleList().get(position);
                break;
            case OFF:
                v = PlaylistsCache.Instance.getNormalList().get(position);
                break;
        }
        final YouTubeVideo video = v;

        //読み込み中ダイアログ表示
        setStateStartLoading();

        String youtubeLink = Config.YOUTUBE_BASE_URL + video.getId();
        YouTubeExtractorTask.YouTubeExtractorTaskCallback youTubeExtractorTaskCallback = new YouTubeExtractorTask.YouTubeExtractorTaskCallback() {
            @Override
            public void onExtractionCompleteListener(SparseArray<YtFile> ytFiles, VideoMeta videoMeta) {
                Log.d(TAG, "onExtractionComplete");
                if (ytFiles == null) {
                    if (videoMeta == null) {
                        Log.d(TAG, "ytFiles == null : videoMeta == null : " + video.getTitle());
                    } else {
                        Log.d(TAG, "ytFiles == null : videoMeta title is : " + videoMeta.getTitle() + "\nvideo title is : " + video.getTitle());
                        if (videoMeta.getTitle() != null) {
                            //videoMeta.getTitle()!=nullの時は削除されたビデオではなく、ytFilesを上手く取ってこれなかっただけっぽい
                            playlistHandle(position);
                            return;
                        }
                    }
                    setStateError();
                    handleNextVideo();
                    return;
                }

                //設定によって画質変える
                SharedPreferences sharedPreferences = getSharedPreferences(VideoQualities.VideoQualityPreferenceFileName, Context.MODE_PRIVATE);
                //デフォルトの画質はノーマル
                int videoQualitySetting = sharedPreferences.getInt(VideoQualities.VideoQualityPreferenceKey, VideoQualities.videoQualityNormal);
                Integer[] itagVideo = VideoQualities.getVideoQualityTagsMap().get(videoQualitySetting);

                int tagVideo = 0;

                for (int i : itagVideo) {
                    if (ytFiles.get(i) != null) {
                        tagVideo = i;
                        break;
                    }
                }

                Log.d(TAG, "video name:" + video.getTitle() + "\ntagVideo" + String.valueOf(tagVideo));
                if (tagVideo != 0) {

                    //最近見たリストに追加
                    YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).createIfNotMatchWithTopVideo(video);

                    String videoDownloadUrl = ytFiles.get(tagVideo).getUrl();
                    Log.d(TAG, "VideoURL:" + videoDownloadUrl);

                    //ポーズから戻ったときのためMovieUrlも変えとく
                    videoUrl = videoDownloadUrl;
                    videoTitle.setValue(video.getTitle());

                    //サムネイルの設定
                    Notification notification = mNotificationCompatBuilder.build();
                    Picasso.with(MediaPlayerService.this).load(video.getThumbnailURL()).into(mRemoteViews, R.id.video_thumbnail, notificationId, notification);

                    mRemoteViews.setTextViewText(R.id.title_view, video.getTitle());
                    mRemoteViews.setTextViewText(R.id.video_duration, video.getDuration());
                    mRemoteViews.setImageViewResource(R.id.pause_start, R.drawable.ic_pause_black_24dp);
                    // mNotificationManagerCompat.notify(notificationId, notification);
                    startForeground(notificationId, notification);

                    Log.d(TAG, "SingletonMediaplayer.instance.getMediaPlayer().isPlaying:" + String.valueOf(mediaPlayer.isPlaying()));

                    //Progressダイアログ消すのはvideoCreate()のなかでやってる。
                    videoCreate(position);
                } else {
                    Log.d(TAG, "tag is 0 : " + video.getTitle());
                    String tags = "";
                    for (int i = 0; i < ytFiles.size(); i++) {
                        tags += String.valueOf(ytFiles.get(ytFiles.keyAt(i)).getFormat().getItag()) + " ";
                    }
                    Log.d(TAG, "ytFile's itags is : " + tags);
                    //FabricにLog飛ばす
                    Crashlytics.logException(new Exception("tag is 0 : " + video.getTitle() + "\n" + "ytFile's itags is : " + tags));
                    //viewModel.setStateError();
                    //handleNextVideo(false);
                    //youtue上に動画はあるのにytFilesをうまく取ってこれていないときがあるようなので再度やり直し
                    playlistHandle(position);
                }
            }
        };
        new YouTubeExtractorTask(this, youTubeExtractorTaskCallback).execute(youtubeLink);
    }

    private void videoCreate(final int position) {
        if (videoUrl == null) {
            //通常あり得ない
            Log.d(TAG, "videoCreate()-videoUrl==null");
            return;
        }

        // mediaplayer関係
        // URLの先にある動画を再生する

        Uri mediaPath = Uri.parse(videoUrl);
        try {
            //新しいビデオを再生するために一度resetしてMediaPlayerをIDLE状態にする
            mediaPlayer.reset();
            Log.d(TAG, "videoCreate");
            mediaPlayer.setDataSource(this, mediaPath);
            mediaPlayer.setDisplay(mHolder);

            //prepareに時間かかることを想定し直接startせずにLister使う
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.d(TAG, "onPrepared");
                    //読み込み中ダイアログ消す
                    setStateStopLoading();
                    //videoTitleをセット
                    /*if (videoTitle != null) {
                        setVideoTitle(videoTitle);
                    }*/

                    if (playlistSelectedCancelFlag) {
                        //もしユーザーがビデオ読み込みやめるよう操作していたら
                        //Flagをfalseにする
                        playlistSelectedCancelFlag = false;
                        //スタートせずにreturn
                        return;
                    }

                    mp.start();

                }
            });
            mediaPlayer.prepareAsync();
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "videoCreate-IllegalArgumentException" + e.getMessage());
            e.printStackTrace();
        } catch (IllegalStateException e) {
            //Mediaplyerをstart()しちゃいけないstateのときにstart()してしまったときなど
            Log.d(TAG, "videoCreate-IllegalStateException" + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "videoCreate-IOException" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 次のビデオを再生するとなったときに呼ぶメゾッド。
     * 設定によって同じビデオを再生するか、プレイリストの最後に達していた時に次のビデオに行くかなどを
     * ハンドリングする
     */
    private void handleNextVideo() {
        Log.d(TAG, "handleNextVideo");
        Settings settings = Settings.getInstance();
        int playlistSize = PlaylistsCache.Instance.getPlayingListSize();
        int currentVideoIndex = PlaylistsCache.Instance.getCurrentVideoIndex();
        if (settings.getRepeatOne() == Settings.RepeatOne.ON) {
            // one song repeat
            //1曲リピート時

            //再度同じビデオの再生を試みる
            playSameVideoAgain();
            return;
        }
        /*if (settings.getShuffle() == Settings.Shuffle.ON && playlistSize <= currentVideoIndex + 1) {
            Log.d(TAG,"settings.getShuffle() == Settings.Shuffle.ON && playlistSize <= currentVideoIndex + 1");
            //シャッフルモードでかつシャッフルリストの最後まで来てしまっていた時
            //リピートモードだった時のためにシャッフルし直す
            PlaylistsCash.Instance.reShuffle();
        }*/
        if (settings.getRepeatPlaylist() == ON) {
            // It is not a repeat of one song, and at play list repeat
            //1曲リピートではなく、かつプレイリストリピート時
            playlistHandle((currentVideoIndex + 1) % playlistSize);
            return;
        }


        // When it is neither a single song repeat nor a play list repeat
        //一曲リピートでもプレイリストリピートでもないとき
        if (currentVideoIndex + 1 < playlistSize) {
            playlistHandle(currentVideoIndex + 1);
        } else {
            //最後の曲のときはprogressbarが出ていたらそれを消すだけ。
            setStateStopLoading();
        }
    }

    /**
     * 今と同じビデオをもう一度再生する。
     */
    private void playSameVideoAgain() {
        playlistHandle(PlaylistsCache.Instance.getCurrentVideoIndex());
    }

    public void nextPlay() {
        if (PlaylistsCache.Instance.getPlayingListSize() == 0) {
            //When playlist is not set, you can also press it so that it will not fall at that time
            //playlistセットされてないときも押せてしまうからその時落ちないように
            return;
        }
        int currentVideoIndex = PlaylistsCache.Instance.getCurrentVideoIndex();
        playlistHandle((currentVideoIndex + 1) % PlaylistsCache.Instance.getPlayingListSize());

    }

    public void prevPlay() {
        if (PlaylistsCache.Instance.getPlayingListSize() == 0) {
            //When playlist is not set, you can also press it so that it will not fall at that time
            //playlistセットされてないときも押せてしまうからその時落ちないように
            return;
        }
        int currentVideoIndex = PlaylistsCache.Instance.getCurrentVideoIndex();
        int playlistSize = PlaylistsCache.Instance.getPlayingListSize();
        playlistHandle((currentVideoIndex - 1 + playlistSize) % playlistSize);

    }

    @Override
    public void start() {
        mediaPlayer.start();
        mRemoteViews.setImageViewResource(R.id.pause_start, R.drawable.ic_pause_black_24dp);
        startForeground(notificationId, mNotificationCompatBuilder.build());
        //mNotificationManagerCompat.notify(notificationId, mNotificationCompatBuilder.build());
    }

    @Override
    public void pause() {
        mediaPlayer.pause();
        mRemoteViews.setImageViewResource(R.id.pause_start, R.drawable.ic_play_arrow_black_24dp);
        mNotificationManagerCompat.notify(notificationId, mNotificationCompatBuilder.build());
        stopForeground(false);
    }

    @Override
    public int getDuration() {
        //MediaPlayer#getDuration()はnativeメゾッドで、
        // かつ再生対象のないとき(setDataSource()前？/start()前？)のMediaPlayerに呼んだ時の挙動については定義されてないので
        //再生対象のないMediaPlayerに対してよんだ時の挙動は機種によって違う。
        return mediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        //MediaPlayer#getPositon()はnativeメゾッドで、
        // かつ再生対象のないとき(setDataSource()前？/start()前？)のMediaPlayerに呼んだ時の挙動については定義されてないので
        //再生対象のないMediaPlayerに対してよんだ時の挙動は機種によって違う。
        return mediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        mediaPlayer.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
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

    /**
     * YouTubeExtractor()を使うためのstatic innerクラス
     * そのままYouTubeExtractor()をnew して使うとメモリーリークのwarningが出るためインナークラスにする。
     */
    static private class YouTubeExtractorTask extends YouTubeExtractor {
        private YouTubeExtractorTaskCallback callback;

        interface YouTubeExtractorTaskCallback {
            void onExtractionCompleteListener(SparseArray<YtFile> ytFiles, VideoMeta videoMeta);
        }

        public YouTubeExtractorTask(Context context, YouTubeExtractorTaskCallback callback) {
            super(context);
            this.callback = callback;
        }

        @Override
        protected void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta videoMeta) {
            callback.onExtractionCompleteListener(ytFiles, videoMeta);
        }
    }

}
