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

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kkkkan.youtube.R;
import com.kkkkan.youtube.tubtub.adapters.PlaylistsAdapter;
import com.kkkkan.youtube.tubtub.interfaces.OnFavoritesSelected;
import com.kkkkan.youtube.tubtub.model.YouTubePlaylist;
import com.kkkkan.youtube.tubtub.model.YouTubeVideo;

import java.util.ArrayList;
import java.util.List;

import static com.kkkkan.youtube.R.layout.suggestions;

/**
 * Created by ka1n9 on 2017/12/10.
 */

public class TabLayoutFragment extends Fragment implements OnFavoritesSelected, PlaylistsAdapter.OnDetailClickListener {
    final private static String TAG = "TabLayoutFragment";
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private SearchFragment searchFragment;
    private RecentlyWatchedFragment recentlyPlayedFragment;
    private FavoritesFragment favoritesFragment;
    private int[] tabIcons = {
            R.drawable.ic_action_heart,
            R.drawable.ic_recently_wached,
            R.drawable.ic_search,
            R.drawable.ic_action_playlist
    };

    /**
     * When making a new instance of TabLayoutFragment make sure to make with this mezzo
     * <p>
     * TabLyoutFragmentの新しいインスタンスを作るときは必ず
     * このメゾッドで作ること
     */
    static public TabLayoutFragment newInstance() {
        TabLayoutFragment fragment = new TabLayoutFragment();
        return fragment;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onattach");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        final View view = inflater.inflate(R.layout.fragment_tab, container, false);

        //Set the fragment held by viewPage to 3
        //viewPageでキャッシュして保持するfragmentを三枚に設定
        viewPager = (ViewPager) view.findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(3);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) view.findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        setupTabIcons();

        setHasOptionsMenu(true);
        return view;
    }

    /**
     * Setups icons for 3 tabs
     */
    private void setupTabIcons() {
        tabLayout.getTabAt(0).setIcon(tabIcons[0]);
        tabLayout.getTabAt(1).setIcon(tabIcons[1]);
        tabLayout.getTabAt(2).setIcon(tabIcons[2]);
        tabLayout.getTabAt(3).setIcon(tabIcons[3]);
    }

    /**
     * Setups viewPager for switching between pages according to the selected tab
     *
     * @param viewPager
     */
    private void setupViewPager(ViewPager viewPager) {
        Log.d(TAG, "setupViewPager");
        TabLayoutFragment.ViewPagerAdapter adapter = new TabLayoutFragment.ViewPagerAdapter(getChildFragmentManager());

        searchFragment = SearchFragment.newInstance();
        recentlyPlayedFragment = RecentlyWatchedFragment.newInstance();
        favoritesFragment = FavoritesFragment.newInstance();
        PlaylistsFragment playlistsFragment = PlaylistsFragment.newInstance();

        adapter.addFragment(favoritesFragment, null);//0
        adapter.addFragment(recentlyPlayedFragment, null);//1
        adapter.addFragment(searchFragment, null);//2
        adapter.addFragment(playlistsFragment, null);//3

        viewPager.setAdapter(adapter);
    }

    public void handleSearch(String query) {
        //スムーズスクロールありでfragmenを2に変更
        viewPager.setCurrentItem(2, true); //switch to search fragment

        if (searchFragment != null) {
            Log.d(TAG, "searchFragment != null");
            searchFragment.searchQuery(query);
        }
    }

    public void clearRecentlyPlayedList() {
        recentlyPlayedFragment.clearRecentlyPlayedList();
    }

    public void removeFromFavorites(YouTubeVideo video) {
        favoritesFragment.removeFromFavorites(video);
    }

    @Override
    public void onFavoritesSelected(YouTubeVideo video, boolean isChecked) {
        Fragment fragment = getParentFragment();
        if (fragment instanceof OnFavoritesSelected) {
            ((OnFavoritesSelected) fragment).onFavoritesSelected(video, isChecked);
        }
    }

    @Override
    public void onAddSelected(YouTubeVideo video) {
        Fragment fragment = getParentFragment();
        if (fragment instanceof OnFavoritesSelected) {
            ((OnFavoritesSelected) fragment).onAddSelected(video);
        }
    }

    @Override
    public void onDetailClick(YouTubePlaylist playlist) {
        Fragment fragment = getParentFragment();
        if (fragment instanceof PlaylistsAdapter.OnDetailClickListener) {
            ((PlaylistsAdapter.OnDetailClickListener) fragment).onDetailClick(playlist);
        }
    }

    /**
     * Class which provides adapter for fragment pager
     */
    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<android.support.v4.app.Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        private ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        private void addFragment(android.support.v4.app.Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }

    }


}
