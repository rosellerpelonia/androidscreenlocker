package com.example.androidscreenlocker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class LockScreenActivity extends AppCompatActivity {

    private BroadcastReceiver closeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_screen);

        // Make the activity non-dismissible and full-screen
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

        // Set up the lock screen UI
        TextView lockMessage = findViewById(R.id.lockMessage);
        lockMessage.setText("Device Locked\nPlease contact your parent to unlock.");

        // Register a local broadcast receiver to close this activity when unlocked
        closeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("CLOSE_LOCK_SCREEN".equals(intent.getAction())) {
                    finish(); // Close the lock screen activity
                }
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(
                closeReceiver,
                new IntentFilter("CLOSE_LOCK_SCREEN")
        );
    }

    // Prevent back button presses
    @Override
    public void onBackPressed() {
        // Do nothing
    }

    // Disable other keys like Home or Recent Apps
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            return true; // Block these keys
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(closeReceiver);
    }
}
