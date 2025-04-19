package com.example.dunkdash;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SelectBasketballs extends AppCompatActivity {

    private String selectedBallName = "None";
    private TextView selectedBallText;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_basketballs);  // ודא שה-XML נקרא כך

        selectedBallText = findViewById(R.id.selected_ball_text);
        sharedPreferences = getSharedPreferences("BallPrefs", MODE_PRIVATE);

        // כפתור Save
        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("selected_ball", selectedBallName);
            editor.apply();
            Toast.makeText(this, "Saved: " + selectedBallName, Toast.LENGTH_SHORT).show();
        });

        // כל לחיצות הכדורים
        setupBallClick(R.id.ball_orange, "Orange");
        setupBallClick(R.id.ball_blue, "Blue");
        setupBallClick(R.id.ball_pink, "Pink");
        setupBallClick(R.id.ball_black, "Black");
        setupBallClick(R.id.ball_red, "Red");

        // טעינת ערך שמור (אם קיים)
        String savedBall = sharedPreferences.getString("selected_ball", "None");
        selectedBallName = savedBall;
        selectedBallText.setText("Selected: " + savedBall);
    }

    private void setupBallClick(int viewId, String name) {
        ImageView ball = findViewById(viewId);
        ball.setOnClickListener(v -> {
            selectedBallName = name;
            selectedBallText.setText("Selected: " + name);
        });
    }
}


