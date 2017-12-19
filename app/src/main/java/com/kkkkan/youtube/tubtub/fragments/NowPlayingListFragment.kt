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
package com.kkkkan.youtube.tubtub.fragments


import android.arch.lifecycle.Observer
import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearSmoothScroller
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.kkkkan.youtube.R
import com.kkkkan.youtube.tubtub.adapters.NowPlayingListAdapter
import com.kkkkan.youtube.tubtub.interfaces.CurrentPositionChanger
import com.kkkkan.youtube.tubtub.interfaces.ItemEventsListener
import com.kkkkan.youtube.tubtub.model.YouTubePlaylist
import com.kkkkan.youtube.tubtub.model.YouTubeVideo
import com.kkkkan.youtube.tubtub.utils.PlaylistsCash
import com.kkkkan.youtube.tubtub.utils.Settings

/**
 * AttachするactivitはCurrentPointChangerのインスタンスであること
 *
 *
 * Created by kkkkan on 2017/12/08.
 */

class NowPlayingListFragment : BaseFragment(), ItemEventsListener<YouTubeVideo> {
    private val TAG: String = "NowPlayingListFragment"
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeToRefresh: SwipeRefreshLayout
    private var c: Context? = null
    private var list =  ArrayList<YouTubeVideo>()
    private lateinit var adapter: NowPlayingListAdapter
    private lateinit var noListTextView: TextView

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        c = context
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        Log.d(TAG, "onCreateView")
        val view: View = inflater!!.inflate(R.layout.fragment_list, container, false)
        view.findViewById(R.id.frame_layout).setBackgroundColor(ContextCompat.getColor(c, R.color.colorPrimaryDark))
        noListTextView = view.findViewById(R.id.no_playiniglist_text) as TextView
        recyclerView = view.findViewById(R.id.fragment_list_items) as RecyclerView
        swipeToRefresh = view.findViewById(R.id.swipe_to_refresh) as SwipeRefreshLayout
        val linearLayoutManager: LinearLayoutManager = LinearLayoutManager(c)
        recyclerView.layoutManager = linearLayoutManager
        val dividerItemDecoration: DividerItemDecoration = DividerItemDecoration((recyclerView as RecyclerView).context, linearLayoutManager.orientation)
        recyclerView.addItemDecoration(dividerItemDecoration)
        adapter = NowPlayingListAdapter(c!!, list, this as ItemEventsListener<YouTubeVideo>)

        recyclerView.adapter = adapter

        swipeToRefresh.setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener {
            //くるくるの時することをここに書く
            updateRecyclerView()
        })

        Settings.getInstance().shuffleMutableLiveData.observe(this, Observer {
            updateRecyclerView()
        })
        return view
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        //ここでrecyclerviewの整え等する
        updateRecyclerView()
        //スタート位置を今再生中のビデオがリストのトップになるようにする
        //表示完了後すぐやろうとするとうまくいかないので少し遅らす
        recyclerView.postDelayed(Runnable {
            prepareScrollPosition()
        }, 500)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")

    }

    fun updateRecyclerView() {
        Log.d(TAG, "updataRecyclerView")
        val nowList: List<YouTubeVideo>?
        when (Settings.getInstance().shuffle) {
            Settings.Shuffle.ON -> {
                nowList = PlaylistsCash.Instance.shuffleList
            }
            Settings.Shuffle.OFF -> {
                nowList = PlaylistsCash.Instance.normalList
            }
            null -> {
                //Settingsのコンストラクタで初期値を与えているので
                // 本来ならnullのことはありえ無いが、warningが出るのでかく
                nowList = null
            }
        }
        if (nowList == null || nowList.size == 0) {
            noListTextView.visibility = View.VISIBLE
            if (swipeToRefresh.isRefreshing) {
                swipeToRefresh.setRefreshing(false)
            }
            return
        }
        noListTextView.visibility = View.INVISIBLE
        Log.d(TAG, "nowList.size.toString() : " + nowList.size.toString())
        list.clear()
        list.addAll(nowList)
        adapter.notifyDataSetChanged()
        //スクロール位置直す
        prepareScrollPosition()
        if (swipeToRefresh.isRefreshing) {
            swipeToRefresh.setRefreshing(false)
        }
    }

    /**
     * 今再生中のビデオがリストのトップになるようにスクロールするメゾッド
     */
    private fun prepareScrollPosition() {
        class mySmoothScroller : LinearSmoothScroller(recyclerView.context) {
            override fun getVerticalSnapPreference(): Int {
                return LinearSmoothScroller.SNAP_TO_START
            }
        }

        val smoothScroller: LinearSmoothScroller = mySmoothScroller()
        smoothScroller.setTargetPosition(PlaylistsCash.Instance.currentVideoIndex);
        recyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
    }

    override fun onShareClicked(itemId: String?) {

    }

    override fun onFavoriteClicked(video: YouTubeVideo?, isChecked: Boolean) {

    }

    override fun onAddClicked(video: YouTubeVideo?) {

    }

    override fun onItemClick(model: YouTubeVideo?) {
        if (context is CurrentPositionChanger) {
            (context as CurrentPositionChanger).changeCurrentPosition(list.indexOf(model))
        }
    }

    override fun onDeleteClicked(video: YouTubeVideo?) {
        PlaylistsCash.Instance.deleteVideoInList(video, Settings.getInstance().shuffle)
        updateRecyclerView()
    }

    override fun onDeleteClicked(playlist: YouTubePlaylist?) {

    }

}