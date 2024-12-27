package com.example.androidscreenlocker;

import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class ForegroundAppChecker {

    private final Context context;

    public ForegroundAppChecker(Context context) {
        this.context = context;
    }

    // Get the currently running foreground app
    public String getForegroundApp() {
        String currentApp = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

            if (usageStatsManager == null) {
                Log.e("ForegroundAppChecker", "UsageStatsManager is null!");
                return null;
            }

            long currentTime = System.currentTimeMillis();

            List<UsageStats> usageStats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    currentTime - 1000 * 60 * 5, // Last 5 minutes
                    currentTime
            );

            if (usageStats != null && !usageStats.isEmpty()) {
                SortedMap<Long, UsageStats> sortedStats = new TreeMap<>();
                for (UsageStats stats : usageStats) {
                    if (stats.getLastTimeUsed() > 0) {
                        sortedStats.put(stats.getLastTimeUsed(), stats);
                    }
                }

                if (!sortedStats.isEmpty()) {
                    currentApp = sortedStats.get(sortedStats.lastKey()).getPackageName();
                } else {
                    Log.w("ForegroundAppChecker", "No recently used apps found.");
                }
            } else {
                Log.w("ForegroundAppChecker", "No UsageStats available. Ensure permissions are granted.");
            }
        } else {
            // Fallback for older Android versions
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

            if (activityManager != null) {
                List<ActivityManager.RunningTaskInfo> taskInfo = activityManager.getRunningTasks(1);
                if (taskInfo != null && !taskInfo.isEmpty()) {
                    currentApp = taskInfo.get(0).topActivity.getPackageName();
                } else {
                    Log.w("ForegroundAppChecker", "No running tasks found!");
                }
            } else {
                Log.e("ForegroundAppChecker", "ActivityManager is null!");
            }
        }

        Log.d("ForegroundAppChecker", "Detected Foreground App: " + currentApp);
        return currentApp;
    }

    // Check if a specific app is running in the foreground
    public void checkAndLockApp(String packageName) {
        String currentApp = getForegroundApp();
        Log.d("ForegroundAppChecker", "Comparing: " + packageName + " with " + currentApp);

        if (currentApp != null && currentApp.equals(packageName)) {
            Log.d("ForegroundAppChecker", "Locked App Detected! Launching LockActivity...");
            Intent intent = new Intent(context, LockActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        }
    }

    // Check if a specific app is running
    public boolean isAppRunning(String packageName) {
        String currentApp = getForegroundApp();
        return currentApp != null && currentApp.equals(packageName);
    }
}
