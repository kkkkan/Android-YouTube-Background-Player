package com.kkkkan.youtube.tubtub;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import com.kkkkan.youtube.R;
import com.kkkkan.youtube.tubtub.BroadcastReceiver.NextReceiver;
import com.kkkkan.youtube.tubtub.BroadcastReceiver.PauseStartReceiver;
import com.kkkkan.youtube.tubtub.BroadcastReceiver.PrevReceiver;
import com.kkkkan.youtube.tubtub.database.YouTubeSqlDb;
import com.kkkkan.youtube.tubtub.model.YouTubeVideo;
import com.kkkkan.youtube.tubtub.utils.Config;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

import static android.media.MediaPlayer.MEDIA_ERROR_IO;
import static android.media.MediaPlayer.MEDIA_INFO_UNKNOWN;
import static com.kkkkan.youtube.tubtub.Settings.RepeatPlaylist.ON;

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
    private MainActivityViewModel viewModel;
    private List<YouTubeVideo> playlist;
    private int currentVideoIndex;
    private String videoUrl;
    private String videoTitle;


    public class MediaPlayerBinder extends Binder {
        public MediaPlayerService getService(MainActivityViewModel viewModel) {
            MediaPlayerService.this.viewModel = viewModel;
            return MediaPlayerService.this;
        }
    }

    public void setPlaylistSelectedCancelFlag(boolean playlistSelectedCancelFlag) {
        this.playlistSelectedCancelFlag = playlistSelectedCancelFlag;
    }

    public boolean setDisplay(SurfaceHolder holder) {
        mHolder = holder;
        try {
            mediaPlayer.setDisplay(holder);
        } catch (IllegalArgumentException e) {
            //surfaceの解放や生成の処理が前後してしまい、うまくいっておらず
            //既に解放済みのsurfaceのholderが来てしまったとき
            Log.d(TAG, "changeSurfaceHolderAndTitlebar#IllegalArgumentException\n" + e.getMessage());
            mHolder = null;
            mediaPlayer.setDisplay(null);
        }
        if (holder == null) {
            Log.d(TAG, "changeSurfaceHolderAndTitlebar#\nholder==null");
            return false;
        }
        return true;
    }

    /**
     * 今のmediaplayerの移している先を返す
     * このメゾッドの存在はあまり好ましくないがこれがないと画面が横→縦になったときに
     * 縦の画面が動かなくなってしまう
     *
     * @return
     */
    public SurfaceHolder getmHolder() {
        return mHolder;
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
                if (playlist == null) {
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
                }
                //Calling start () etc. in the idel state
                //Usually it does not happen
                //idel状態でstart()等を呼ぶと
                //通常は起きない


                //call setOnComplateListener
                //setOnComplateListener呼ぶ
                return false;
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


    private BroadcastReceiver pauseStartBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "pauseStartBroadcastReceiver");
            if (!mediaPlayer.isPlaying()) {
                mRemoteViews.setImageViewResource(R.id.pause_start, R.drawable.ic_pause_black_24dp);
                mNotificationManagerCompat.notify(notificationId, mNotificationCompatBuilder.build());
                mediaPlayer.start();
            } else {
                mRemoteViews.setImageViewResource(R.id.pause_start, R.drawable.ic_play_arrow_black_24dp);
                mNotificationManagerCompat.notify(notificationId, mNotificationCompatBuilder.build());
                mediaPlayer.pause();
            }

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

                //Videoでは形式を変えて360p（ノーマル画質）で試す。アプリを軽くするため高画質は非対応にした。これ以上落とすと音声が含まれなくなっちゃう。

                //18:Non-DASH/MP4/360p
                //134:DASH/MP4/360p


                int[] itagVideo = {18, 134};
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
                    Picasso.with(MediaPlayerService.this).load(video.getThumbnailURL()).into(mRemoteViews, R.id.video_thumbnail, 0, notification);

                    mRemoteViews.setTextViewText(R.id.title_view, videoTitle);
                    mRemoteViews.setTextViewText(R.id.video_duration, video.getDuration());
                    mRemoteViews.setImageViewResource(R.id.pause_start, R.drawable.ic_pause_black_24dp);
                    // mNotificationManagerCompat.notify(notificationId, notification);
                    startForeground(notificationId, notification);

                    Log.d(TAG, "SingletonMediaplayer.instance.getMediaPlayer().isPlaying:" + String.valueOf(mediaPlayer.isPlaying()));

                    //Progressダイアログ消すのはvideoCreate()のなかでやってる。
                    videoCreate();
                } else {
                    Log.d(TAG, "ytFile-null-next:" + video.getId());
                    viewModel.setStateError();
                    handleNextVideo();
                }
            }
        }.execute(youtubeLink);

    }

    private void videoCreate() {
        if (videoUrl == null) {
            //通常あり得ない
            Log.d(TAG, "videoCreate()-videoUrl==null");
            return;
        }
         /*ネット環境にちゃんとつながってるかチェック*/
        /*if (!networkConf.isNetworkAvailable()) {
            networkConf.createNetErrorDialog();
            return;
        }*/
        // mediaplayer関係
        // URLの先にある動画を再生する

        Uri mediaPath = Uri.parse(videoUrl);
        try {
            //新しいビデオを再生するために一度resetしてMediaPlayerをIDLE状態にする
            mediaPlayer.reset();
            Log.d(TAG, "videoCreate");
            //長いビデオだとarrows M02で途中でストリーミングが終わってしまう問題の解決のために
            //分割ストリーミングの設定
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "video/mp4"); // change content type if necessary
            headers.put("Accept-Ranges", "bytes");
            headers.put("Status", "206");
            headers.put("Cache-control", "no-cache");
            mediaPlayer.setDataSource(getApplicationContext(), mediaPath, headers);
            //SingletonMediaplayer.instance.getMediaPlayer().setDataSource(this, mediaPath);
            mediaPlayer.setDisplay(mHolder);
            //videoTitleをセット
            if (videoTitle != null) {
                viewModel.setVideoTitle(videoTitle);
            }

            //prepareに時間かかることを想定し直接startせずにLister使う
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.d(TAG, "onPrepared");
                    mp.start();
                    //読み込み中ダイアログ消す
                    viewModel.setStateStopLoading();
                }
            });
            mediaPlayer.prepareAsync();
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "videoCreate-IllegalArgumentException" + e.getMessage());
            e.printStackTrace();
        } catch (IllegalStateException e) {
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
        mediaPlayer.start();
        mRemoteViews.setImageViewResource(R.id.pause_start, R.drawable.ic_pause_black_24dp);
        mNotificationManagerCompat.notify(notificationId, mNotificationCompatBuilder.build());
    }

    @Override
    public void pause() {
        mediaPlayer.pause();
        mRemoteViews.setImageViewResource(R.id.pause_start, R.drawable.ic_play_arrow_black_24dp);
        mNotificationManagerCompat.notify(notificationId, mNotificationCompatBuilder.build());
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

}
