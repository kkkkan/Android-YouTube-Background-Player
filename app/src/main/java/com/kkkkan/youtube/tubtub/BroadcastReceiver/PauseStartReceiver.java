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
package com.kkkkan.youtube.tubtub.BroadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.kkkkan.youtube.BuildConfig;
import com.kkkkan.youtube.R;


public class PauseStartReceiver extends BroadcastReceiver {
    static public String ACTION = " PauseStartReceiver";
    static private String TAG = "PauseStartReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "push");
        String filterKey = context.getString(R.string.broadcast_receiver_filter_key);
        String filter = intent.getStringExtra(filterKey);
        Log.d(TAG, BuildConfig.APPLICATION_ID + " " + filter + " " + context.getString(R.string.broadcast_receiver_filter));
        //違うビルドタイプのAPKが同一端末で動いていたときに正しいAPKのみ反応できるようハンドリングする。
        //これでもまだ、発したbroadcastを違うビルドタイプのAPKが受信できてしまうので、
        // サービス内の受信側でも正しく反応できるようハンドリングする必要がある。
        if (filter == null || !filter.equals(context.getString(R.string.broadcast_receiver_filter))) {
            //このクラスは明示的intentでbroadcastを受けているはずなので、ここには来ないはず
            return;
        }
        Intent intent1 = new Intent(ACTION);
        intent1.putExtra(filterKey, filter);
        context.sendBroadcast(intent1);
    }

}
