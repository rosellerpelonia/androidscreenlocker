package com.example.androidscreenlocker;

import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.NonNull;

import com.google.firebase.database.*;

public class LockStatusService extends Service {

    private DatabaseReference lockStatusRef;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName deviceAdmin;

    @Override
    public void onCreate() {
        super.onCreate();
        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
//        deviceAdmin = new ComponentName(this, MyDeviceAdminReceiver.class);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String parentKey = intent.getStringExtra("PARENT_KEY");
        String childKey = intent.getStringExtra("CHILD_KEY");

        lockStatusRef = FirebaseDatabase.getInstance("https://androidscreenlocker-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Users")
                .child(parentKey)
                .child("Children")
                .child(childKey)
                .child("lock");

        // Listen for changes in the lock status
        lockStatusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isLocked = snapshot.getValue(Boolean.class);
                if (isLocked != null && isLocked && devicePolicyManager.isAdminActive(deviceAdmin)) {
                    // Lock the device
                    devicePolicyManager.lockNow();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Log error
            }
        });

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
