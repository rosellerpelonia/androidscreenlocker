package com.example.androidscreenlocker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText emailInput, passwordInput;
    private TextInputLayout emailInputLayout, passwordInputLayout;
    private Button loginButton;
    private TextView forgotPassword, signUpText;
    private CheckBox rememberMe;
    private FirebaseAuth firebaseAuth;

    private boolean isPasswordVisible = false;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LoginPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Bind UI elements
        emailInputLayout = findViewById(R.id.emailInputLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        forgotPassword = findViewById(R.id.forgotPassword);
        signUpText = findViewById(R.id.signUpText);
        rememberMe = findViewById(R.id.rememberMe);

        // Login Button Click Listener
        loginButton.setOnClickListener(view -> loginUser());

        // Forgot Password Click Listener
        forgotPassword.setOnClickListener(view -> showForgotPasswordDialog());

        // Sign Up Text Click Listener
        signUpText.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ModeActivity.class);
            startActivity(intent);
        });

        // Set up password toggle icon
        setupPasswordToggle();

        // Load saved credentials
        loadCredentials();
    }

    private void setupPasswordToggle() {
        passwordInputLayout.setEndIconDrawable(R.drawable.eye_close);
        passwordInputLayout.setEndIconOnClickListener(v -> {
            if (isPasswordVisible) {
                // Hide password
                passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
                passwordInputLayout.setEndIconDrawable(R.drawable.eye_close);
            } else {
                // Show password
                passwordInput.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                passwordInputLayout.setEndIconDrawable(R.drawable.eye_open);
            }
            isPasswordVisible = !isPasswordVisible;
            passwordInput.setSelection(passwordInput.getText().length());
        });
    }

    private void loadCredentials() {
        String savedEmail = sharedPreferences.getString("email", "");
        String savedPassword = sharedPreferences.getString("password", "");

        if (!TextUtils.isEmpty(savedEmail) && !TextUtils.isEmpty(savedPassword)) {
            emailInput.setText(savedEmail);
            passwordInput.setText(savedPassword);
            rememberMe.setChecked(true);
        }
    }

    private void saveCredentials(String email, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (rememberMe.isChecked()) {
            editor.putString("email", email);
            editor.putString("password", password);
        } else {
            editor.clear();
        }
        editor.apply();
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Email is required!");
            return;
        } else {
            emailInputLayout.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError("Password is required!");
            return;
        } else {
            passwordInputLayout.setError(null);
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveCredentials(email, password);

                        String userId = firebaseAuth.getCurrentUser().getUid();
                        checkUserRole(userId);
                    } else {
                        Toast.makeText(MainActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserRole(String userId) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance("https://androidscreenlocker-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Users");

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean userFound = false;

                for (DataSnapshot parentSnapshot : dataSnapshot.getChildren()) {
                    String parentId = parentSnapshot.getKey();

                    if (parentId.equals(userId)) {
                        userFound = true;
                        String role = parentSnapshot.child("role").getValue(String.class);

                        if ("parent".equals(role)) {
                            String parentName = parentSnapshot.child("name").getValue(String.class);
                            DataSnapshot childrenSnapshot = parentSnapshot.child("Children");
                            ArrayList<String> childrenList = new ArrayList<>();

                            if (childrenSnapshot.exists()) {
                                for (DataSnapshot child : childrenSnapshot.getChildren()) {
                                    String childName = child.child("name").getValue(String.class);
                                    if (childName != null) {
                                        childrenList.add(childName);
                                    }
                                }
                            }

                            Intent intent = new Intent(MainActivity.this, ParentActivity.class);
                            intent.putExtra("parentName", parentName);
                            intent.putStringArrayListExtra("childrenList", childrenList);

                            if (childrenList.isEmpty()) {
                                intent.putExtra("childrenMessage", "No Child Accounts found");
                            }

                            startActivity(intent);
                            finish();
                        }
                        break;
                    }

                    if (parentSnapshot.hasChild("Children")) {
                        for (DataSnapshot childSnapshot : parentSnapshot.child("Children").getChildren()) {
                            if (childSnapshot.getKey().equals(userId)) {
                                userFound = true;
                                String role = childSnapshot.child("role").getValue(String.class);

                                if ("child".equals(role)) {
                                    FirebaseAuth auth = FirebaseAuth.getInstance();
                                    Intent intent = new Intent(MainActivity.this, EnableAdminActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                                break;
                            }
                        }
                    }

                    if (userFound) break;
                }

                if (!userFound) {
                    Toast.makeText(MainActivity.this, "User not found in database", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Error checking user role: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_forgot_password, null);

        builder.setView(dialogView);

        TextInputEditText emailField = dialogView.findViewById(R.id.emailField);
        Button nextButton = dialogView.findViewById(R.id.nextButton);

        AlertDialog emailDialog = builder.create();
        emailDialog.show();

        nextButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference databaseReference = FirebaseDatabase.getInstance("https://androidscreenlocker-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("Users");

            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean emailFound = false;

                    // Search Parent Emails
                    for (DataSnapshot parentSnapshot : dataSnapshot.getChildren()) {
                        String parentEmail = parentSnapshot.child("email").getValue(String.class);

                        if (email.equals(parentEmail)) {
                            emailFound = true;
                            emailDialog.dismiss();
                            showChangePasswordModal(email);
                            break;
                        }

                        // Search Child Emails
                        if (parentSnapshot.hasChild("Children")) {
                            for (DataSnapshot childSnapshot : parentSnapshot.child("Children").getChildren()) {
                                String childEmail = childSnapshot.child("email").getValue(String.class);
                                if (email.equals(childEmail)) {
                                    emailFound = true;
                                    emailDialog.dismiss();
                                    showChangePasswordModal(email);
                                    break;
                                }
                            }
                        }

                        if (emailFound) break;
                    }

                    if (!emailFound) {
                        Toast.makeText(MainActivity.this, "Email not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(MainActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

    }

    private void showChangePasswordModal(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View modalView = inflater.inflate(R.layout.dialog_change_password, null);

        builder.setView(modalView);

        TextInputEditText newPasswordInput = modalView.findViewById(R.id.newPasswordInput);
        TextInputEditText confirmNewPasswordInput = modalView.findViewById(R.id.confirmNewPasswordInput);
        Button confirmButton = modalView.findViewById(R.id.confirmButton);

        AlertDialog changePasswordDialog = builder.create();
        changePasswordDialog.show();

        confirmButton.setOnClickListener(v -> {
            String newPassword = newPasswordInput.getText().toString().trim();
            String confirmPassword = confirmNewPasswordInput.getText().toString().trim();

            if (TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Search for user ID corresponding to the email
            DatabaseReference databaseReference = FirebaseDatabase.getInstance("https://androidscreenlocker-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("Users");

            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean userFound = false;

                    for (DataSnapshot parentSnapshot : dataSnapshot.getChildren()) {
                        String parentEmail = parentSnapshot.child("email").getValue(String.class);

                        if (email.equals(parentEmail)) {
                            userFound = true;
                            updateFirebaseAuthPassword(newPassword, changePasswordDialog);
                            break;
                        }

                        if (parentSnapshot.hasChild("Children")) {
                            for (DataSnapshot childSnapshot : parentSnapshot.child("Children").getChildren()) {
                                String childEmail = childSnapshot.child("email").getValue(String.class);
                                if (email.equals(childEmail)) {
                                    userFound = true;
                                    updateFirebaseAuthPassword(newPassword, changePasswordDialog);
                                    break;
                                }
                            }
                        }

                        if (userFound) break;
                    }

                    if (!userFound) {
                        Toast.makeText(MainActivity.this, "User not found in database", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(MainActivity.this, "Error updating password: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void updateFirebaseAuthPassword(String newPassword, AlertDialog dialog) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            user.updatePassword(newPassword).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(MainActivity.this, "Error updating password: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "User is not authenticated", Toast.LENGTH_SHORT).show();
        }
    }


}
