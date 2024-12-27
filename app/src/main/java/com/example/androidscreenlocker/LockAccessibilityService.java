package com.example.androidscreenlocker;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashSet;
import java.util.Set;

public class LockAccessibilityService extends AccessibilityService {

    private boolean isPhoneLocked = false;
    private Set<String> lockedApps = new HashSet<>(); // Holds locked app package names
    private DatabaseReference lockRef, appsRef;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (isPhoneLocked || !lockedApps.isEmpty()) {
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                String currentPackageName = event.getPackageName() != null ? event.getPackageName().toString() : "";

                // Check if phone is locked or the app is in the lockedApps set
                if (isPhoneLocked || lockedApps.contains(currentPackageName)) {
                    launchLockActivity();
                }
            }
        }
    }

    private void launchLockActivity() {
        Intent lockIntent = new Intent(this, LockActivity.class);
        lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(lockIntent);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        String childUID = getSharedPreferences("ChildPrefs", MODE_PRIVATE).getString("childUID", null);
        if (childUID != null) {
            FirebaseDatabase database = FirebaseDatabase.getInstance(
                    "https://androidscreenlocker-default-rtdb.asia-southeast1.firebasedatabase.app");

            // Reference to phone lock status
            lockRef = database.getReference("Users").child("yGmqCborArRX4mAoDe20fHjZADR2") // Replace with dynamic parent ID if needed
                    .child("Children").child(childUID).child("lock");

            // Reference to locked apps
            appsRef = database.getReference("Users").child("yGmqCborArRX4mAoDe20fHjZADR2")
                    .child("Children").child(childUID).child("Apps");

            // Listen for phone lock status
            lockRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    isPhoneLocked = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Handle errors
                }
            });

            // Listen for locked apps
            appsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    lockedApps.clear();
                    for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                        Boolean isLocked = appSnapshot.getValue(Boolean.class);
                        if (Boolean.TRUE.equals(isLocked)) {
                            lockedApps.add(appSnapshot.getKey()); // Add package name to lockedApps set
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Handle errors
                }
            });
        }
    }

    @Override
    public void onInterrupt() {
        // Required method
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (lockRef != null) lockRef.removeEventListener((ValueEventListener) this);
        if (appsRef != null) appsRef.removeEventListener((ValueEventListener) this);
    }
}
