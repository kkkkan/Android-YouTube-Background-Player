package com.smedic.tubtub.BroadcastReceiver;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.RemoteViews;

import com.smedic.tubtub.MainActivity;
import com.smedic.tubtub.R;

/**
 * Created by admin on 2017/09/13.
 */

public class NextReceiver extends BroadcastReceiver {
    static private String TAG="NextReceiver";
    static public String ACTION="NextReceiver";


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG,"push");
        Intent intent1=new Intent(ACTION);
        context.sendBroadcast(intent1);
    }
}
