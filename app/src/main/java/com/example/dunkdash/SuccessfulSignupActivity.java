package com.example.dunkdash;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SuccessfulSignupActivity extends AppCompatActivity {

    private Button exitButton;
    private TextView successMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_successful_signup);

        // Initialize Views
        successMessage = findViewById(R.id.successMessage);
        exitButton = findViewById(R.id.exitButton);

        // Set Success Message
        successMessage.setText("Registration Successful! Welcome to DunkDash.");

        // Set Click Listener for Exit Button
        exitButton.setOnClickListener(v -> {
            // Redirect to Login Page
            Intent intent = new Intent(SuccessfulSignupActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Close current activity
        });
    }
}
