package com.smedic.tubtub.youtube;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.smedic.tubtub.MainActivity;
import com.smedic.tubtub.googleAuthIO;
import com.smedic.tubtub.model.YouTubePlaylist;
import com.smedic.tubtub.utils.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.smedic.tubtub.MainActivity.mainHandler;
import static com.smedic.tubtub.youtube.YouTubeSingleton.getCredential;
import static com.smedic.tubtub.youtube.YouTubeSingleton.getYouTubeWithCredentials;

/**
 * Created by smedic on 13.2.17..
 */

public class YouTubePlaylistsLoader extends AsyncTaskLoader<List<YouTubePlaylist>> {
    private  int UserRecoverableAuthIOException=100;

    private static final String TAG = "SMEDIC";
    /*googleのYouTubeAPIの登場*/
    private YouTube youtube = getYouTubeWithCredentials();

    public YouTubePlaylistsLoader(Context context) {
        super(context);
    }


    @Override
    public List<YouTubePlaylist> loadInBackground() {

        Log.d("kandabshi","playlistLoading");
        /*アカウントが設定されてなかったら？*/
        if (getCredential().getSelectedAccountName() == null) {
            Log.d(TAG, "loadInBackground: account not picked!");
            return Collections.emptyList();
        }

        try {
            Log.d("kandabashi","YouTubePlaylistLoader-1");
            Log.d("kandabashi","YouTubePlaylistLoader-2");
            /*setMineまでは大丈夫。execute()で例外発生->execute()が実行命令だから当たり前。*/
            /*setmine→アカウントに登録されたチャンネルだけ返す。*/
            ChannelListResponse channelListResponse = youtube.channels().list("snippet").setMine(true).execute();

            List<Channel> channelList = channelListResponse.getItems();
            if (channelList.isEmpty()) {
                Log.d(TAG, "Can't find user channel");
            }
            Log.d("kandabashi","YouTubePlaylistLoader-3");
            Channel channel = channelList.get(0);

            Log.d("kandabashi","YouTubePlaylistLoader-4");
            YouTube.Playlists.List searchList = youtube.playlists().list("id,snippet,contentDetails,status");//.setKey("AIzaSyApidIQCEBbqishTDtwuNky9uA-wyqZlR0");

            Log.d("kandabshi","YouTubePlaylistLoader-5");
            searchList.setChannelId(channel.getId());
            searchList.setFields("items(id,snippet/title,snippet/thumbnails/default/url,contentDetails/itemCount,status)");
            searchList.setMaxResults(Config.NUMBER_OF_VIDEOS_RETURNED);
            Log.d("kandabshi","YouTubePlaylistLoader-6");
            PlaylistListResponse playListResponse = searchList.execute();
            Log.d("kandabshi","YouTubePlaylistLoader-7");
            List<Playlist> playlists = playListResponse.getItems();
            Log.d("kandabshi","YouTubePlaylistLoader-8");

            if (playlists != null) {

                Iterator<Playlist> iteratorPlaylistResults = playlists.iterator();
                if (!iteratorPlaylistResults.hasNext()) {
                    Log.d(TAG, " There aren't any results for your query.");
                }
                Log.d("kandabshi","YouTubePlaylistLoader-9");
                ArrayList<YouTubePlaylist> youTubePlaylistList = new ArrayList<>();
                Log.d("kandabshi","YouTubePlaylistLoader-10");
                while (iteratorPlaylistResults.hasNext()) {
                    Playlist playlist = iteratorPlaylistResults.next();
                    String id=playlist.getId();
                    Log.d("kandabshi","YouTubePlaylistLoader-"+id);

                    YouTubePlaylist playlistItem = new YouTubePlaylist(playlist.getSnippet().getTitle(),
                            playlist.getSnippet().getThumbnails().getDefault().getUrl(),
                            playlist.getId(),
                            playlist.getContentDetails().getItemCount(),
                            playlist.getStatus().getPrivacyStatus());
                    youTubePlaylistList.add(playlistItem);
                }
                Log.d("kandabshi","YouTubePlaylistLoader-11");
                return youTubePlaylistList;
            }
        }catch (UserRecoverableAuthIOException e) {
            Log.d("kandabashi","YouTubePlaylistLoader-"+e.toString());
            Log.d(TAG, "loadInBackground: exception REQUEST_AUTHORIZATION");
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(new MainActivity().mainContext,"もう一度ログインしなおしてください。",Toast.LENGTH_LONG).show();
                }
            });
            cancelLoad();
            e.printStackTrace();
        } catch (IOException e) {
            Log.d("kandabshi","YouTubePlaylistLoadrer-error");
            Log.d(TAG, "loadInBackground: " + e.getMessage());
            cancelLoad();
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    @Override
    public void deliverResult(List<YouTubePlaylist> data) {
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            return;
        }
        super.deliverResult(data);
    }

    @Override
    public void onCanceled(List<YouTubePlaylist> data) {
        super.onCanceled(data);
        Log.d(TAG, "onCanceled: ");
    }
}
