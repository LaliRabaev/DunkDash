package com.example.dunkdash;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class SignupActivity extends AppCompatActivity {

    private EditText fullNameInput, usernameInput, passwordInput, repeatPasswordInput;
    private Button doneButton, loginButton;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // “Strong” password: ≥8 chars, at least one digit, lower, upper, special
    private static final Pattern STRONG_PW = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        fullNameInput = findViewById(R.id.fullNameInput);
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        repeatPasswordInput = findViewById(R.id.repeatpasswordInput);
        doneButton = findViewById(R.id.doneButton);
        loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(v ->
                startActivity(new Intent(SignupActivity.this, LoginActivity.class))
        );

        doneButton.setOnClickListener(v -> {
            if (validateInput()) {
                createUser();
            }
        });
    }

    private boolean validateInput() {
        String email = usernameInput.getText().toString().trim();
        String pwd = passwordInput.getText().toString();
        String repeatPw = repeatPasswordInput.getText().toString();

        if (email.isEmpty()) {
            usernameInput.setError("Email is required");
            usernameInput.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            usernameInput.setError("Enter a valid email");
            usernameInput.requestFocus();
            return false;
        }
        if (pwd.isEmpty()) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return false;
        }
        if (!STRONG_PW.matcher(pwd).matches()) {
            passwordInput.setError("Use ≥8 chars, upper, lower, digit & special");
            passwordInput.requestFocus();
            return false;
        }
        if (!pwd.equals(repeatPw)) {
            repeatPasswordInput.setError("Passwords must match");
            repeatPasswordInput.requestFocus();
            return false;
        }
        return true;
    }

    private void createUser() {
        String email = usernameInput.getText().toString().trim();
        String pwd = passwordInput.getText().toString();

        auth.createUserWithEmailAndPassword(email, pwd)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser  user = auth.getCurrentUser();
                        if (user != null) writeUserToFirestore(user.getUid(), email, pwd);
                    } else {
                        Toast.makeText(
                                this,
                                "Authentication Error: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void writeUserToFirestore(String uid, String username, String password) {
        Map<String,Object> userRecord = new HashMap<>();
        userRecord.put("username", username);
        userRecord.put("password", password);
        userRecord.put("nickname", username);
        userRecord.put("current_background", 1);
        userRecord.put("current_basketball", 1);
        userRecord.put("total_games", 0);
        userRecord.put("max_score", 0);
        userRecord.put("id", uid);

        db.collection("users")
                .document(uid)
                .set(userRecord)
                .addOnSuccessListener(aVoid -> navigateToSuccessPage())
                .addOnFailureListener(e -> Toast.makeText(
                        SignupActivity.this,
                        "Error saving user data: " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show());
    }

    private void navigateToSuccessPage() {
        Toast.makeText(this, "User Registered Successfully!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(SignupActivity.this, SuccessfulSignupActivity.class));
        finish();
    }
}
