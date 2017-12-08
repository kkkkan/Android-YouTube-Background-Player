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

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kkkkan.youtube.R
import com.kkkkan.youtube.tubtub.interfaces.ViewPagerListener

/**
 * Created by admin on 2017/12/08.
 */

class NowPlayingListFragment : BaseFragment() {
    private val TAG: String = "NowPlayingListFragment"
    private var recyclerView: RecyclerView? = null
    private var swipeToRefresh: SwipeRefreshLayout? = null


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        Log.d(TAG, "onCreateView")
        val view: View = inflater!!.inflate(R.layout.fragment_list, container, false)
        val linearLayoutManager: LinearLayoutManager = LinearLayoutManager(activity)
        val listItems: View? = view.findViewById(R.id.fragment_list_items)
        if (listItems is RecyclerView) {
            recyclerView = listItems
            recyclerView?.setLayoutManager(linearLayoutManager)
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        val fragment: android.support.v4.app.Fragment? = parentFragment
        if (fragment is ViewPagerListener) {
            val viewPager: ViewPager = fragment.viewPager
            viewPager.setOnTouchListener { v, event -> true }
            viewPager.visibility = View.VISIBLE
            val tabLayout: TabLayout = fragment.tabLayout
            tabLayout.setOnTouchListener { v, event -> true }
            tabLayout.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()

    }
}