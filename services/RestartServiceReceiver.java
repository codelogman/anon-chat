package com.node.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.node.anonchat.AppSocketListener;

public class RestartServiceReceiver extends BroadcastReceiver
{

    private static final String TAG = "RestartServiceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "onReceive");
        AppSocketListener.getInstance().initialize();
                Toast.makeText(context, "service restarted", Toast.LENGTH_SHORT).show();
       }

}