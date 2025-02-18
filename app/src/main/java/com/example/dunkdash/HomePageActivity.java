package com.example.dunkdash;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class HomePageActivity extends AppCompatActivity {

    private boolean gameStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // Set up the touch listener on the root layout to start the game on touch
        View rootLayout = findViewById(R.id.rootLayout);
        rootLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // When the screen is touched (ACTION_DOWN) and the game hasn't started yet
                if (event.getAction() == MotionEvent.ACTION_DOWN && !gameStarted) {
                    gameStarted = true;
                    startGame();
                    return true;
                }
                return false;
            }
        });
    }

    private void startGame() {
        Intent intent = new Intent(HomePageActivity.this, GameActivity.class);
        startActivity(intent);
        finish();
    }
}

