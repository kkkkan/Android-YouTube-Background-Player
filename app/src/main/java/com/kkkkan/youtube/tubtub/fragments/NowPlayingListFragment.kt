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

import android.content.Context
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kkkkan.youtube.R

/**
 * Created by admin on 2017/12/08.
 */

class NowPlayingListFragment : BaseFragment() {
    private val TAG: String = "NowPlayingListFragment"
    private var recyclerView: RecyclerView? = null
    private var swipeToRefresh: SwipeRefreshLayout? = null
    private var c: Context? = null


    override fun onAttach(context: Context?) {
        super.onAttach(context)
        c = context
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        Log.d(TAG, "onCreateView")
        val view: View = inflater!!.inflate(R.layout.fragment_list, container, false)
        recyclerView = view.findViewById(R.id.fragment_list_items) as RecyclerView
        swipeToRefresh = view.findViewById(R.id.swipe_to_refresh) as SwipeRefreshLayout
        val linearLayoutManager: LinearLayoutManager = LinearLayoutManager(c)
        val dividerItemDecoration: DividerItemDecoration = DividerItemDecoration((recyclerView as RecyclerView).context, linearLayoutManager.orientation)
        (recyclerView as RecyclerView).addItemDecoration(dividerItemDecoration)
        //(recyclerView as RecyclerView).setBackgroundColor(getColor("#ffffff"))
        (swipeToRefresh as SwipeRefreshLayout).setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener {
            //くるくるの時することをここに書く
        })
        return view
    }

    override fun onResume() {
        super.onResume()
        //ここでrecyclerviewの整え等する
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
    }

    override fun onDestroy() {
        super.onDestroy()

    }


}