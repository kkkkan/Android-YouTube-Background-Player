package com.smedic.tubtub.BroadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by admin on 2017/09/13.
 */

public class NextReceiver extends BroadcastReceiver {
    static private String TAG = "NextReceiver";
    static public String ACTION = "NextReceiver";


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "push");
        Intent intent1 = new Intent(ACTION);
        context.sendBroadcast(intent1);
    }
}
