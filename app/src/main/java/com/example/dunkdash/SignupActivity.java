package com.example.dunkdash;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import android.content.Intent;

public class SignupActivity extends AppCompatActivity {

    private EditText fullNameInput, usernameInput, passwordInput, repeatpasswordInput;
    private Button doneButton, loginButton;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Initialize Views
        fullNameInput = findViewById(R.id.fullNameInput);
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        repeatpasswordInput = findViewById(R.id.repeatpasswordInput);
        doneButton = findViewById(R.id.doneButton);
        loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // Set Click Listener
        doneButton.setOnClickListener(v -> createUser());
    }

    private void createUser() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Create Firebase User
        auth.createUserWithEmailAndPassword(username, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Immediately navigate to success page
                        navigateToSuccessPage();
                    } else {
                        // Handle the error if authentication fails
                        Toast.makeText(this, "Authentication Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToSuccessPage() {
        Intent intent = new Intent(SignupActivity.this, SuccessfulSignupActivity.class);
        startActivity(intent);
        Toast.makeText(SignupActivity.this, "User Registered Successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }
}