package com.kkkkan.youtube.tubtub;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.widget.MediaController;
import android.widget.RemoteViews;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashChunkSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.kkkkan.youtube.R;
import com.kkkkan.youtube.tubtub.BroadcastReceiver.NextReceiver;
import com.kkkkan.youtube.tubtub.BroadcastReceiver.PauseStartReceiver;
import com.kkkkan.youtube.tubtub.BroadcastReceiver.PrevReceiver;
import com.kkkkan.youtube.tubtub.database.YouTubeSqlDb;
import com.kkkkan.youtube.tubtub.model.YouTubeVideo;
import com.kkkkan.youtube.tubtub.utils.Config;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

import static com.kkkkan.youtube.tubtub.Settings.RepeatPlaylist.ON;

/**
 * Created by ka1n9 on 2017/11/24.
 */

public class MediaPlayerService extends Service implements MediaController.MediaPlayerControl {
    static final private String TAG = "MediaPlayerService";
    private final IBinder binder = new MediaPlayerBinder();
    //private final MediaPlayer mediaPlayer = new MediaPlayer();
    public RemoteViews mRemoteViews;
    public NotificationCompat.Builder mNotificationCompatBuilder;
    public NotificationManagerCompat mNotificationManagerCompat;
    static private int notificationId = 1;
    private boolean playlistSelectedCancelFlag = false;
    //private SurfaceHolder mHolder;
    private MainActivityViewModel viewModel;
    private List<YouTubeVideo> playlist;
    private int currentVideoIndex;
    private String videoUrl;
    private String videoTitle;

    //exoPlayerにはMediaPlayerのisPlaying()に当たるメゾッドがないようなので
    //代わりに今再生中か否か入れとくフラグ
    private boolean isPlaying = false;

    private ExoPlayer exoPlayer;

    public class MediaPlayerBinder extends Binder {
        public MediaPlayerService getService(MainActivityViewModel viewModel) {
            MediaPlayerService.this.viewModel = viewModel;
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
        Log.d(TAG, "setDisplay");
        //mHolder = holder;
        try {
            Log.d(TAG, "((SimpleExoPlayer) exoPlayer).setVideoSurfaceHolder(holder)");
            ((SimpleExoPlayer) exoPlayer).setVideoSurfaceHolder(holder);
            //mediaPlayer.setDisplay(holder);
        } catch (IllegalArgumentException e) {
            //surfaceの解放や生成の処理が前後してしまい、うまくいっておらず
            //既に解放済みのsurfaceのholderが来てしまったとき
            Log.d(TAG, "changeSurfaceHolderAndTitlebar#IllegalArgumentException\n" + e.getMessage());
            //mHolder = null;
            ((SimpleExoPlayer) exoPlayer).setVideoSurfaceHolder(null);
            //mediaPlayer.setDisplay(null);
        }
        Log.d(TAG, "exoPlayer.setPlayWhenReady(isPlaying) : " + String.valueOf(isPlaying));
        exoPlayer.setPlayWhenReady(isPlaying);
        if (holder == null) {
            //基本的にあり得ない
            Log.d(TAG, "changeSurfaceHolderAndTitlebar#\nholder==null");
            return false;
        }
        return true;
    }

