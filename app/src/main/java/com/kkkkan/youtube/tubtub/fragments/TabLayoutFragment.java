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

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
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

/**
 * Created by kkkkan on 2017/12/10.
 */

public class TabLayoutFragment extends Fragment implements OnFavoritesSelected, PlaylistsAdapter.OnDetailClickListener {
    final private static String TAG = "TabLayoutFragment";
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private final int favoriteTabNum = 0;
    private final int recentlyTabNum = 1;
    private final int searchTabNum = 2;
    private final int playlistTabNum = 3;
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
        Log.d(TAG, "onAttach");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        Log.d(TAG, "savedInstanceState==null is : " + String.valueOf(savedInstanceState == null));
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach()");
    }

    /**
     * Setups icons for 3 tabs
     */
    private void setupTabIcons() {
        tabLayout.getTabAt(favoriteTabNum).setIcon(tabIcons[0]);
        tabLayout.getTabAt(recentlyTabNum).setIcon(tabIcons[1]);
        tabLayout.getTabAt(searchTabNum).setIcon(tabIcons[2]);
        tabLayout.getTabAt(playlistTabNum).setIcon(tabIcons[3]);
    }

    /**
     * Setups viewPager for switching between pages according to the selected tab
     *
     * @param viewPager
     */
    private void setupViewPager(ViewPager viewPager) {
        Log.d(TAG, "setupViewPager");
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getChildFragmentManager());
        SearchFragment searchFragment = SearchFragment.newInstance();
        RecentlyWatchedFragment recentlyPlayedFragment = RecentlyWatchedFragment.newInstance();
        FavoritesFragment favoritesFragment = FavoritesFragment.newInstance();
        PlaylistsFragment playlistsFragment = PlaylistsFragment.newInstance();

        viewPagerAdapter.addFragment(favoriteTabNum, favoritesFragment, null);//0
        viewPagerAdapter.addFragment(recentlyTabNum, recentlyPlayedFragment, null);//1
        viewPagerAdapter.addFragment(searchTabNum, searchFragment, null);//2
        viewPagerAdapter.addFragment(playlistTabNum, playlistsFragment, null);//3

        viewPager.setAdapter(viewPagerAdapter);
    }


    public void startSearch(String query) {
        //すぐにviewPagerのpositionを変えると、検索のためにpopBackStack()してきてたときに
        //検索結果ページに動いてくれないので少し遅らす
        viewPager.postDelayed(new Runnable() {
            @Override
            public void run() {
                viewPager.setCurrentItem(searchTabNum, true);
            }
        }, 100);
        Fragment searchFragment = getFragmentInVewPager(searchTabNum);
        if (searchFragment instanceof SearchFragment) {
            Log.d(TAG, "searchFragment != null");
            ((SearchFragment) searchFragment).searchQuery(query);
        }
    }

    public void clearRecentlyPlayedList() {
        Fragment recentlyWatchedFragment = getFragmentInVewPager(recentlyTabNum);
        if (recentlyWatchedFragment instanceof RecentlyWatchedFragment) {
            ((RecentlyWatchedFragment) recentlyWatchedFragment).clearRecentlyPlayedList();
        }
    }

    public void removeFromFavorites(YouTubeVideo video) {
        Fragment favoritesFragment = getFragmentInVewPager(favoriteTabNum);
        if (favoritesFragment instanceof FavoritesFragment) {
            ((FavoritesFragment) favoritesFragment).removeFromFavorites(video);
        }
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

    private Fragment getFragmentInVewPager(int index) {
        ViewPagerAdapter adapter = (ViewPagerAdapter) viewPager.getAdapter();
        //instantiateItem()で今viewpagerで表示しているfragmentセット上のfragment取得できる
        Fragment fragment = (Fragment) adapter.instantiateItem(viewPager, index);
        return fragment;
    }

    /**
     * Class which provides adapter for fragment pager
     */
    class ViewPagerAdapter extends FragmentStatePagerAdapter {
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

        private void addFragment(int index, android.support.v4.app.Fragment fragment, String title) {
            mFragmentList.add(index, fragment);
            mFragmentTitleList.add(index, title);
        }


        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }


    }


}
