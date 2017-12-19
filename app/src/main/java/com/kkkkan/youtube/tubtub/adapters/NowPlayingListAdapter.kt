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
package com.kkkkan.youtube.tubtub.adapters

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.kkkkan.youtube.R
import com.kkkkan.youtube.tubtub.interfaces.ItemEventsListener
import com.kkkkan.youtube.tubtub.model.YouTubeVideo
import com.kkkkan.youtube.tubtub.utils.PlaylistsCash
import com.squareup.picasso.Picasso

/**
 * Created by admin on 2017/12/11.
 */
class NowPlayingListAdapter(val context: Context, val playlist: List<YouTubeVideo>?, val itemEventsListener: ItemEventsListener<YouTubeVideo>) : RecyclerView.Adapter<NowPlayingListAdapter.ViewHolder>() {
    private val TAG = "NowPlayingListAdapter"

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        Log.d(TAG, "onCreateViewHolder")
        val view: View = LayoutInflater.from(parent?.context).inflate(R.layout.fragment_now_playing_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        if (playlist == null || holder == null) {
            Log.d(TAG, "playlist == null || holder == null")
            return
        }
        //Log.d(TAG, "onBindViewHolder : " + position.toString())
        val video: YouTubeVideo = playlist.get(position)

        Picasso.with(context).load(video.thumbnailURL).into(holder.thumbnail)
        holder.title.setText(video.title)
        holder.duration.setText(video.duration)
        holder.viewCount.setText(video.viewCount)
        holder.thumbnail.setOnClickListener(View.OnClickListener {
            itemEventsListener.onItemClick(video)
        })

        holder.itemView.setTag(position)
        //今再生中のビデオのみはっきり表示
        //それ以外は暗めに表示
        PlaylistsCash.Instance.mutableCurrentVideoIndex.observe(context as LifecycleOwner, Observer<Int> { t ->
            if (t != null) {
                if (t == position) {
                    holder.background.alpha = 1f
                    holder.deleteButton.visibility = View.INVISIBLE
                    holder.deleteButton.setOnClickListener(null)
                } else {
                    holder.background.alpha = 0.5f
                    holder.deleteButton.visibility = View.VISIBLE
                    holder.deleteButton.setOnClickListener(View.OnClickListener {
                        itemEventsListener.onDeleteClicked(video)
                    })
                }
            }
        })
    }

    override fun getItemCount(): Int {
        return if (null != playlist) (playlist.size) else 0
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //super(itemView)は自然に既に呼ばれている。
        val thumbnail: ImageView = itemView.findViewById(R.id.video_thumbnail) as ImageView
        val title: TextView = itemView.findViewById(R.id.video_title) as TextView
        val duration: TextView = itemView.findViewById(R.id.video_duration) as TextView
        val viewCount: TextView = itemView.findViewById(R.id.views_number) as TextView
        val deleteButton: ImageView = itemView.findViewById(R.id.delete_button) as ImageView
        val background: RelativeLayout = itemView.findViewById(R.id.item_background) as RelativeLayout

    }

}
