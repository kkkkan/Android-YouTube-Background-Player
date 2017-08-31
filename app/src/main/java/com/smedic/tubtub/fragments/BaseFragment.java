package com.smedic.tubtub.fragments;

import android.content.Intent;
import android.support.v4.app.Fragment;

/**
 * Created by smedic on 9.2.17..
 */

public class BaseFragment extends Fragment {

    protected void share(String url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, url);
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Check out this song!");
        startActivity(Intent.createChooser(intent, "Share"));
    }

    /*ViewPager時は非選択時にあらかじめページ生成されているので選択された（ユーザーが見えるようになる）
    と同時に処理開始したいときはsetUserVisibleHintを使う*/
    @Override
    public void setUserVisibleHint(boolean visible) {
        super.setUserVisibleHint(visible);
        if (visible && isResumed()) {
            //Only manually call onResume if fragment is already visible
            //Otherwise allow natural fragment lifecycle to call onResume
            onResume();
        }
    }

}
