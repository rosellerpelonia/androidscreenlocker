package com.example.androidscreenlocker;

import android.app.admin.DevicePolicyManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityManager;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.util.List;

public class EnableAdminActivity extends AppCompatActivity {

    private DevicePolicyManager devicePolicyManager;
    private ComponentName compName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enable_admin);

        // Initialize DevicePolicyManager and ComponentName
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        compName = new ComponentName(this, AdminReceiver.class);

        // Check if all permissions (Device Admin, Accessibility, and Usage Access) are enabled
        if (isDeviceAdminEnabled() && isAccessibilityServiceEnabled() && isUsageAccessGranted()) {
            redirectToChildActivity();
            return;
        }

        // Set up button to enable Device Admin
        Button enableAdminButton = findViewById(R.id.btnEnableAdmin);
        enableAdminButton.setOnClickListener(v -> {
            if (!isDeviceAdminEnabled()) {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "This permission is required to lock the device when needed.");
                startActivityForResult(intent, 101);
            } else {
                Toast.makeText(this, "Device Admin is already enabled!", Toast.LENGTH_SHORT).show();
                checkAccessibilityService();
            }
        });

        // Set up button to enable Accessibility Service
        Button enableAccessibilityButton = findViewById(R.id.btnEnableAccess);
        enableAccessibilityButton.setOnClickListener(v -> checkAccessibilityService());

        // Set up button to enable Usage Access
        Button enableUsageAccessButton = findViewById(R.id.btnEnableUsageAccess);
        enableUsageAccessButton.setOnClickListener(v -> checkUsageAccess());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check the result of enabling device admin
        if (requestCode == 101) {
            if (isDeviceAdminEnabled()) {
                Toast.makeText(this, "Device Admin enabled successfully!", Toast.LENGTH_SHORT).show();
                checkAccessibilityService(); // Check Accessibility Service next
            } else {
                Toast.makeText(this, "Device Admin not enabled. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkAccessibilityService() {
        if (!isAccessibilityServiceEnabled()) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Enable Accessibility Service to activate lock screen", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Accessibility Service is already enabled!", Toast.LENGTH_SHORT).show();
            checkUsageAccess();
        }
    }

    private void checkUsageAccess() {
        if (!isUsageAccessGranted()) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Enable Usage Access for app monitoring", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Usage Access is already granted!", Toast.LENGTH_SHORT).show();
            redirectToChildActivity();
        }
    }

    private boolean isDeviceAdminEnabled() {
        return devicePolicyManager.isAdminActive(compName);
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo service : enabledServices) {
            if (service.getId().contains(getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isUsageAccessGranted() {
        try {
            UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            long currentTime = System.currentTimeMillis();
            // Query the stats for the last minute
            List<UsageStats> stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, currentTime - 1000 * 60, currentTime);
            return stats != null && !stats.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }


    private void redirectToChildActivity() {
        Intent intent = new Intent(this, ChildActivity.class);
        startActivity(intent);
        finish();
    }
}
