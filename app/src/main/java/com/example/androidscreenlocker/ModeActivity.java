package com.example.androidscreenlocker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ModeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mode);

        // Adjust padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set click listener for Parent Icon
        findViewById(R.id.parentOption).setOnClickListener(v -> {
            Intent intent = new Intent(ModeActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        // Set click listener for Child Icon
        findViewById(R.id.childOption).setOnClickListener(v -> {
            Intent intent = new Intent(ModeActivity.this, SignUpChildActivity.class);
            startActivity(intent);
        });
    }
}
