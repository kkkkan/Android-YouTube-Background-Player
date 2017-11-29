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

package com.kkkkan.youtube.tubtub.fragments;


import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kkkkan.youtube.R;
import com.kkkkan.youtube.tubtub.interfaces.ViewPagerListener;

/**
 * A simple {@link Fragment} subclass.
 */
public class PlaylistTitleFragment extends Fragment {

    private String playlistTitle;
    private TextView textView;

    public PlaylistTitleFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist_title, container, false);
        textView = (TextView) view.findViewById(R.id.title_view);
        textView.setText(playlistTitle);
        return view;
    }

    public void setPlaylistTitle(String playlistTitle) {
        this.playlistTitle = playlistTitle;
    }

    //Tab visualization and touch revival
    //tabの可視化とタッチの復活。
    public void onDestroy() {
        super.onDestroy();
        Fragment fragment = getParentFragment();
        if (fragment instanceof ViewPagerListener) {
            TabLayout tabLayout = ((ViewPagerListener) fragment).getTabLayout();
            tabLayout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
            tabLayout.setVisibility(View.VISIBLE);
        }
    }
}
