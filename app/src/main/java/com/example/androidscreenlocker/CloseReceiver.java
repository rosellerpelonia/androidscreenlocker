package com.example.androidscreenlocker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class CloseReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Check the action of the received intent
        if ("com.example.androidscreenlocker.CLOSE_LOCK_SCREEN".equals(intent.getAction())) {
            // Perform the action to close the lock screen
            Toast.makeText(context, "Closing Lock Screen", Toast.LENGTH_SHORT).show();
            // Add your lock screen close logic here
        }
    }
}
