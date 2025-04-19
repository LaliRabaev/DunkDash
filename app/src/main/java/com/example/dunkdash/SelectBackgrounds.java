package com.example.dunkdash;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SelectBackgrounds extends AppCompatActivity {

    private String selectedBackgroundName = "None";
    private TextView selectedBackgroundText;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_backgrounds);

        selectedBackgroundText = findViewById(R.id.selected_background_text);
        sharedPreferences = getSharedPreferences("BackgroundPrefs", MODE_PRIVATE);

        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("selected_background", selectedBackgroundName);
            editor.apply();
            Toast.makeText(this, "Saved: " + selectedBackgroundName, Toast.LENGTH_SHORT).show();
        });

        setupBackgroundClick(R.id.bg_blue, "Blue");
        setupBackgroundClick(R.id.bg_green, "Green");
        setupBackgroundClick(R.id.bg_grey, "Grey");
        setupBackgroundClick(R.id.bg_light_blue, "Light Blue");
        setupBackgroundClick(R.id.bg_orange, "Orange");

        String saved = sharedPreferences.getString("selected_background", "None");
        selectedBackgroundName = saved;
        selectedBackgroundText.setText("Selected: " + saved);
    }

    private void setupBackgroundClick(int viewId, String name) {
        ImageView bg = findViewById(viewId);
        bg.setOnClickListener(v -> {
            selectedBackgroundName = name;
            selectedBackgroundText.setText("Selected: " + name);
        });
    }
}
