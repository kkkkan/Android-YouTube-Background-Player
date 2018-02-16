package com.kkkkan.youtube.tubtub.adapters;

/**
 * Created by smedic on 6.2.17..
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

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kkkkan.youtube.R;
import com.kkkkan.youtube.tubtub.interfaces.ItemEventsListener;
import com.kkkkan.youtube.tubtub.model.YouTubePlaylist;
import com.squareup.picasso.Picasso;

import java.util.List;

public class PlaylistsAdapter extends RecyclerView.Adapter<PlaylistsAdapter.ViewHolder> {
    private final static String TAG_NAME = "PlaylistsAdapter";
    final private boolean deleteIconShow;
    private Context context;
    private List<YouTubePlaylist> playlists;
    private ItemEventsListener<YouTubePlaylist> itemEventsListener;
    private OnDetailClickListener onDetailClickListener;


    public PlaylistsAdapter(Context context, List<YouTubePlaylist> playlists) {
        super();
        this.context = context;
        this.playlists = playlists;
        deleteIconShow = true;
    }


    /**
     * ゴミ箱アイコンを見せるか見せないか指定してインスタンスを作るコンストラクタ。
     * デフォルトは見えるようにしているので、特に見せたくないときに使うのを想定。
     *
     * @param context
     * @param playlists
     * @param deleteIconShow
     */
    public PlaylistsAdapter(Context context, List<YouTubePlaylist> playlists, boolean deleteIconShow) {
        super();
        this.context = context;
        this.playlists = playlists;
        this.deleteIconShow = deleteIconShow;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_item, null);
        ViewHolder holder = new ViewHolder(v);
        //1つのPlaylistAdapterのインスタンスのなかではdeleteIconShowの値は変わらないので、
        // ViewHolder生成時にdeleteButtonの見える/見えないを決めてしまう。
        if (!deleteIconShow) {
            holder.deleteButton.setVisibility(View.INVISIBLE);
        }
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final YouTubePlaylist playlist = playlists.get(position);
        Picasso.with(context).load(playlist.getThumbnailURL()).into(holder.thumbnail);
        holder.title.setText(playlist.getTitle());
        String videosNumberText = context.getString(R.string.number_of_videos) + String.valueOf(playlist.getNumberOfVideos());
        holder.videosNumber.setText(videosNumberText);
        String status = context.getString(R.string.status) + playlist.getStatus();
        holder.privacy.setText(status);

        if (playlist.getStatus().equals("private")) {
            holder.shareButton.setEnabled(false);
        } else {
            holder.shareButton.setVisibility(View.VISIBLE);
        }

        holder.shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (itemEventsListener != null) {
                    itemEventsListener.onShareClicked(playlist.getId());
                }
            }
        });

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemEventsListener != null) {
                    itemEventsListener.onDeleteClicked(playlist);
                }
            }
        });

        holder.itemView.setTag(playlist);

        /**
         * Show song list of playlist
         *
         * プレイリストの曲一覧を表示
         * */
        holder.playlistDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onDetailClickListener != null) {
                    //onDetailClickListener:MainActivity
                    onDetailClickListener.onDetailClick(playlist);
                    Log.d(TAG_NAME, "onDetailClickListener-playlistId:" + String.valueOf(playlist.getId()) + String.valueOf(playlist.getTitle()));
                }
            }
        });

        holder.thumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemEventsListener != null) {
                    //itemEventListener:playlistFrragment
                    itemEventsListener.onItemClick(playlist);
                }
            }
        });


    }

    @Override
    public int getItemCount() {
        return (null != playlists ? playlists.size() : 0);
    }

    public void setOnItemEventsListener(ItemEventsListener<YouTubePlaylist> listener) {
        itemEventsListener = listener;
    }

    public void setOnDetailClickListener(OnDetailClickListener onDetailClickListener) {
        this.onDetailClickListener = onDetailClickListener;
    }


    public interface OnDetailClickListener {
        void onDetailClick(YouTubePlaylist playlist);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView videosNumber;
        TextView privacy;
        ImageView shareButton;
        ImageView playlistDetail;
        ImageView deleteButton;

        public ViewHolder(View itemView) {
            super(itemView);
            thumbnail = (ImageView) itemView.findViewById(R.id.video_thumbnail);
            title = (TextView) itemView.findViewById(R.id.playlist_title);
            videosNumber = (TextView) itemView.findViewById(R.id.videos_number);
            privacy = (TextView) itemView.findViewById(R.id.privacy);
            shareButton = (ImageView) itemView.findViewById(R.id.share_button);
            playlistDetail = (ImageView) itemView.findViewById(R.id.detail_button);
            deleteButton = (ImageView) itemView.findViewById(R.id.delete_button);
        }
    }

}