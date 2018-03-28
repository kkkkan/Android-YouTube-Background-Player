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
import com.google.api.services.youtube.model.ChannelContentDetails;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.kkkkan.youtube.tubtub.model.YouTubePlaylist;
import com.kkkkan.youtube.tubtub.utils.Config;
import com.kkkkan.youtube.tubtub.utils.NetworkConf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.kkkkan.youtube.tubtub.MainActivity.mainHandler;
import static com.kkkkan.youtube.tubtub.utils.Utils.putPlaylistsToArrayList;
import static com.kkkkan.youtube.tubtub.youtube.YouTubeSingleton.getCredential;
import static com.kkkkan.youtube.tubtub.youtube.YouTubeSingleton.getYouTubeWithCredentials;


/**
 * アカウントに紐づけられているplaylistをサーバーから全てとってきて返します。
 * 並び替えなどはしていないので、毎回同じ規則に並び替えて使いたい場合は使う側でソートしてください。
 * <p>
 * Created by smedic on 13.2.17..
 */

public class YouTubePlaylistsLoader extends AsyncTaskLoader<List<YouTubePlaylist>> {

    private static final String TAG = "ouTubePlaylistsLoader";
    private YouTube youtube = getYouTubeWithCredentials();
    private Context context;

    public YouTubePlaylistsLoader(Context context) {
        super(context);
        this.context = context;
    }


    @Override
    public List<YouTubePlaylist> loadInBackground() {

        Log.d(TAG, "playlistLoading");
        //If the count is not set or it is not connected to the net
        //カウントが設定されてないorネットにつながっていなかったら
        if (getCredential().getSelectedAccountName() == null || !NetworkConf.isNetworkAvailable(getContext())) {
            Log.d(TAG, "loadInBackground: account not picked!");
            return Collections.emptyList();
        }
        Log.d(TAG, "playlistLoading");
        try {
            //setMine → Return only channels registered in account
            //setMine→アカウントに登録されたチャンネルだけ返す。
            ChannelListResponse channelListResponse = youtube.channels().list("snippet,contentDetails").setMine(true).execute();

            List<Channel> channelList = channelListResponse.getItems();
            if (channelList.isEmpty()) {
                Log.d(TAG, "Can't find user channel");
                return Collections.emptyList();
            }
            Log.d(TAG, "channelList number is : " + String.valueOf(channelList.size()));
            Channel channel = channelList.get(0);
            ChannelContentDetails.RelatedPlaylists relatedPlaylists = channel.getContentDetails().getRelatedPlaylists();

            //お気に入りリストなどのIDを取得
            //高く評価した動画リスト
            String likesId = relatedPlaylists.getLikes();
            //お気に入りリスト->お気に入りリストは普通に取ってこれているらしい？
            //String favoritesId = relatedPlaylists.getFavorites();
            //後で見るリスト
            String watchLaterId = relatedPlaylists.getWatchLater();


            //自分で作った再生リストの詳細を取ってくる
            YouTube.Playlists.List searchList = youtube.playlists().list("id,snippet,contentDetails,status");//.setKey("AIzaSyApidIQCEBbqishTDtwuNky9uA-wyqZlR0");
            searchList.setChannelId(channel.getId());
            searchList.setFields("items(id,snippet/title,snippet/thumbnails/default/url,contentDetails/itemCount,status)");
            searchList.setMaxResults(Config.NUMBER_OF_VIDEOS_RETURNED);

            PlaylistListResponse playListResponse = searchList.execute();

            List<Playlist> playlists = playListResponse.getItems();

            ArrayList<YouTubePlaylist> youTubePlaylistList = new ArrayList<>();
            if (playlists != null) {
                //自分で作った再生リストを返り値用のArrayListに入れる
                putPlaylistsToArrayList(youTubePlaylistList, playlists);
            }
            Log.d(TAG, "my list put success");
            //お気に入りリストなどの詳細を取ってくる
            searchList = youtube.playlists().list("id,snippet,contentDetails,status");//.setKey("AIzaSyApidIQCEBbqishTDtwuNky9uA-wyqZlR0");
            searchList.setId(likesId + "," /*+ favoritesId */ + "," + watchLaterId);
            //searchList.setMine(true);
            searchList.setFields("items(id,snippet/title,snippet/thumbnails/default/url,contentDetails/itemCount,status)");
            searchList.setMaxResults(Config.NUMBER_OF_VIDEOS_RETURNED);

            playListResponse = searchList.execute();
            playlists = playListResponse.getItems();

            if (playlists != null) {
                //お気に入りリストなどを返り値用のArrayListに入れる
                putPlaylistsToArrayList(youTubePlaylistList, playlists);
            }
            //とりあえず並び替えなどはせず返す
            return youTubePlaylistList;
        } catch (UserRecoverableAuthIOException e) {
            Log.d(TAG, "YouTubePlaylistLoader-" + e.toString());
            Log.d(TAG, "loadInBackground: exception REQUEST_AUTHORIZATION");
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "もう一度ログインしなおしてください。", Toast.LENGTH_LONG).show();
                }
            });
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "YouTubePlaylistLoadrer-error");
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
