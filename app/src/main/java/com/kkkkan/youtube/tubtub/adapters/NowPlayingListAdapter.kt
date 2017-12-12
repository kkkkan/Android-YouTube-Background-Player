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
import com.squareup.picasso.Picasso

/**
 * Created by admin on 2017/12/11.
 */
class NowPlayingListAdapter(c: Context, list: List<YouTubeVideo>?, listener: ItemEventsListener<YouTubeVideo>) : RecyclerView.Adapter<NowPlayingListAdapter.ViewHolder>() {
    private val TAG = "NowPlayingListAdapter"
    private val context: Context = c
    private val playlist: List<YouTubeVideo>? = list
    private val itemEventsListener: ItemEventsListener<YouTubeVideo>

    init {
        itemEventsListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        Log.d(TAG, "onCreateViewHolder")
        val view: View = LayoutInflater.from(parent?.context).inflate(R.layout.fragment_now_playing_list, null)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        if (playlist == null || holder == null) {
            Log.d(TAG, "playlist == null || holder == null")
            return
        }
        Log.d(TAG, "onBindViewHolder : " + position.toString())
        val video: YouTubeVideo = playlist.get(position)

        Picasso.with(context).load(video.thumbnailURL).into(holder.thumbnail)
        holder.title.setText(video.title)
        holder.duration.setText(video.duration)
        holder.viewCount.setText(video.viewCount)
        holder.deleteButton.setOnClickListener(View.OnClickListener {
            itemEventsListener.onDeleteClicked(video)
        })
        holder.thumbnail.setOnClickListener(View.OnClickListener {
            itemEventsListener.onItemClick(video)
        })

        holder.itemView.setTag(position)
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

        fun changeItemViewLookWhenStartPlay() {
            val background: RelativeLayout = itemView.findViewById(R.id.item_background) as RelativeLayout
            background.alpha = 1f
            return
        }

        fun changeItemViewLookWhenfinishPlay() {
            val background: RelativeLayout = itemView.findViewById(R.id.item_background) as RelativeLayout
            background.alpha = 0.5f
            return
        }
    }

}
