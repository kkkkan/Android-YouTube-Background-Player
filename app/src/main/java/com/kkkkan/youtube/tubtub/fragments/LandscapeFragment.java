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


import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.kkkkan.youtube.R;
import com.kkkkan.youtube.tubtub.MainActivity;
import com.kkkkan.youtube.tubtub.Settings;
import com.kkkkan.youtube.tubtub.interfaces.TitlebarListener;

/**
 * A simple {@link Fragment} subclass.
 */
public class LandscapeFragment extends Fragment implements SurfaceHolder.Callback {
    private final static String TAG = "LandscapeFragment";
    private SurfaceView surfaceView;
    private SurfaceHolder holder;
    private TextView titleView;
    private CheckBox repeatOneBox;
    private CheckBox lockBox;
    private CheckBox repeatPlaylistBox;
    private TitlebarListener titlebarListener;

    /**
     * When making a new instance of LandscapeFragment make sure to make with this mezzo
     * <p>
     * LandscapeFragmentの新しいインスタンスを作るときは必ず
     * このメゾッドで作ること
     */
    static public LandscapeFragment getNewLandscapeFragment(TitlebarListener titlebarListener) {
        LandscapeFragment fragment = new LandscapeFragment();
        fragment.titlebarListener = titlebarListener;
        return fragment;
    }

    public LandscapeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_landscape, container, false);
        surfaceView = (SurfaceView) view.findViewById(R.id.surface);
        surfaceView.getHolder().addCallback(this);
        titleView = (TextView) view.findViewById(R.id.title_view);
        lockBox = (CheckBox) view.findViewById(R.id.lock_box);
        repeatOneBox = (CheckBox) view.findViewById(R.id.repeat_one_box);
        repeatPlaylistBox = (CheckBox) view.findViewById(R.id.repeat_playlist_box);

        //Click listener installed in check box
        //チェックボックスにクリックリスナー設置
        repeatOneBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                titlebarListener.repeatOneCheckListener();
                checkBoxUpdata();
            }
        });

        repeatPlaylistBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                titlebarListener.repeatPlaylistCheckListener();
                checkBoxUpdata();
            }
        });

        lockBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                titlebarListener.lockCheckListener();
                checkBoxUpdata();
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        //Adjust the state of the check box to the setting
        //チェックボックスの状態を設定に合わせる
        checkBoxUpdata();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    /**
     * Retrieving the surfaceView and recreating the instance of the holder accordingly
     * <p>
     * surfaceViewの取得しなおしとそれに伴う
     * holderのインスタンスの作り直し
     */
    private void makeSurfaceView() {
        surfaceView = (SurfaceView) getView().findViewById(R.id.surface);
        holder = surfaceView.getHolder();
        holder.addCallback(this);
    }

    /**
     * Method to fit checkbox image to setting
     * <p>
     * チェックボックスの画像を設定に合わせるメゾッド
     */
    private void checkBoxUpdata() {
        Settings settings = Settings.getInstance();
        //Change checkbox screen according to whether song repeat or not
        //一曲リピートか否かに合わせてチェックボックス画面変更
        boolean repeatOne = false;
        switch (settings.getRepeatOne()) {
            case ON:
                repeatOne = true;
                break;
            case OFF:
                repeatOne = false;
        }
        repeatOneBox.setChecked(repeatOne);
        //Change checkbox screen according to whether playlist repeat or not
        //プレイリストリピートか否かに合わせてチェックボックス画面変更
        boolean repeatPlaylist = false;
        switch (settings.getRepeatPlaylist()) {
            case ON:
                repeatPlaylist = true;
                break;
            case OFF:
                repeatPlaylist = false;
        }
        repeatPlaylistBox.setChecked(repeatPlaylist);
        //Change checkbox screen according to whether or not screen horizontal fixation
        //画面横固定か否かに合わせてチェックボックス画面変更
        boolean screenLock = false;
        switch (settings.getScreenLock()) {
            case ON:
                screenLock = true;
                break;
            case OFF:
                screenLock = false;
        }
        lockBox.setChecked(screenLock);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged");
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).changeSurfaceHolderAndTitlebar(surfaceView.getHolder(), surfaceView, titleView);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            if (((MainActivity) activity).getmHolder() == surfaceView.getHolder()) {
                //Release now if it is my surfaceHolder that is projected on mediaplayer
                //今mediaplayerに投影されているのが自分のsurfaceHolderだったら解放する
                ((MainActivity) activity).changeSurfaceHolderAndTitlebar(null, null, titleView);
            }
        }
    }


}