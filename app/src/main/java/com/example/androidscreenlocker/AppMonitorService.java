package com.example.androidscreenlocker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashSet;
import java.util.Set;

public class AppMonitorService extends Service {

    private static final String TAG = "AppMonitorService";
    private static final int MONITOR_INTERVAL = 3000; // 3 second
    private static final String CHANNEL_ID = "service_channel";

    private DatabaseReference appsRef;
    private Set<String> lockedApps = new HashSet<>();
    private ForegroundAppChecker appChecker;
    private Handler handler;
    private Runnable monitorRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created.");
        appChecker = new ForegroundAppChecker(this);
        handler = new Handler();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            startForeground(1, buildNotification());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Log.e(TAG, "FirebaseAuth user is null. Service stopping.");
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent == null || intent.getStringExtra("parentId") == null || intent.getStringExtra("childId") == null) {
            Log.e(TAG, "Invalid or missing intent extras. Stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        String parentId = intent.getStringExtra("parentId");
        String childId = intent.getStringExtra("childId");
        Log.d(TAG, "Service started with parentId: " + parentId + ", childId: " + childId);

        fetchLockedApps(parentId, childId);
        startMonitoring();

        return START_STICKY;
    }

    private void fetchLockedApps(String parentId, String childId) {
        String dbPath = "Users/" + parentId + "/Children/" + childId + "/Apps";
        appsRef = FirebaseDatabase.getInstance("https://androidscreenlocker-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference(dbPath);

        Log.d(TAG, "Fetching locked apps from path: " + dbPath);

        appsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lockedApps.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                        String packageName = appSnapshot.getKey(); // Directly use the package name
                        Boolean isLocked = appSnapshot.getValue(Boolean.class);

                        if (Boolean.TRUE.equals(isLocked)) {
                            lockedApps.add(packageName);
                            Log.d(TAG, "Locked app added: " + packageName);
                        }
                    }
                } else {
                    Log.d(TAG, "No locked apps found in database.");
                }

                Log.d(TAG, "Locked Apps List: " + lockedApps); // Debugging: Print the locked apps
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching locked apps: " + error.getMessage());
            }
        });
    }


    private void startMonitoring() {
        Log.d(TAG, "Starting app monitoring...");

        monitorRunnable = new Runnable() {
            @Override
            public void run() {
                String currentApp = appChecker.getForegroundApp(); // Define currentApp here

                if (currentApp != null) {
                    Log.d(TAG, "Foreground App Detected: " + currentApp);

                    if (lockedApps.contains(currentApp)) {
                        Log.d(TAG, "Locked App Detected: " + currentApp);
                        launchLockScreen();
                    } else {
                        Log.d(TAG, "No match in locked apps. Current foreground app: " + currentApp);
                    }
                } else {
                    Log.d(TAG, "Unable to detect the current foreground app.");
                }

                handler.postDelayed(this, MONITOR_INTERVAL);
            }
        };

        handler.post(monitorRunnable);
    }



    private void launchLockScreen() {
        Log.d(TAG, "Launching LockActivity...");
        try {
            Intent lockIntent = new Intent(this, LockActivity.class);
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(lockIntent);
            Log.d(TAG, "LockActivity launched successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch LockActivity: " + e.getMessage(), e);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && monitorRunnable != null) {
            handler.removeCallbacks(monitorRunnable);
            Log.d(TAG, "Monitoring stopped.");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "App Monitor Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("App Monitor Service")
                .setContentText("Monitoring running apps...")
                .setSmallIcon(R.drawable.ic_lock)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
