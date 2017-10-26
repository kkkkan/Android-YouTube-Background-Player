package com.smedic.tubtub.adapters;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.smedic.tubtub.R;
import com.smedic.tubtub.database.YouTubeSqlDb;
import com.smedic.tubtub.interfaces.ItemEventsListener;
import com.smedic.tubtub.model.YouTubeVideo;
import com.squareup.picasso.Picasso;

import java.util.Arrays;
import java.util.List;

/**
 * Created by admin on 2017/08/30.
 */

public class RecentlyVideosAdapter extends RecyclerView.Adapter<RecentlyVideosAdapter.ViewHolder> {
    private Context context;
    private final List<YouTubeVideo> list;
    //private ArrayList<Boolean> itemCheck;
    private Boolean[] itemChecked;
    private ItemEventsListener<YouTubeVideo> itemEventsListener;

    public RecentlyVideosAdapter(Context context, List<YouTubeVideo> list) {
        super();
        this.list = list;
        this.context = context;
        //this.itemCheck = new ArrayList<>();
        this.itemChecked = new Boolean[list.size()];
    }

    @Override
    public RecentlyVideosAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recently_video_item, null);
        return new RecentlyVideosAdapter.ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(RecentlyVideosAdapter.ViewHolder holder, final int position) {
        final YouTubeVideo video = list.get(position);
        /*if (YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE).checkIfExists(video.getId())) {
            itemCheck.add(true);
        } else {
            itemCheck.add(false);
        }*/

        //最近見たリストで再生しているときにこれがないとリストが動的に数が増えるので落ちる
        if (position >= itemChecked.length) {
            List check = Arrays.asList(itemChecked);
            itemChecked = (Boolean[]) check.toArray(new Boolean[position + 1]);
        }

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
        holder.favoriteCheckBox.setChecked(itemChecked[position]/*itemCheck.get(position)*/);

        /*holder.favoriteCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
                itemCheck.set(position, isChecked);
                if (itemEventsListener != null) {
                    itemEventsListener.onFavoriteClicked(video, isChecked);
                }
            }
        });*/

        /*お気に入りリストに入れたり抜いたり*/
        holder.favoriteCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
                itemChecked[position] = isChecked;
                if (itemEventsListener != null) {
                    itemEventsListener.onFavoriteClicked(video, isChecked);
                }
            }
        });
        holder.shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (itemEventsListener != null) {
                    itemEventsListener.onShareClicked(video.getId());
                }
            }
        });

        holder.addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (itemEventsListener != null) {
                    itemEventsListener.onAddClicked(video);
                }
            }
        });


        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemEventsListener != null) {
                    itemEventsListener.onDeleteClicked(video);
                }
            }
        });
        holder.thumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemEventsListener != null) {
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


    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView duration;
        TextView viewCount;
        CheckBox favoriteCheckBox;
        ImageView shareButton;
        ImageView addButton;
        ImageView deleteButton;

        public ViewHolder(final View itemView) {
            super(itemView);
            thumbnail = (ImageView) itemView.findViewById(R.id.video_thumbnail);
            title = (TextView) itemView.findViewById(R.id.video_title);
            duration = (TextView) itemView.findViewById(R.id.video_duration);
            viewCount = (TextView) itemView.findViewById(R.id.views_number);
            favoriteCheckBox = (CheckBox) itemView.findViewById(R.id.favoriteButton);
            shareButton = (ImageView) itemView.findViewById(R.id.shareButton);
            addButton = (ImageView) itemView.findViewById(R.id.PlaylistAddButton);
            deleteButton = (ImageView) itemView.findViewById(R.id.musicDeleteButton);

            itemView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int dp = 4;
                    int px = (int) (itemView.getResources().getDisplayMetrics().density * dp);
                    Rect delegateArea = new Rect();
                    favoriteCheckBox.getHitRect(delegateArea);
                    delegateArea.top -= px;
                    delegateArea.left -= px;
                    delegateArea.right += px;
                    delegateArea.bottom += px;
                    ((View) favoriteCheckBox.getParent()).setTouchDelegate(new TouchDelegate(delegateArea, favoriteCheckBox));
                }
            });
        }
    }

    public void setOnItemEventsListener(ItemEventsListener<YouTubeVideo> listener) {
        itemEventsListener = listener;
    }
}
