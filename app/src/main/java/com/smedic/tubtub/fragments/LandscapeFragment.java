package com.smedic.tubtub.fragments;


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

import com.smedic.tubtub.MainActivity;
import com.smedic.tubtub.R;
import com.smedic.tubtub.Settings;
import com.smedic.tubtub.interfaces.TitlebarListener;

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

    /*
    * LandscapeFragmentの新しいインスタンスを作るときは必ず
    * このメゾッドで作ること*/
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
     * surfaceViewの取得しなおしとそれに伴う
     * holderのインスタンスの作り直し
     */
    private void makeSurfaceView() {
        surfaceView = (SurfaceView) getView().findViewById(R.id.surface);
        holder = surfaceView.getHolder();
        holder.addCallback(this);
    }

    /**
     * チェックボックスの画像を設定に合わせるメゾッド
     */
    private void checkBoxUpdata() {
        Settings settings = Settings.getInstance();
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
                //今mediaplayerに投影されているのが自分のsurfaceHolderだったら解放する
                ((MainActivity) activity).changeSurfaceHolderAndTitlebar(null, null, titleView);
            }
        }
    }


}
