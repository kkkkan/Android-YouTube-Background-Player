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
import android.widget.TextView;

import com.smedic.tubtub.MainActivity;
import com.smedic.tubtub.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class LandscapeFragment extends Fragment implements SurfaceHolder.Callback {
    private final static String TAG = "LandscapeFragment";
    private SurfaceView surfaceView;
    private TextView titleView;


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
            ((MainActivity) activity).changeSurfaceHolderAndTitlebar(surfaceView, titleView);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
    }
}
