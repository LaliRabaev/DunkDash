package com.example.dunkdash;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SelectBackgroundsActivity extends AppCompatActivity {

    private Button buttonReturnHome;
    private Button saveButton;
    private TextView selectedBackgroundText;
    private GridLayout backgroundsGrid;
    private String selectedBackground = "None";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_backgrounds);

        // Initialize views
        buttonReturnHome = findViewById(R.id.button_return_home);
        saveButton = findViewById(R.id.save_button);
        selectedBackgroundText = findViewById(R.id.selected_background_text);
        backgroundsGrid = findViewById(R.id.backgrounds_grid);

        // Set up return home button click listener
        buttonReturnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to home activity
                Intent intent = new Intent(SelectBackgroundsActivity.this, HomePageActivity.class);
                startActivity(intent);
                finish(); // Close this activity
            }
        });

        // Set up save button click listener
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save the selected background
                Toast.makeText(SelectBackgroundsActivity.this, 
                    "Background saved: " + selectedBackground, Toast.LENGTH_SHORT).show();
                
                // Optionally return to home after saving
                Intent intent = new Intent(SelectBackgroundsActivity.this, HomePageActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Initialize background grid (placeholder for now)
        setupBackgroundGrid();
    }

    private void setupBackgroundGrid() {
        // TODO: Populate grid with background options
        // This is a placeholder - you can add actual background images later
        selectedBackgroundText.setText("Selected: " + selectedBackground);
    }

    private void updateSelectedBackground(String backgroundName) {
        selectedBackground = backgroundName;
        selectedBackgroundText.setText("Selected: " + selectedBackground);
    }
}