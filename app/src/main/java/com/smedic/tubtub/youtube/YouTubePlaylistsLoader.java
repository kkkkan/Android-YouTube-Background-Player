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


    private static final String TAG = "SMEDIC";
    private static final String TAG_NAME="kandabashi";
    /*googleのYouTubeAPIの登場*/
    private YouTube youtube = getYouTubeWithCredentials();

    public YouTubePlaylistsLoader(Context context) {
        super(context);
    }


    @Override
    public List<YouTubePlaylist> loadInBackground() {

        Log.d(TAG_NAME,"playlistLoading");
        /*アカウントが設定されてなかったら*/
        if (getCredential().getSelectedAccountName() == null) {
            Log.d(TAG, "loadInBackground: account not picked!");
            return Collections.emptyList();
        }

        try {
            /*setMine→アカウントに登録されたチャンネルだけ返す。*/
            ChannelListResponse channelListResponse = youtube.channels().list("snippet").setMine(true).execute();

            List<Channel> channelList = channelListResponse.getItems();
            if (channelList.isEmpty()) {
                Log.d(TAG, "Can't find user channel");
            }
            Channel channel = channelList.get(0);

            YouTube.Playlists.List searchList = youtube.playlists().list("id,snippet,contentDetails,status");//.setKey("AIzaSyApidIQCEBbqishTDtwuNky9uA-wyqZlR0");

            searchList.setChannelId(channel.getId());
            searchList.setFields("items(id,snippet/title,snippet/thumbnails/default/url,contentDetails/itemCount,status)");
            searchList.setMaxResults(Config.NUMBER_OF_VIDEOS_RETURNED);

            PlaylistListResponse playListResponse = searchList.execute();

            List<Playlist> playlists = playListResponse.getItems();


            if (playlists != null) {

                Iterator<Playlist> iteratorPlaylistResults = playlists.iterator();
                if (!iteratorPlaylistResults.hasNext()) {
                    Log.d(TAG, " There aren't any results for your query.");
                }

                ArrayList<YouTubePlaylist> youTubePlaylistList = new ArrayList<>();

                while (iteratorPlaylistResults.hasNext()) {
                    Playlist playlist = iteratorPlaylistResults.next();
                    String id=playlist.getId();
                    Log.d(TAG_NAME,"YouTubePlaylistLoader-"+id);

                    YouTubePlaylist playlistItem = new YouTubePlaylist(playlist.getSnippet().getTitle(),
                            playlist.getSnippet().getThumbnails().getDefault().getUrl(),
                            playlist.getId(),
                            playlist.getContentDetails().getItemCount(),
                            playlist.getStatus().getPrivacyStatus());
                    youTubePlaylistList.add(playlistItem);
                }
                Log.d(TAG_NAME,"YouTubePlaylistLoader-11");
                return youTubePlaylistList;
            }
        }catch (UserRecoverableAuthIOException e) {
            Log.d(TAG_NAME,"YouTubePlaylistLoader-"+e.toString());
            Log.d(TAG, "loadInBackground: exception REQUEST_AUTHORIZATION");
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(new MainActivity().getMainContext(),"もう一度ログインしなおしてください。",Toast.LENGTH_LONG).show();
                }
            });
            cancelLoad();
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG_NAME,"YouTubePlaylistLoadrer-error");
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
