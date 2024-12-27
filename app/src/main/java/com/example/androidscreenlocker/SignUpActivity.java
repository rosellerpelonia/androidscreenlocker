package com.example.androidscreenlocker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {

    private TextInputEditText nameInput, emailInput, passwordInput;
    private TextInputLayout nameInputLayout, emailInputLayout, passwordInputLayout;
    private FirebaseAuth firebaseAuth;

    private boolean isPasswordVisible = false;  // Password visibility toggle state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Bind UI elements
        nameInputLayout = findViewById(R.id.nameInputLayout);
        nameInput = findViewById(R.id.nameInput);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        passwordInput = findViewById(R.id.passwordInput);
        Button signupButton = findViewById(R.id.signupButton);
        TextView signUpText = findViewById(R.id.signUpText);

        // Set up click listeners
        signupButton.setOnClickListener(view -> registerUser());

        signUpText.setOnClickListener(view -> {
            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
            finish();  // Prevent back navigation
        });

        // Set up the password toggle functionality
        setupPasswordToggle();
    }

    /**
     * Method to toggle the visibility of the password field
     */
    private void setupPasswordToggle() {
        passwordInputLayout.setEndIconDrawable(R.drawable.eye_close);
        passwordInputLayout.setEndIconOnClickListener(v -> {
            if (isPasswordVisible) {
                // Hide password
                passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
                passwordInputLayout.setEndIconDrawable(R.drawable.eye_close);  // Change icon to closed eye
            } else {
                // Show password
                passwordInput.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                passwordInputLayout.setEndIconDrawable(R.drawable.eye_open);  // Change icon to open eye
            }
            isPasswordVisible = !isPasswordVisible;  // Toggle the flag
            passwordInput.setSelection(passwordInput.getText().length());  // Maintain cursor position
        });
    }

    /**
     * Method to handle user registration
     */
    private void registerUser() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Validation checks
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

        // Register the user with Firebase Authentication
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Add debug logs
                        Log.d("SignUpActivity", "Firebase Auth User created successfully");

                        // After successful registration, store user details in the database
                        String userId = firebaseAuth.getCurrentUser().getUid();
                        saveParentDetailsToDatabase(userId, name, email);
                    } else {
                        // Log the error and show a toast message
                        Log.e("SignUpActivity", "Firebase Auth User creation failed", task.getException());
                        Toast.makeText(SignUpActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Save parent account details to Firebase Database
     */
    private void saveParentDetailsToDatabase(String userId, String name, String email) {
        // Specify the region-specific database URL
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://androidscreenlocker-default-rtdb.asia-southeast1.firebasedatabase.app");
        DatabaseReference databaseReference = database.getReference("Users").child(userId);

        ParentUser parentUser = new ParentUser(name, email, "parent");

        databaseReference.setValue(parentUser)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d("SignUpActivity", "User data saved successfully in database");

                        // Display success message
                        Toast.makeText(SignUpActivity.this, "Registration Successful! Redirecting...", Toast.LENGTH_LONG).show();

                        // Redirect to MainActivity after 1.5 seconds
                        new android.os.Handler().postDelayed(() -> {
                            Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }, 1500);
                    } else {
                        // Handle failure to save data in the database
                        Log.e("SignUpActivity", "Failed to save user data in database", task.getException());
                        Toast.makeText(SignUpActivity.this, "Failed to save user details: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

}
