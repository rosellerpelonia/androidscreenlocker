package com.example.androidscreenlocker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ParentActivity extends AppCompatActivity {

    private TextView parentNameText;
    private LinearLayout childListContainer;

    // Firebase Database reference
    private DatabaseReference databaseReference;
    private String parentUniqueId;
    private static final String TAG = "ParentActivity";
    private ImageButton btnBack, btnSettings;
    private TextView txtTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent);

        // Initialize Firebase Database
        databaseReference = FirebaseDatabase.getInstance("https://androidscreenlocker-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();

        // Retrieve the parentUniqueId from Firebase Authentication
        parentUniqueId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Bind UI components
        parentNameText = findViewById(R.id.parentName);
        childListContainer = findViewById(R.id.childListContainer);

        // Get parent name from intent
        String parentName = getIntent().getStringExtra("parentName");

        // Display parent's name
        if (parentName != null) {
            parentNameText.setText(parentName);
        } else {
            parentNameText.setText("Unknown Parent");
        }
// Retrieve the parentUniqueId from Firebase Authentication
        parentUniqueId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        // Log the parentUniqueId to verify its value
        if (parentUniqueId != null) {
            Log.d(TAG, "Parent Unique ID: " + parentUniqueId);
        } else {
            Log.e(TAG, "Failed to fetch Parent Unique ID. User may not be logged in.");
        }
        // Fetch children dynamically from Firebase
        fetchChildrenFromFirebase();


        // Bind UI elements
        btnBack = findViewById(R.id.btnBack);
        btnSettings = findViewById(R.id.btnSettings);
        txtTitle = findViewById(R.id.txtTitle);

        // Set toolbar title dynamically if needed
//        txtTitle.setText("Home");

        // Back Button Click Listener
        btnBack.setOnClickListener(v -> onBackPressed());

        // Settings Button Click Listener
        btnSettings.setOnClickListener(v -> showSettingsMenu(v));
    }

    private void showSettingsMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view, 0, 0, R.style.CustomPopupMenu);
        popupMenu.getMenuInflater().inflate(R.menu.settings_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_about) {
                // Navigate to AboutActivity
                Intent aboutIntent = new Intent(ParentActivity.this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;
            } else if (id == R.id.menu_logout) {
                // Clear saved credentials
                SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();

                // Sign out the user
                FirebaseAuth.getInstance().signOut();

                // Handle logout
                Toast.makeText(ParentActivity.this, "Logged out", Toast.LENGTH_SHORT).show();

                // Redirect to login screen
                Intent logoutIntent = new Intent(ParentActivity.this, MainActivity.class);
                logoutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(logoutIntent);
                finish();
                return true;
            }

            return false;
        });


        popupMenu.show();
    }
    private void fetchChildrenFromFirebase() {
        if (parentUniqueId == null) {
            showNoChildrenMessage("Parent ID not found.");
            Log.e(TAG, "Cannot fetch children because Parent Unique ID is null.");
            return;
        }

        Log.d(TAG, "Fetching children for Parent ID: " + parentUniqueId);

        // Reference to the parent's children
        DatabaseReference childrenRef = databaseReference.child("Users").child(parentUniqueId).child("Children");

        // Listen for data changes
        childrenRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DataSnapshot childrenSnapshot = task.getResult();
                if (childrenSnapshot.exists()) {
                    Log.d(TAG, "Children data retrieved successfully.");
                    for (DataSnapshot child : childrenSnapshot.getChildren()) {
                        // Fetch the unique ID and details of each child
                        String childId = child.getKey();
                        String childName = child.child("name").getValue(String.class);
                        Boolean isLocked = child.child("lock").getValue(Boolean.class);

                        Log.d(TAG, "Child ID: " + childId + ", Name: " + childName + ", Locked: " + isLocked);

                        // Add child to the UI
                        addChildToUI(childId, childName, isLocked);
                    }
                } else {
                    Log.w(TAG, "No children found for Parent ID: " + parentUniqueId);
                    showNoChildrenMessage("No children found under this parent.");
                }
            } else {
                Log.e(TAG, "Failed to fetch children: " +
                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                showNoChildrenMessage("Failed to fetch children: " +
                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
            }
        });
    }

    private void addChildToUI(String childId, String childName, Boolean isLocked) {
        // Validate childId
        if (childId == null || childId.isEmpty()) {
            Toast.makeText(this, "Child ID is missing or invalid.", Toast.LENGTH_SHORT).show();
            return;
        }

        View childView = getLayoutInflater().inflate(R.layout.child_item, childListContainer, false);

        // Bind child details to the view
        TextView txtChildName = childView.findViewById(R.id.txtChildName);
        CircleImageView imgChild = childView.findViewById(R.id.imgChild);
        Switch switchLockPhone = childView.findViewById(R.id.switchLockPhone);

        // Ensure child name and set a default if null
        String safeChildName = (childName != null) ? childName : "Unknown Child";
        txtChildName.setText(safeChildName);
        imgChild.setImageResource(R.drawable.children); // Default child image

        // Set the lock switch state
        switchLockPhone.setChecked(isLocked != null && isLocked);

        // Handle switch toggle to update lock status in Firebase
        switchLockPhone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                lockChildDevice(childId);
            } else {
                unlockChildDevice(childId);
            }
        });

        // Check and insert the Apps node if it doesn't exist
        DatabaseReference appsRef = FirebaseDatabase.getInstance("https://androidscreenlocker-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Users")
                .child(parentUniqueId)
                .child("Children")
                .child(childId)
                .child("Apps");

        appsRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                // Insert default Apps node
                Map<String, Boolean> defaultApps = new HashMap<>();
                defaultApps.put("com_facebook_katana", false); // Facebook
                defaultApps.put("com_instagram_android", false); // Instagram
                defaultApps.put("com_zhiliaoapp_musically", false); // TikTok
                defaultApps.put("com_google_android_youtube", false); // YouTube

                appsRef.setValue(defaultApps)
                        .addOnSuccessListener(aVoid ->
                                Log.d("AppsInit", "Default Apps node initialized for child: " + childId)
                        )
                        .addOnFailureListener(e ->
                                Log.e("AppsInit", "Failed to initialize Apps node: " + e.getMessage())
                        );
            }
        }).addOnFailureListener(e ->
                Log.e("AppsInit", "Error checking Apps node: " + e.getMessage())
        );

        // Navigate to ChildAppActivity on click
        childView.setOnClickListener(v -> {
            Log.d("ChildClick", "Navigating to ChildAppActivity with Child ID: " + childId + " and Name: " + safeChildName + " and Parent ID: " + parentUniqueId);

            // Start the ChildAppActivity with the child ID and name
            Intent intent = new Intent(ParentActivity.this, ChildAppActivity.class);
            intent.putExtra("parentId", parentUniqueId);
            intent.putExtra("childId", childId);
            intent.putExtra("childName", safeChildName);

            // Start activity
            startActivity(intent);
        });

        // Add the child view to the container
        childListContainer.addView(childView);
    }




    private void lockChildDevice(String childId) {
        updateLockStatusInFirebase(childId, true);
    }

    private void unlockChildDevice(String childId) {
        updateLockStatusInFirebase(childId, false);
    }

    private void showNoChildrenMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        TextView noChildMessage = new TextView(this);
        noChildMessage.setText(message);
        noChildMessage.setTextColor(getResources().getColor(R.color.flatui_wet_asphalt));
        noChildMessage.setTextSize(16);
        childListContainer.addView(noChildMessage);
    }

    private void updateLockStatusInFirebase(String childId, boolean isLocked) {
        if (parentUniqueId == null || childId == null || childId.isEmpty()) {
            Toast.makeText(this, "Unable to identify parent or child in Firebase.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference childRef = databaseReference.child("Users")
                .child(parentUniqueId)
                .child("Children")
                .child(childId);

        childRef.child("lock").setValue(isLocked)
                .addOnSuccessListener(aVoid -> {
                    if (isLocked) {
                        Toast.makeText(this, "Child's device locked.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Child's device unlocked.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update lock status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


}
