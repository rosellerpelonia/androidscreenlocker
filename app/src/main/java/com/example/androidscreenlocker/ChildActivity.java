package com.example.androidscreenlocker;

import android.content.Intent;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ChildActivity extends AppCompatActivity {

    private TextView childNameText, statusText;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private String currentChildUID;

    private ValueEventListener lockStatusListener;
    private boolean isLocked = false;

    private String parentKey, childKey; // Keys to identify the parent and child in Firebase

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child);

        // Initialize views and Firebase references
        childNameText = findViewById(R.id.childName);
        statusText = findViewById(R.id.statusText);
        firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            currentChildUID = currentUser.getUid(); // Get authenticated child's UID
            fetchChildData(); // Fetch dynamic data
        } else {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void fetchChildData() {
        // Reference to the 'Users' node
        databaseReference = FirebaseDatabase.getInstance(
                        "https://androidscreenlocker-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Users");

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot parentSnapshot : snapshot.getChildren()) {
                    DataSnapshot childrenSnapshot = parentSnapshot.child("Children");

                    for (DataSnapshot childSnapshot : childrenSnapshot.getChildren()) {
                        if (childSnapshot.getKey().equals(currentChildUID)) {
                            String childName = childSnapshot.child("name").getValue(String.class);
                            childNameText.setText((childName != null ? childName : "Unknown"));

                            // Assign the parentKey and childKey dynamically
                            parentKey = parentSnapshot.getKey();
                            childKey = childSnapshot.getKey();

                            // Monitor lock and app statuses
                            monitorLockStatus(parentKey, childKey);
                            monitorAppStatuses(parentKey, childKey);

                            // Start AppMonitorService
                            startAppMonitorService();
                            return;
                        }
                    }
                }
                Toast.makeText(ChildActivity.this, "Child data not found!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChildActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void monitorLockStatus(String parentKey, String childKey) {
        DatabaseReference lockStatusRef = databaseReference.child(parentKey).child("Children").child(childKey).child("lock");

        lockStatusListener = lockStatusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isLocked = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                if (isLocked) {
                    statusText.setText("Status: Locked");
                    launchLockScreen();
                } else {
                    statusText.setText("Status: Unlocked");
                    closeLockScreen();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChildActivity.this, "Failed to monitor lock status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void monitorAppStatuses(String parentKey, String childKey) {
        DatabaseReference appsRef = databaseReference.child(parentKey).child("Children").child(childKey).child("Apps");

        appsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                    String packageName = appSnapshot.getKey(); // App's package name
                    boolean isBlocked = Boolean.TRUE.equals(appSnapshot.getValue(Boolean.class)); // Block status

                    // Corrected condition: Block app when 'isBlocked' is true
                    if (isBlocked) {
                        blockApp(packageName);
                    } else {
                        unblockApp(packageName);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChildActivity.this, "Failed to monitor app statuses", Toast.LENGTH_SHORT).show();
            }
        });
    }




    private void blockApp(String packageName) {
        ForegroundAppChecker appChecker = new ForegroundAppChecker(this);

        if (appChecker.isAppRunning(packageName)) {
            launchLockScreen(); // Launch lock screen if app is running
            Toast.makeText(this, packageName + " is blocked!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, packageName + " is not running but blocked!", Toast.LENGTH_SHORT).show();
        }
    }


    private void unblockApp(String packageName) {
        Toast.makeText(this, packageName + " is unblocked!", Toast.LENGTH_SHORT).show();
        // Optionally close the lock screen if needed
        closeLockScreen();
    }


    private void launchLockScreen() {
        Intent lockScreenIntent = new Intent(ChildActivity.this, LockActivity.class);
        lockScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(lockScreenIntent);
    }

    private void closeLockScreen() {
        Intent closeIntent = new Intent("CLOSE_LOCK_SCREEN");
        LocalBroadcastManager.getInstance(this).sendBroadcast(closeIntent);
    }

    private void startAppMonitorService() {
        Intent monitorServiceIntent = new Intent(this, AppMonitorService.class);
        monitorServiceIntent.putExtra("parentId", parentKey);
        monitorServiceIntent.putExtra("childId", childKey);
        startService(monitorServiceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lockStatusListener != null) {
            databaseReference.removeEventListener(lockStatusListener);
        }

        Intent monitorServiceIntent = new Intent(this, AppMonitorService.class);
        stopService(monitorServiceIntent);
    }
}
