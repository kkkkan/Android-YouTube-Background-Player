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

import android.content.Intent;
import android.support.v4.app.Fragment;


public class BaseFragment extends Fragment {

    protected void share(String url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, url);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Check out this song!");
        startActivity(Intent.createChooser(intent, "Share"));
    }

    /**
     * When using ViewPager, setUserVisibleHint is used when you want to start processing at the same time as it was selected (as the user can see it)
     * because the page was generated beforehand when not selected
     * <p>
     * ViewPager時は非選択時にあらかじめページ生成されているので選択された（ユーザーが見えるようになる）
     * と同時に処理開始したいときはsetUserVisibleHintを使う
     */
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
