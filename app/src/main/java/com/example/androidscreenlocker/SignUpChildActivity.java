package com.example.androidscreenlocker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SignUpChildActivity extends AppCompatActivity {

    private TextInputEditText nameInput, emailInput, passwordInput, emailParentInput;
    private TextInputLayout nameInputLayout, emailInputLayout, passwordInputLayout, emailParentInputLayout;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_child);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Bind UI elements
        emailParentInputLayout = findViewById(R.id.emailParentInputLayout);
        emailParentInput = findViewById(R.id.emailParentInput);
        nameInputLayout = findViewById(R.id.nameInputLayout);
        nameInput = findViewById(R.id.nameInput);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        passwordInput = findViewById(R.id.passwordInput);
        Button signupButton = findViewById(R.id.signupButton);
        TextView signUpText = findViewById(R.id.signUpText);

        // Set up click listeners
        signupButton.setOnClickListener(view -> registerChildUser());

        signUpText.setOnClickListener(view -> finish());  // Navigate back to parent activity
    }

    /**
     * Handle child user registration
     */
    private void registerChildUser() {
        String parentEmail = emailParentInput.getText().toString().trim();
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Validation checks
        if (TextUtils.isEmpty(parentEmail)) {
            emailParentInputLayout.setError("Parent email is required!");
            return;
        } else {
            emailParentInputLayout.setError(null);
        }

        if (TextUtils.isEmpty(name)) {
            nameInputLayout.setError("Name is required!");
            return;
        } else {
            nameInputLayout.setError(null);
        }

        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Email is required!");
            return;
        } else {
            emailInputLayout.setError(null);
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordInputLayout.setError("Password must be at least 6 characters!");
            return;
        } else {
            passwordInputLayout.setError(null);
        }

        // Check if the parent email exists in Firebase Database
        checkParentEmail(parentEmail, name, email, password);
    }

    /**
     * Check if parent email exists in the database
     */
    private void checkParentEmail(String parentEmail, String childName, String childEmail, String childPassword) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance("https://androidscreenlocker-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Users");

        databaseReference.orderByChild("email").equalTo(parentEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Parent email found, now register the child under this parent
                    String parentUserId = dataSnapshot.getChildren().iterator().next().getKey(); // Get the parent user ID
                    registerChildInFirebase(parentUserId, childName, childEmail, childPassword);
                } else {
                    // Parent email not found
                    Toast.makeText(SignUpChildActivity.this, "Parent email not found!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(SignUpChildActivity.this, "Error checking parent email: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Register child in Firebase Authentication and Database
     */
    private void registerChildInFirebase(String parentUserId, String childName, String childEmail, String childPassword) {
        firebaseAuth.createUserWithEmailAndPassword(childEmail, childPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Child user successfully created, get the user ID
                        String childUserId = firebaseAuth.getCurrentUser().getUid();

                        // Save child details in the Firebase database under the parent
                        saveChildDetailsToDatabase(parentUserId, childUserId, childName, childEmail);
                    } else {
                        Toast.makeText(SignUpChildActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Save child details to Firebase Realtime Database under the parent
     */
    private void saveChildDetailsToDatabase(String parentUserId, String childUserId, String childName, String childEmail) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance("https://androidscreenlocker-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Users").child(parentUserId).child("Children").child(childUserId);

        // Add "lock: false" for the child account
        ChildUser childUser = new ChildUser(childName, childEmail, "child", false);

        databaseReference.setValue(childUser)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Child registration successful
                        Toast.makeText(SignUpChildActivity.this, "Child Registration Successful!", Toast.LENGTH_LONG).show();
                        // Redirect to MainActivity after 1.5 seconds
                        new android.os.Handler().postDelayed(() -> {
                            Intent intent = new Intent(SignUpChildActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }, 1500);
                    } else {
                        Toast.makeText(SignUpChildActivity.this, "Failed to save child details: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}

// ChildUser class
class ChildUser {
    public String name;
    public String email;
    public String role;
    public boolean lock;

    public ChildUser() {
        // Default constructor required for calls to DataSnapshot.getValue(ChildUser.class)
    }

    public ChildUser(String name, String email, String role, boolean lock) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.lock = lock;
    }
}
