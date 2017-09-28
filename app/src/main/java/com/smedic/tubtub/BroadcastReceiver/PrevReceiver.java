package com.smedic.tubtub.BroadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by admin on 2017/09/13.
 */

public class PrevReceiver extends BroadcastReceiver {
    static private String TAG=" PrevReceiver";
    static public String ACTION=" PrevReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG,"push");
        Intent intent1=new Intent(ACTION);
        context.sendBroadcast(intent1);
    }
}
