package com.example.dunkdash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnDone, btnSignup, btnDevLogin;
    private ImageButton btnTogglePassword;
    private TextView forgotPasswordText;
    private boolean isPasswordVisible = false;
    private FirebaseAuth mAuth;

    // Developer credentials for quick testing
    private static final String DEV_EMAIL = "yonatan2704@gmail.com";
    private static final String DEV_PASSWORD = "Yona@5674";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.usernameInput);
        etPassword = findViewById(R.id.passwordInput);
        btnDone = findViewById(R.id.doneButton);
        btnSignup = findViewById(R.id.signupButton);
        btnTogglePassword = findViewById(R.id.togglePasswordVisibility);
        btnDevLogin = findViewById(R.id.devLoginButton);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        mAuth = FirebaseAuth.getInstance();

        // Password visibility toggle - users like to see what they're typing
        btnTogglePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility();
            }
        });

        // Main login button - validate fields then try to authenticate
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please fill out both fields", Toast.LENGTH_SHORT).show();
                } else {
                    loginUser(username, password);
                }
            }
        });

        // Navigate to signup if user doesn't have account
        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });

        // Forgot password click listener
        forgotPasswordText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });

        // Developer shortcut - fills in credentials and logs in automatically
        // Super handy during development but should be hidden in production
        btnDevLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performDevLogin();
            }
        });
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password with dots
            etPassword.setTransformationMethod(new PasswordTransformationMethod());
            btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
        } else {
            // Show password as plain text
            etPassword.setTransformationMethod(null);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility);
        }
        isPasswordVisible = !isPasswordVisible;
        
        // Keep cursor at the end after toggling - better UX
        etPassword.setSelection(etPassword.getText().length());
    }

    private void loginUser(String username, String password) {
        // Firebase handles all the authentication complexity for us
        mAuth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        
                        // Start music if user has it enabled - nice welcome touch
                        startBackgroundMusicAfterLogin();
                        
                        Intent movingtohomepage = new Intent(LoginActivity.this, HomePageActivity.class);
                        startActivity(movingtohomepage);
                    } else {
                        // Show the actual error - helps users understand what went wrong
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void startBackgroundMusicAfterLogin() {
        // Check user's music preference - respect their choice
        SharedPreferences prefs = getSharedPreferences("DunkDashSettings", MODE_PRIVATE);
        boolean musicEnabled = prefs.getBoolean("background_music", true); // Default to on
        
        if (musicEnabled) {
            Intent musicIntent = new Intent(this, MusicService.class);
            musicIntent.setAction(MusicService.ACTION_START_MUSIC);
            startService(musicIntent);
            Log.d("LoginActivity", "Started background music after login");
        }
    }

    private void performDevLogin() {
        // Show the credentials in the fields so we can see what's happening
        etUsername.setText(DEV_EMAIL);
        etPassword.setText(DEV_PASSWORD);
        
        // Then actually log in with those credentials
        loginUser(DEV_EMAIL, DEV_PASSWORD);
        
        Toast.makeText(this, "Developer login initiated...", Toast.LENGTH_SHORT).show();
    }
}