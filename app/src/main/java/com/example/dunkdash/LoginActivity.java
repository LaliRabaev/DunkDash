package com.example.dunkdash;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
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

        // Setup password visibility toggle
        btnTogglePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility();
            }
        });

        // מאזין ללחיצה על כפתור התחברות
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

        // מאזין ללחיצה על כפתור מעבר לעמוד Signup
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

        // Developer login button
        btnDevLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performDevLogin();
            }
        });
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            etPassword.setTransformationMethod(new PasswordTransformationMethod());
            btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
        } else {
            // Show password
            etPassword.setTransformationMethod(null);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility);
        }
        isPasswordVisible = !isPasswordVisible;
        
        // Maintain cursor position
        etPassword.setSelection(etPassword.getText().length());
    }

    private void loginUser(String username, String password) {
        mAuth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        Intent movingtohomepage = new Intent(LoginActivity.this, HomePageActivity.class);
                        startActivity(movingtohomepage);
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void performDevLogin() {
        // Fill in the credentials for visual feedback
        etUsername.setText(DEV_EMAIL);
        etPassword.setText(DEV_PASSWORD);
        
        // Perform automatic login
        loginUser(DEV_EMAIL, DEV_PASSWORD);
        
        Toast.makeText(this, "Developer login initiated...", Toast.LENGTH_SHORT).show();
    }
}