    /**
     * 引数のsurfaceHolderと今mediaPlayerの投影先になっているsurfaceHolderが同じインスタンスだったら
     * 解放するメゾッド
     *
     * @param holder
     */
    public void releaseSurfaceHolder(SurfaceHolder holder) {
        Log.d(TAG, "releaseSurfaceHolder : " /*+ String.valueOf(holder == mHolder)*/);
        /*if (holder == mHolder) {
            mHolder = null;
            ((SimpleExoPlayer) exoPlayer).setVideoSurfaceHolder(null);
            //mediaPlayer.setDisplay(null);
            exoPlayer.setPlayWhenReady(isPlaying);
        }*/
        ((SimpleExoPlayer) exoPlayer).clearVideoSurfaceHolder(holder);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        exoPlayer = ExoPlayerFactory.newSimpleInstance(this, new DefaultTrackSelector());

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
        exoPlayer.addListener(new Player.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest) {
                //Log.d(TAG, "onTimelineChanged");
            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
                //Log.d(TAG, "onTracksChanged");
            }

            @Override
            public void onLoadingChanged(boolean isLoading) {
                //Log.d(TAG, "onLoadingChanged");
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                Log.d(TAG, "onPlayerStateChanged: playWhenReady is " + String.valueOf(playWhenReady) + " playbackState is " + String.valueOf(playbackState));
                switch (playbackState) {
                    case Player.STATE_READY:
                        //読み込み中ダイアログ消す
                        viewModel.setStateStopLoading();
                        isPlaying = playWhenReady;
                        break;
                    case Player.STATE_ENDED:
                        if (playlist == null) {
                            //Originally it should not be playlist == null,
                            // but because there was something that was fallen by null reference with playlist.size ()
                            //本来ならplaylist==nullとなることは無いはずだが、
                            // playlist.size()でnull参照で落ちたことがあったので対策
                            Log.d(TAG, "\nplaylist is null!\n");
                            return;
                        }
                        handleNextVideo();
                        break;
                }
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {
                //Log.d(TAG, "onRepeatModeChanged");
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Log.d(TAG, "onPlayerError : " + String.valueOf(error.type));
                switch (error.type) {
                    case ExoPlaybackException.TYPE_SOURCE:
                        Log.d(TAG, "TYPE_SOURCE: " + error.getSourceException().getMessage());
                        break;

                    case ExoPlaybackException.TYPE_RENDERER:
                        Exception exception = error.getRendererException();
                        Log.d(TAG, "TYPE_RENDERER: " + error.getRendererException().getMessage());
                        break;

                    case ExoPlaybackException.TYPE_UNEXPECTED:
                        //surfaceViewの解放タイミングが遅かった？時
                        Log.d(TAG, "TYPE_UNEXPECTED: " + error.getUnexpectedException().getMessage());
                        break;
                }

                ExtractorMediaSource mediaSource = makeExtractorMediaSource();
                if (mediaSource == null) {
                    Log.d(TAG, "onPlayerError : mediaSource == null");
                    //通常あり得ない
                    return;
                }
                Log.d(TAG, "onPlayerError : exoPlayer.prepare(mediaSource, false, true)");
                exoPlayer.prepare(mediaSource, false, true);
                //読み込み中ダイアログ消す
                viewModel.setStateStopLoading();
                Log.d(TAG, "onPlayerError : exoPlayer.setPlayWhenReady(isPlaying)");
                exoPlayer.setPlayWhenReady(isPlaying);
            }

            @Override
            public void onPositionDiscontinuity() {
                //Log.d(TAG, "onPositionDiscontinuity()");
            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
                //Log.d(TAG, "onPlaybackParametersChanged");
            }
        });

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
            Log.d(TAG, "pauseStartBroadcastReceiver: " + String.valueOf(exoPlayer.getPlaybackState()));
            if (!isPlaying) {
                mRemoteViews.setImageViewResource(R.id.pause_start, R.drawable.ic_pause_black_24dp);
                startForeground(notificationId, mNotificationCompatBuilder.build());
            } else {
                mRemoteViews.setImageViewResource(R.id.pause_start, R.drawable.ic_play_arrow_black_24dp);
                mNotificationManagerCompat.notify(notificationId, mNotificationCompatBuilder.build());
                stopForeground(false);
            }
            exoPlayer.setPlayWhenReady(!isPlaying);
        }
    };


    private BroadcastReceiver prevBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "prevBroadcastReceiver");
            onPlaylistSelected(playlist, (currentVideoIndex - 1 + playlist.size()) % playlist.size());
        }

    };

    private BroadcastReceiver nextBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "nextStartBroadcastReceiver");
            onPlaylistSelected(playlist, (currentVideoIndex + 1) % playlist.size());
        }
    };


    /**
     * 選んだビデオをmediaplayerで再生するためのurlをvideoUrlにセットし、バックグラウンド再生かフォアグランド再生かによって
     * 次の制御をvideoCreate()に振る
     */
    public void onPlaylistSelected(List<YouTubeVideo> playlist, final int position) {
        Log.d(TAG, "onPlaylistSelected");
        if (playlistSelectedCancelFlag) {
            //もしユーザーがビデオ読み込みやめるよう操作していたら
            //Flagをfalseにする
            playlistSelectedCancelFlag = false;
            //一応Loading…出てたら消す
            viewModel.setStateStopLoading();
            return;
        }
        //読み込み中ダイアログ表示
        viewModel.setStateStartLoading();
        this.playlist = playlist;
        currentVideoIndex = position;

        //ネット環境にちゃんとつながってるかチェック
        /*if (!networkConf.isNetworkAvailable()) {
            networkConf.createNetErrorDialog();
            return;
        }
*/
        final YouTubeVideo video = playlist.get(position);

        String youtubeLink = Config.YOUTUBE_BASE_URL + video.getId();
        new YouTubeExtractor(this) {
            @Override
            protected void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta videoMeta) {
                Log.d(TAG, "onExtractionComplete");
                if (ytFiles == null) {
                    viewModel.setStateError();
                    handleNextVideo();
                    return;
                }
                //設定によって画質変える
                SharedPreferences sharedPreferences = getSharedPreferences(VideoQualitys.VideoQualityPreferenceFileName, Context.MODE_PRIVATE);
                //デフォルトの画質はノーマル
                int videoQualitySetting = sharedPreferences.getInt(VideoQualitys.VideoQualityPreferenceKey, VideoQualitys.videoQualityNormal);
                Integer[] itagVideo = VideoQualitys.getVideoQualityTagsMap().get(videoQualitySetting);

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
                    YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).create(video);

                    String videoDownloadUrl = ytFiles.get(tagVideo).getUrl();
                    Log.d(TAG, "VideoURL:" + videoDownloadUrl);

                    //ポーズから戻ったときのためMovieUrlも変えとく
                    videoUrl = videoDownloadUrl;
                    videoTitle = video.getTitle();

                    //サムネイルの設定
                    Notification notification = mNotificationCompatBuilder.build();
                    Picasso.with(MediaPlayerService.this).load(video.getThumbnailURL()).into(mRemoteViews, R.id.video_thumbnail, notificationId, notification);

                    mRemoteViews.setTextViewText(R.id.title_view, videoTitle);
                    mRemoteViews.setTextViewText(R.id.video_duration, video.getDuration());
                    mRemoteViews.setImageViewResource(R.id.pause_start, R.drawable.ic_pause_black_24dp);
                    // mNotificationManagerCompat.notify(notificationId, notification);
                    startForeground(notificationId, notification);

                    // Log.d(TAG, "SingletonMediaplayer.instance.getMediaPlayer().isPlaying:" + String.valueOf(mediaPlayer.isPlaying()));

                    //Progressダイアログ消すのはvideoCreate()のなかでやってる。
                    videoCreate(tagVideo);
                } else {
                    Log.d(TAG, "ytFile-null-next:" + video.getId());
                    viewModel.setStateError();
                    handleNextVideo();
                }
            }
        }.execute(youtubeLink);

    }

    private void videoCreate(int tag) {
        Log.d(TAG, "videoCreate()");
        MediaSource mediaSource = null;
        for (int itag : VideoQualitys.dashTags) {
            if (tag == itag) {
                mediaSource = makeDashMediaSource();
            }
        }
        for (int itag : VideoQualitys.hlsTags) {
            if (tag == itag) {
                mediaSource = makeHlsMediaSource();
            }
        }

        if (mediaSource == null) {
            //通常あり得ない
            return;
        }
        exoPlayer.prepare(mediaSource);
        //mediaPlayer.setDataSource(this, mediaPath);
        //((SimpleExoPlayer) exoPlayer).setVideoSurfaceHolder(mHolder);
        //mediaPlayer.setDisplay(mHolder);
        //videoTitleをセット
        if (videoTitle != null) {
            viewModel.setVideoTitle(videoTitle);
        }
        exoPlayer.setPlayWhenReady(true);
    }

    @Nullable
    private ExtractorMediaSource makeExtractorMediaSource() {
        if (videoUrl == null) {
            //通常あり得ない
            Log.d(TAG, "videoUrl==null");
            return null;
        }

        // mediaplayer関係
        // URLの先にある動画を再生する

        Uri mediaPath = Uri.parse(videoUrl);
        //新しいビデオを再生するために一度resetしてMediaPlayerをIDLE状態にする
        //mediaPlayer.reset();
        Log.d(TAG, "makeExtractorMediaSource()");

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, getPackageName()));
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

        // 任意のイベントリスナー
        Handler handler = new Handler();
        ExtractorMediaSource.EventListener eventListener = new ExtractorMediaSource.EventListener() {
            @Override
            public void onLoadError(IOException error) {
                Log.d(TAG, "ExtractorMediaSource.EventListener : onLoadError");
                error.printStackTrace();
            }
        };

        return new ExtractorMediaSource(mediaPath, dataSourceFactory, extractorsFactory, handler, eventListener);
    }

    @Nullable
    private DashMediaSource makeDashMediaSource() {
        if (videoUrl == null) {
            //通常あり得ない
            Log.d(TAG, "videoUrl==null");
            return null;
        }

        // mediaplayer関係
        // URLの先にある動画を再生する

        Uri mediaPath = Uri.parse(videoUrl);
        //新しいビデオを再生するために一度resetしてMediaPlayerをIDLE状態にする
        //mediaPlayer.reset();
        Log.d(TAG, "makeExtractorMediaSource()");

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, getPackageName()));
        DashChunkSource.Factory dashChunkSourceFactory = new DefaultDashChunkSource.Factory(dataSourceFactory);

        // 任意のイベントリスナー
        Handler handler = new Handler();
        AdaptiveMediaSourceEventListener eventListener = new AdaptiveMediaSourceEventListener() {

            @Override
            public void onLoadStarted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs) {
                Log.d(TAG, "onLoadStarted: ");
            }

            @Override
            public void onLoadCompleted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
                Log.d(TAG, "onLoadCompleted: ");
            }

            @Override
            public void onLoadCanceled(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
                Log.d(TAG, "onLoadCanceled: ");
            }

            @Override
            public void onLoadError(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded, IOException error, boolean wasCanceled) {
                Log.d(TAG, "onLoadError: ");
            }

            @Override
            public void onUpstreamDiscarded(int trackType, long mediaStartTimeMs, long mediaEndTimeMs) {
                Log.d(TAG, "onUpstreamDiscarded: ");
            }

            @Override
            public void onDownstreamFormatChanged(int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaTimeMs) {
                Log.d(TAG, "onDownstreamFormatChanged: ");
            }
        };

        return new DashMediaSource(mediaPath, dataSourceFactory, dashChunkSourceFactory, handler, eventListener);

    }

    @Nullable
    private HlsMediaSource makeHlsMediaSource() {
        if (videoUrl == null) {
            //通常あり得ない
            Log.d(TAG, "videoUrl==null");
            return null;
        }

        // mediaplayer関係
        // URLの先にある動画を再生する

        Uri mediaPath = Uri.parse(videoUrl);
        //新しいビデオを再生するために一度resetしてMediaPlayerをIDLE状態にする
        //mediaPlayer.reset();
        Log.d(TAG, "makeExtractorMediaSource()");

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, getPackageName()));

        // 任意のイベントリスナー
        Handler handler = new Handler();
        AdaptiveMediaSourceEventListener eventListener = new AdaptiveMediaSourceEventListener() {
            @Override
            public void onLoadStarted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs) {
                Log.d(TAG, "onLoadStarted: ");
            }

            @Override
            public void onLoadCompleted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
                Log.d(TAG, "onLoadCompleted: ");
            }

            @Override
            public void onLoadCanceled(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
                Log.d(TAG, "onLoadCanceled: ");
            }

            @Override
            public void onLoadError(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded, IOException error, boolean wasCanceled) {
                Log.d(TAG, "onLoadError: ");
            }

            @Override
            public void onUpstreamDiscarded(int trackType, long mediaStartTimeMs, long mediaEndTimeMs) {
                Log.d(TAG, "onUpstreamDiscarded: ");
            }

            @Override
            public void onDownstreamFormatChanged(int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaTimeMs) {
                Log.d(TAG, "onDownstreamFormatChanged: ");
            }
        };

        return new HlsMediaSource(mediaPath, dataSourceFactory, handler, eventListener);

    }

    /**
     * 次のビデオを再生するとなったときに呼ぶメゾッド。
     * 設定によって同じビデオを再生するか、プレイリストの最後に達していた時に次のビデオに行くかなどを
     * ハンドリングする
     */
    private void handleNextVideo() {
        Log.d(TAG, "handleNextVideo");
        Settings settings = Settings.getInstance();
        if (settings.getRepeatOne() == Settings.RepeatOne.ON) {
            // one song repeat
            //1曲リピート時
            onPlaylistSelected(playlist, currentVideoIndex);
        } else if (settings.getRepeatPlaylist() == ON) {
            // It is not a repeat of one song, and at play list repeat
            //1曲リピートではなく、かつプレイリストリピート時
            onPlaylistSelected(playlist, (currentVideoIndex + 1) % playlist.size());
        } else {
            // When it is neither a single song repeat nor a play list repeat
            //一曲リピートでもプレイリストリピートでもないとき
            if (currentVideoIndex + 1 < playlist.size()) {
                onPlaylistSelected(playlist, currentVideoIndex + 1);
            } else {
                //最後の曲のときはprogressbarが出ていたらそれを消すだけ。
                viewModel.setStateStopLoading();
            }
        }
    }

    public void nextPlay() {
        if (playlist != null) {
            //When playlist is not set, you can also press it so that it will not fall at that time
            //playlistセットされてないときも押せてしまうからその時落ちないように
            onPlaylistSelected(playlist, (currentVideoIndex + 1) % playlist.size());
        }
    }

    public void prevPlay() {
        //When playlist is not set, you can also press it so that it will not fall at that time
        //playlistセットされてないときも押せてしまうからその時落ちないように
        if (playlist != null) {
            onPlaylistSelected(playlist, (currentVideoIndex - 1 + playlist.size()) % playlist.size());
        }
    }

    @Override
    public void start() {
        //mediaPlayer.start();
        exoPlayer.setPlayWhenReady(true);
        mRemoteViews.setImageViewResource(R.id.pause_start, R.drawable.ic_pause_black_24dp);
        startForeground(notificationId, mNotificationCompatBuilder.build());
        //mNotificationManagerCompat.notify(notificationId, mNotificationCompatBuilder.build());
    }

    @Override
    public void pause() {
        //mediaPlayer.pause();
        exoPlayer.setPlayWhenReady(false);
        mRemoteViews.setImageViewResource(R.id.pause_start, R.drawable.ic_play_arrow_black_24dp);
        mNotificationManagerCompat.notify(notificationId, mNotificationCompatBuilder.build());
        stopForeground(false);
    }

    @Override
    public int getDuration() {
        //MediaPlayer#getDuration()はnativeメゾッドで、
        // かつ再生対象のないとき(setDataSource()前？/start()前？)のMediaPlayerに呼んだ時の挙動については定義されてないので
        //再生対象のないMediaPlayerに対してよんだ時の挙動は機種によって違う。
        return (int) exoPlayer.getDuration();
        //mediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        //MediaPlayer#getPositon()はnativeメゾッドで、
        // かつ再生対象のないとき(setDataSource()前？/start()前？)のMediaPlayerに呼んだ時の挙動については定義されてないので
        //再生対象のないMediaPlayerに対してよんだ時の挙動は機種によって違う。
        return (int) exoPlayer.getCurrentPosition();
        //mediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        exoPlayer.seekTo(pos);
        //mediaPlayer.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return isPlaying;
        //mediaPlayer.isPlaying();
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

}
