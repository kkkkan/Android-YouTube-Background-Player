package com.smedic.tubtub.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.smedic.tubtub.R;
import com.smedic.tubtub.database.YouTubeSqlDb;
import com.smedic.tubtub.fragments.PlaylistDetailFragment;
import com.smedic.tubtub.interfaces.ItemEventsListener;
import com.smedic.tubtub.model.YouTubeVideo;
import com.smedic.tubtub.utils.Config;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by admin on 2017/08/24.
 */

public class PlaylistDetailAdapter extends RecyclerView.Adapter<PlaylistDetailAdapter.ViewHolder>
        implements View.OnClickListener {

    private static final String TAG = "PlaylistDetailAdapter";
    private Context context;
    private final List<YouTubeVideo> list;
    private boolean[] itemChecked;
    private ItemEventsListener<YouTubeVideo> itemEventsListener;

    public PlaylistDetailAdapter(Context context, List<YouTubeVideo> list) {
        super();
        this.list = list;
        this.context = context;
        this.itemChecked = new boolean[(int) Config.NUMBER_OF_VIDEOS_RETURNED];
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_playlist_detail, null);
        view.setOnClickListener(this);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PlaylistDetailAdapter.ViewHolder holder, final int position) {
        final YouTubeVideo video = list.get(position);
        if (YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE).checkIfExists(video.getId())) {
            itemChecked[position] = true;
        } else {
            itemChecked[position] = false;
        }

        Picasso.with(context).load(video.getThumbnailURL()).into(holder.thumbnail);
        holder.title.setText(video.getTitle());
        holder.duration.setText(video.getDuration());
        holder.viewCount.setText(video.getViewCount());
        holder.favoriteCheckBox.setOnCheckedChangeListener(null);
        holder.favoriteCheckBox.setChecked(itemChecked[position]);

        /*お気に入りリストに入れたり抜いたり*/
        holder.favoriteCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
                itemChecked[position] = isChecked;
                if (itemEventsListener != null) {
                    itemEventsListener.onFavoriteClicked(video, isChecked);
                }
            }
        });

        /*shareする*/
        holder.shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (itemEventsListener != null) {
                    itemEventsListener.onShareClicked(video.getId());
                }
            }
        });


        holder.DeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (itemEventsListener != null) {
                    ((PlaylistDetailFragment) itemEventsListener).setDeleteVideoIndex(position);
                    itemEventsListener.onAddClicked(video);
                }
            }
        });

        holder.thumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemEventsListener != null) {
                    Log.d(TAG, "onClick");
                    itemEventsListener.onItemClick(video);
                }
            }
        });
        holder.itemView.setTag(video);

    }

    @Override
    public int getItemCount() {
        return (null != list ? list.size() : 0);
    }

    @Override
    public void onClick(View v) {
       /*if (itemEventsListener != null) {
            Log.d(TAG, "onClick");
            YouTubeVideo item = (YouTubeVideo) v.getTag();
            itemEventsListener.onItemClick(item);
        }*/
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView duration;
        TextView viewCount;
        CheckBox favoriteCheckBox;
        ImageView shareButton;
        ImageView DeleteButton;

        public ViewHolder(View itemView) {
            super(itemView);
            thumbnail = (ImageView) itemView.findViewById(R.id.video_thumbnail);
            title = (TextView) itemView.findViewById(R.id.video_title);
            duration = (TextView) itemView.findViewById(R.id.video_duration);
            viewCount = (TextView) itemView.findViewById(R.id.views_number);
            favoriteCheckBox = (CheckBox) itemView.findViewById(R.id.favoriteButton);
            shareButton = (ImageView) itemView.findViewById(R.id.shareButton);
            DeleteButton = (ImageView) itemView.findViewById(R.id.musicDeleteButton);
        }
    }

    public void setOnItemEventsListener(ItemEventsListener<YouTubeVideo> listener) {
        itemEventsListener = listener;
    }

}
