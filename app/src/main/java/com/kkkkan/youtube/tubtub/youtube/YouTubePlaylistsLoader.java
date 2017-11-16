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

package com.kkkkan.youtube.tubtub.youtube;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.kkkkan.youtube.tubtub.MainActivity;
import com.kkkkan.youtube.tubtub.model.YouTubePlaylist;
import com.kkkkan.youtube.tubtub.utils.Config;
import com.kkkkan.youtube.tubtub.utils.NetworkConf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.kkkkan.youtube.tubtub.MainActivity.mainHandler;
import static com.kkkkan.youtube.tubtub.youtube.YouTubeSingleton.getCredential;
import static com.kkkkan.youtube.tubtub.youtube.YouTubeSingleton.getYouTubeWithCredentials;


/**
 * Created by smedic on 13.2.17..
 */

public class YouTubePlaylistsLoader extends AsyncTaskLoader<List<YouTubePlaylist>> {


    private static final String TAG = "SMEDIC";
    private static final String TAG_NAME = "ouTubePlaylistsLoader";
    private YouTube youtube = getYouTubeWithCredentials();

    public YouTubePlaylistsLoader(Context context) {
        super(context);
    }


    @Override
    public List<YouTubePlaylist> loadInBackground() {

        Log.d(TAG_NAME, "playlistLoading");
        //If the count is not set or it is not connected to the net
        //カウントが設定されてないorネットにつながっていなかったら
        if (getCredential().getSelectedAccountName() == null || !NetworkConf.isNetworkAvailable(getContext())) {
            Log.d(TAG, "loadInBackground: account not picked!");
            return Collections.emptyList();
        }
        Log.d(TAG_NAME, "playlistLoading");
        try {
            //setMine → Return only channels registered in account
            //setMine→アカウントに登録されたチャンネルだけ返す。
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
                    String id = playlist.getId();
                    Log.d(TAG_NAME, "YouTubePlaylistLoader-" + id);

                    YouTubePlaylist playlistItem = new YouTubePlaylist(playlist.getSnippet().getTitle(),
                            playlist.getSnippet().getThumbnails().getDefault().getUrl(),
                            playlist.getId(),
                            playlist.getContentDetails().getItemCount(),
                            playlist.getStatus().getPrivacyStatus());
                    youTubePlaylistList.add(playlistItem);
                }
                Log.d(TAG_NAME, "YouTubePlaylistLoader-11");
                return youTubePlaylistList;
            }
        } catch (UserRecoverableAuthIOException e) {
            Log.d(TAG_NAME, "YouTubePlaylistLoader-" + e.toString());
            Log.d(TAG, "loadInBackground: exception REQUEST_AUTHORIZATION");
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(new MainActivity().getMainContext(), "もう一度ログインしなおしてください。", Toast.LENGTH_LONG).show();
                }
            });
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG_NAME, "YouTubePlaylistLoadrer-error");
            Log.d(TAG, "loadInBackground: " + e.getMessage());
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
