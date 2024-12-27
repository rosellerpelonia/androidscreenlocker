package com.example.androidscreenlocker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AboutActivity extends AppCompatActivity {

    private ImageButton btnBack, btnSettings;
    private TextView txtTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_about);
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
                Intent aboutIntent = new Intent(AboutActivity.this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;
            } else if (id == R.id.menu_logout) {
                // Handle logout
                Toast.makeText(AboutActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                Intent logoutIntent = new Intent(AboutActivity.this, MainActivity.class);
                logoutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(logoutIntent);
                finish();
                return true;
            }
            return false;
        });


        popupMenu.show();
    }
}