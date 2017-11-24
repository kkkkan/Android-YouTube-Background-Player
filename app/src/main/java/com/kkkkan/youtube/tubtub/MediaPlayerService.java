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

import static com.kkkkan.youtube.tubtub.Settings.RepeatPlaylist.ON;

/**
 * Created by ka1n9 on 2017/11/24.
 */

public class MediaPlayerService extends Service {
    static final private String TAG = "MediaPlayerService";
    private final IBinder binder = new MediaPlayerBinder();
    private final MediaPlayer mediaPlayer = new MediaPlayer();
    public RemoteViews mRemoteViews;
    public NotificationCompat.Builder mNotificationCompatBuilder;
    public NotificationManagerCompat mNotificationManagerCompat;
    static private int notificationId = 0;
    private boolean playlistSelectedCancelFlag = false;
    private SurfaceHolder mHolder;


    public class MediaPlayerBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
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
            if (!SingletonMediaPlayer.instance.getMediaPlayer().isPlaying()) {
                mRemoteViews.setImageViewResource(R.id.pause_start, R.drawable.ic_pause_black_24dp);
                mNotificationManagerCompat.notify(notificationId, mNotificationCompatBuilder.build());
                SingletonMediaPlayer.instance.getMediaPlayer().start();
            } else {
                mRemoteViews.setImageViewResource(R.id.pause_start, R.drawable.ic_play_arrow_black_24dp);
                mNotificationManagerCompat.notify(notificationId, mNotificationCompatBuilder.build());
                SingletonMediaPlayer.instance.getMediaPlayer().pause();
            }


        }
    };


    private BroadcastReceiver prevBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "prevBroadcastReceiver");
            onPlaylistSelected(SingletonMediaPlayer.instance.playlist, (SingletonMediaPlayer.instance.currentVideoIndex - 1 + SingletonMediaPlayer.instance.playlist.size()) % SingletonMediaPlayer.instance.playlist.size());
        }

    };

    private BroadcastReceiver nextBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "nextStartBroadcastReceiver");
            onPlaylistSelected(SingletonMediaPlayer.instance.playlist, (SingletonMediaPlayer.instance.currentVideoIndex + 1) % SingletonMediaPlayer.instance.playlist.size());
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
            //setProgressDialogDismiss();
            return;
        }
        //読み込み中ダイアログ表示
        // setProgressDialogShow();
        SingletonMediaPlayer.instance.playlist = playlist;
        SingletonMediaPlayer.instance.currentVideoIndex = position;

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
                    //Toast.makeText(mainContext, "ビデオが読み込めませんでした。次のビデオを再生します。", Toast.LENGTH_SHORT).show();
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
                    SingletonMediaPlayer.instance.videoUrl = videoDownloadUrl;
                    SingletonMediaPlayer.instance.VideoTitle = video.getTitle();

                    //サムネイルの設定
                    Notification notification = mNotificationCompatBuilder.build();
                    Picasso.with(MediaPlayerService.this).load(video.getThumbnailURL()).into(mRemoteViews, R.id.video_thumbnail, 0, notification);

                    mRemoteViews.setTextViewText(R.id.title_view, SingletonMediaPlayer.instance.VideoTitle);
                    mRemoteViews.setTextViewText(R.id.video_duration, video.getDuration());
                    mRemoteViews.setImageViewResource(R.id.pause_start, R.drawable.ic_pause_black_24dp);
                    mNotificationManagerCompat.notify(notificationId, notification);

                    Log.d(TAG, "SingletonMediaplayer.instance.getMediaPlayer().isPlaying:" + String.valueOf(SingletonMediaPlayer.instance.getMediaPlayer().isPlaying()));

                    //Progressダイアログ消すのはvideoCreate()のなかでやってる。
                    videoCreate();
                } else {
                    Log.d(TAG, "ytFile-null-next:" + video.getId());
                    //Toast.makeText(mainContext, "ビデオが読み込めませんでした。\n次のビデオを再生します。", Toast.LENGTH_SHORT).show();
                    handleNextVideo();
                }
            }
        }.execute(youtubeLink);

    }

    private void videoCreate() {
        if (SingletonMediaPlayer.instance.videoUrl == null) {
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

        Uri mediaPath = Uri.parse(SingletonMediaPlayer.instance.videoUrl);
        try {
            //新しいビデオを再生するために一度resetしてMediaPlayerをIDLE状態にする
            SingletonMediaPlayer.instance.getMediaPlayer().reset();
            Log.d(TAG, "videoCreate");
            //長いビデオだとarrows M02で途中でストリーミングが終わってしまう問題の解決のために
            //分割ストリーミングの設定
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "video/mp4"); // change content type if necessary
            headers.put("Accept-Ranges", "bytes");
            headers.put("Status", "206");
            headers.put("Cache-control", "no-cache");
            SingletonMediaPlayer.instance.getMediaPlayer().setDataSource(getApplicationContext(), mediaPath, headers);
            //SingletonMediaplayer.instance.getMediaPlayer().setDataSource(this, mediaPath);
            SingletonMediaPlayer.instance.getMediaPlayer().setDisplay(mHolder);
            //videoTitleをセット
            if (SingletonMediaPlayer.instance.VideoTitle != null) {
                mTextView.setText(SingletonMediaPlayer.instance.VideoTitle);
            }

            //prepareに時間かかることを想定し直接startせずにLister使う
            SingletonMediaPlayer.instance.getMediaPlayer().setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.d(TAG, "onPrepared");
                    mp.start();
                    //読み込み中ダイアログ消す
                    // setProgressDialogDismiss();
                }
            });
            SingletonMediaPlayer.instance.getMediaPlayer().prepareAsync();
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
            onPlaylistSelected(SingletonMediaPlayer.instance.playlist, SingletonMediaPlayer.instance.currentVideoIndex);
        } else if (settings.getRepeatPlaylist() == ON) {
            // It is not a repeat of one song, and at play list repeat
            //1曲リピートではなく、かつプレイリストリピート時
            onPlaylistSelected(SingletonMediaPlayer.instance.playlist, (SingletonMediaPlayer.instance.currentVideoIndex + 1) % SingletonMediaPlayer.instance.playlist.size());
        } else {
            // When it is neither a single song repeat nor a play list repeat
            //一曲リピートでもプレイリストリピートでもないとき
            if (SingletonMediaPlayer.instance.currentVideoIndex + 1 < SingletonMediaPlayer.instance.playlist.size()) {
                onPlaylistSelected(SingletonMediaPlayer.instance.playlist, SingletonMediaPlayer.instance.currentVideoIndex + 1);
            } else {
                //最後の曲のときはprogressbarが出ていたらそれを消すだけ。
                //setProgressDialogDismiss();
            }
        }
    }
}
