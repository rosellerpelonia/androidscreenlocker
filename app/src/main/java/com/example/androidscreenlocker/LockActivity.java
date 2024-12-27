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

public class LockActivity extends AppCompatActivity {

    private BroadcastReceiver closeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_screen);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        TextView lockMessage = findViewById(R.id.lockMessage);
        lockMessage.setText("Device Locked\nPlease contact your parent to unlock.");

        closeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("CLOSE_LOCK_SCREEN".equals(intent.getAction())) {
                    finish();
                }
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(
                closeReceiver,
                new IntentFilter("CLOSE_LOCK_SCREEN")
        );
    }

    @Override
    public void onBackPressed() {
        // Do nothing to block back button
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_HOME ||
                keyCode == KeyEvent.KEYCODE_APP_SWITCH ||
                keyCode == KeyEvent.KEYCODE_BACK) {
            return true; // Block these keys
        }
        return super.onKeyDown(keyCode, event);
    }




}
