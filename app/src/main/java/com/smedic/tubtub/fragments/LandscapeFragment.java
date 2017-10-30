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
import com.smedic.tubtub.interfaces.TitlebarListener;

/**
 * A simple {@link Fragment} subclass.
 */
public class LandscapeFragment extends Fragment implements SurfaceHolder.Callback {
    private final static String TAG = "LandscapeFragment";
    private SurfaceView surfaceView;
    private TextView titleView;
    private CheckBox repeatBox;
    private CheckBox lockBox;
    private MainActivity.Repeat repeat;
    private TitlebarListener titlebarListener;

    /*
    * LandscapeFragmentの新しいインスタンスを作るときは必ず
    * このメゾッドで作ること*/
    static public LandscapeFragment getNewLandscapeFragment(TitlebarListener titlebarListener, MainActivity.Repeat repeat) {
        LandscapeFragment fragment = new LandscapeFragment();
        fragment.repeat = repeat;
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
        repeatBox = (CheckBox) view.findViewById(R.id.repeat_box);
        //repeatするかどうかは縦画面から横画面にいっても保持
        boolean bool = false;
        switch (repeat) {
            case ON:
                bool = true;
                break;
            case OFF:
                bool = false;
                break;
        }
        repeatBox.setChecked(bool);

        //チェックボックスにクリックリスナー設置
        lockBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                titlebarListener.lockCheckListener();
            }
        });
        repeatBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                titlebarListener.repeatCheckListener();
            }
        });
        return view;
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
            ((MainActivity) activity).changeSurfaceHolderAndTitlebar(null, null, titleView);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }
}
