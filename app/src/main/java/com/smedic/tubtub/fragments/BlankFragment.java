package com.smedic.tubtub.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.smedic.tubtub.MainActivity;
import com.smedic.tubtub.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class BlankFragment extends Fragment {


   /* public BlankFragment() {
        // Required empty public constructor
    }*/


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater,container,savedInstanceState);
        Log.d("kandabashi","blankfragment-shown");
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_blank, container, false);
    }

    public void onDestroy(){
        super.onDestroy();
        ViewPager viewPager=((MainActivity)getActivity()).getViewPager();
        viewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        viewPager.setVisibility(View.VISIBLE);
    }

}
