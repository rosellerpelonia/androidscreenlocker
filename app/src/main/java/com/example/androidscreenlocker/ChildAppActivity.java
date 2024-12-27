package com.example.androidscreenlocker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class ChildAppActivity extends AppCompatActivity {

    private TextView txtChildName;
    private Switch switchFacebook, switchInstagram, switchTikTok, switchYouTube;

    private DatabaseReference databaseReference;
    private String parentId, childId;

    // Mapping switches to app package names
    private final Map<Switch, String> appPackageMap = new HashMap<Switch, String>() {{
        put(switchFacebook, "com.facebook.katana");
        put(switchInstagram, "com.instagram.android");
        put(switchTikTok, "com.zhiliaoapp.musically");
        put(switchYouTube, "com.google.android.youtube");
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_app);

        // Initialize UI components
        txtChildName = findViewById(R.id.txtChildName);
        switchFacebook = findViewById(R.id.switchFacebook);
        switchInstagram = findViewById(R.id.switchInstagram);
        switchTikTok = findViewById(R.id.switchTikTok);
        switchYouTube = findViewById(R.id.switchYouTube);

        // Firebase Database Reference
        databaseReference = FirebaseDatabase.getInstance("https://androidscreenlocker-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        // Get parent ID and child ID (passed through intent)
        parentId = getIntent().getStringExtra("parentId");
        childId = getIntent().getStringExtra("childId");
        String childName = getIntent().getStringExtra("childName");

        if (parentId == null || childId == null) {
            Toast.makeText(this, "Error: Missing parent or child ID.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        txtChildName.setText(childName);

        // Load or initialize app lock states
        checkAndInitializeApps();
    }

    // Helper methods to encode/decode Firebase keys
    private String encodeFirebaseKey(String key) {
        return key.replace(".", "_");
    }

    private String decodeFirebaseKey(String encodedKey) {
        return encodedKey.replace("_", ".");
    }

    // Initialize Firebase apps node with package names
    private void checkAndInitializeApps() {
        DatabaseReference childAppsRef = databaseReference
                .child("Users")
                .child(parentId)
                .child("Children")
                .child(childId)
                .child("Apps");

        // Default app states with package names
        Map<String, Boolean> defaultApps = new HashMap<>();
        defaultApps.put(encodeFirebaseKey("com.facebook.katana"), false);
        defaultApps.put(encodeFirebaseKey("com.instagram.android"), false);
        defaultApps.put(encodeFirebaseKey("com.zhiliaoapp.musically"), false);
        defaultApps.put(encodeFirebaseKey("com.google.android.youtube"), false);

        childAppsRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                // Initialize Apps node with default package names
                childAppsRef.setValue(defaultApps).addOnSuccessListener(aVoid ->
                        Log.d("FirebaseInit", "Default apps initialized with encoded package names."));
            } else {
                Log.d("FirebaseInit", "Apps node already exists.");
            }

            // Load states after initialization
            loadAppStates();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to access Apps: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void loadAppStates() {
        DatabaseReference childAppsRef = databaseReference
                .child("Users")
                .child(parentId)
                .child("Children")
                .child(childId)
                .child("Apps");

        // Fetch current app states and update switches
        childAppsRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                switchFacebook.setChecked(getAppState(snapshot, encodeFirebaseKey("com.facebook.katana")));
                switchInstagram.setChecked(getAppState(snapshot, encodeFirebaseKey("com.instagram.android")));
                switchTikTok.setChecked(getAppState(snapshot, encodeFirebaseKey("com.zhiliaoapp.musically")));
                switchYouTube.setChecked(getAppState(snapshot, encodeFirebaseKey("com.google.android.youtube")));
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load app states: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );

        setSwitchListeners();
    }

    private boolean getAppState(com.google.firebase.database.DataSnapshot snapshot, String packageName) {
        Boolean state = snapshot.child(packageName).getValue(Boolean.class);
        return state != null ? state : false;
    }

    private void setSwitchListeners() {
        switchFacebook.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateAppLockState("com.facebook.katana", isChecked));
        switchInstagram.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateAppLockState("com.instagram.android", isChecked));
        switchTikTok.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateAppLockState("com.zhiliaoapp.musically", isChecked));
        switchYouTube.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateAppLockState("com.google.android.youtube", isChecked));
    }

    private void updateAppLockState(String packageName, boolean isLocked) {
        String encodedKey = encodeFirebaseKey(packageName);
        DatabaseReference appRef = databaseReference
                .child("Users")
                .child(parentId)
                .child("Children")
                .child(childId)
                .child("Apps")
                .child(encodedKey);

        appRef.get().addOnSuccessListener(snapshot -> {
            Boolean currentState = snapshot.getValue(Boolean.class);
            if (currentState == null || currentState != isLocked) {
                appRef.setValue(isLocked).addOnSuccessListener(aVoid -> {
                    Log.d("FirebaseWrite", packageName + " updated to: " + isLocked);
                    String status = isLocked ? "locked" : "unlocked";
                    Toast.makeText(this, packageName + " is now " + status + ".", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    Log.e("FirebaseWrite", "Error updating " + packageName + ": " + e.getMessage());
                    Toast.makeText(this, "Failed to update app state: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).addOnFailureListener(e ->
                Log.e("FirebaseWrite", "Error fetching current state for " + packageName + ": " + e.getMessage()));
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Start AppMonitorService with parentId and childId
        Intent serviceIntent = new Intent(this, AppMonitorService.class);
        serviceIntent.putExtra("parentId", parentId);
        serviceIntent.putExtra("childId", childId);
        startService(serviceIntent);
    }
}
