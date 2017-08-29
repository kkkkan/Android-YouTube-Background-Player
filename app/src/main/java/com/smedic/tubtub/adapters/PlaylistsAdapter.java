package com.smedic.tubtub.adapters;

/**
 * Created by smedic on 6.2.17..
 */

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.smedic.tubtub.R;
import com.smedic.tubtub.fragments.PlaylistDetailFragment;
import com.smedic.tubtub.interfaces.ItemEventsListener;
import com.smedic.tubtub.model.YouTubePlaylist;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Custom array adapter class
 */
public class PlaylistsAdapter extends RecyclerView.Adapter<PlaylistsAdapter.ViewHolder>
        implements View.OnClickListener {

    private Context context;
    private List<YouTubePlaylist> playlists;
    private ItemEventsListener<YouTubePlaylist> itemEventsListener;
    private OnDetailClickListener onDetailClickListener;

    private TextView mTextView;


    public interface OnDetailClickListener{
        void onDetailClick(YouTubePlaylist playlist);
    }

    public PlaylistsAdapter(Context context, List<YouTubePlaylist> playlists) {
        super();
        this.context = context;
        this.playlists = playlists;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_item, null);
        v.setOnClickListener(this);
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
        holder.itemView.setTag(playlist);

        /*プレイリストの曲一覧を表示*/
        holder.playlistDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(onDetailClickListener!=null) {
                    onDetailClickListener.onDetailClick(playlist);
                    mTextView.setText(playlist.getTitle()+"("+playlist.getNumberOfVideos()+")");
                    Log.d("kandabashi","onDetailClickListener-playlistId:"+String.valueOf(playlist.getId())+String.valueOf(playlist.getTitle()));
                  // PlaylistDetailFragment playlistDetailFragment=new PlaylistDetailFragment().newInstance();
                    //playlistDetailFragment.setPlaylistId(playlist.getId());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return (null != playlists ? playlists.size() : 0);
    }

    @Override
    public void onClick(View v) {
        if (itemEventsListener != null) {
            YouTubePlaylist item = (YouTubePlaylist) v.getTag();
            itemEventsListener.onItemClick(item);
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView videosNumber;
        TextView privacy;
        ImageView shareButton;
        ImageView playlistDetail;

        public ViewHolder(View itemView) {
            super(itemView);
            thumbnail = (ImageView) itemView.findViewById(R.id.video_thumbnail);
            title = (TextView) itemView.findViewById(R.id.playlist_title);
            videosNumber = (TextView) itemView.findViewById(R.id.videos_number);
            privacy = (TextView) itemView.findViewById(R.id.privacy);
            shareButton = (ImageView) itemView.findViewById(R.id.share_button);
            playlistDetail=(ImageView)itemView.findViewById(R.id.detail_button);
        }
    }

    public void setOnItemEventsListener(ItemEventsListener<YouTubePlaylist> listener) {
        itemEventsListener = listener;
    }

    public void setOnDetailClickListener(OnDetailClickListener onDetailClickListener) {
        this.onDetailClickListener = onDetailClickListener;
    }

    public void setmTextView(TextView mTextView) {
        this.mTextView = mTextView;
    }
}