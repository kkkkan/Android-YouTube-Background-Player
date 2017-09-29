package com.smedic.tubtub.fragments;


import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.smedic.tubtub.MainActivity;
import com.smedic.tubtub.R;

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

    /*tabの可視化とタッチの復活。*/
    public void onDestroy() {
        super.onDestroy();
        TabLayout tabLayout = ((MainActivity) getActivity()).getTabLayout();
        tabLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        tabLayout.setVisibility(View.VISIBLE);
    }
}
