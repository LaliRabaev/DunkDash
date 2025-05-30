package com.example.dunkdash;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class SelectModes extends AppCompatActivity {

    private String selectedMode = "None";
    private TextView selectedModeText;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_modes);

        selectedModeText = findViewById(R.id.selected_mode_text);
        sharedPreferences = getSharedPreferences("ModePrefs", MODE_PRIVATE);

        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("selected_mode", selectedMode);
            editor.apply();
            Toast.makeText(this, "Saved: " + selectedMode, Toast.LENGTH_SHORT).show();
        });

        // Add return button logic
        Button returnButton = findViewById(R.id.button_return_home);
        returnButton.setOnClickListener(v -> {
            Intent intent = new Intent(SelectModes.this, HomePageActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        setupModeClick(R.id.mode_easy, "Easy");
        setupModeClick(R.id.mode_medium, "Medium");
        setupModeClick(R.id.mode_hard, "Hard");

        String saved = sharedPreferences.getString("selected_mode", "None");
        selectedMode = saved;
        selectedModeText.setText("Selected: " + saved);
    }

    private void setupModeClick(int viewId, String name) {
        ImageView mode = findViewById(viewId);
        mode.setOnClickListener(v -> {
            selectedMode = name;
            selectedModeText.setText("Selected: " + name);
        });
    }
}
