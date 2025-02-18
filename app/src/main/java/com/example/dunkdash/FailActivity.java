package com.example.dunkdash;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class FailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fail);

        Button retryButton = findViewById(R.id.retryButton);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Restart the game activity (or start your home page if needed)
                Intent intent = new Intent(FailActivity.this, HomePageActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}


