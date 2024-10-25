package com.example.dunkdash;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class HomePageActivity extends AppCompatActivity {

    private Button startGameButton;
    private Button exitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // Initialize buttons
        startGameButton = findViewById(R.id.startGameButton);
        exitButton = findViewById(R.id.exitButton);

        // Set onClick listener for Start Game button
       // startGameButton.setOnClickListener(new View.OnClickListener() {
           // @Override
//            public void onClick(View v) {
//                // Replace GameActivity.class with the actual game activity class
//                Intent intent = new Intent(HomePageActivity.this, GameActivity.class);
//                startActivity(intent);
//            }
//        });

        // Set onClick listener for Exit button
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Exiting the app
                finish();
                System.exit(0);
            }
        });
    }
}
