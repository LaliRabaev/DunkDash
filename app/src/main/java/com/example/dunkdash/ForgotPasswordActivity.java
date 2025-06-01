package com.example.dunkdash;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

public class ForgotPasswordActivity extends AppCompatActivity {
    private static final String TAG = "ForgotPasswordActivity";

    private EditText etEmail;
    private Button btnReset;
    private ImageButton btnBack;
    private ProgressBar progressBar;
    private TextView statusMessage;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.emailInput);
        btnReset = findViewById(R.id.resetButton);
        btnBack = findViewById(R.id.backButton);
        progressBar = findViewById(R.id.progressBar);
        statusMessage = findViewById(R.id.statusMessage);
        mAuth = FirebaseAuth.getInstance();
        
        Log.d(TAG, "Firebase Auth initialized: " + (mAuth != null));
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnReset.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            
            if (email.isEmpty()) {
                showError("Please enter your email address");
                return;
            }
            
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError("Please enter a valid email address");
                return;
            }
            
            sendPasswordResetEmail(email);
        });
    }

    private void sendPasswordResetEmail(String email) {
        Log.d(TAG, "Attempting to send password reset email to: " + email);
        showLoading(true);
        hideStatusMessage();

        // Use simple sendPasswordResetEmail without ActionCodeSettings
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent successfully to: " + email);
                        showSuccess("✅ Reset link sent to " + email + "!\n\nPlease check:\n• Your email inbox\n• Spam/junk folder\n• Email may take a few minutes");
                        
                        // Clear the email field
                        etEmail.setText("");
                        
                        // Auto-close after 4 seconds
                        etEmail.postDelayed(() -> finish(), 4000);
                        
                    } else {
                        Exception exception = task.getException();
                        Log.e(TAG, "Failed to send password reset email", exception);
                        
                        String errorMessage = "Failed to send reset email";
                        if (exception instanceof FirebaseAuthException) {
                            FirebaseAuthException firebaseException = (FirebaseAuthException) exception;
                            String errorCode = firebaseException.getErrorCode();
                            Log.e(TAG, "Firebase Auth Error Code: " + errorCode);
                            
                            switch (errorCode) {
                                case "ERROR_USER_NOT_FOUND":
                                    errorMessage = "No account found with this email address";
                                    break;
                                case "ERROR_INVALID_EMAIL":
                                    errorMessage = "Invalid email address format";
                                    break;
                                case "ERROR_TOO_MANY_REQUESTS":
                                    errorMessage = "Too many requests. Please wait before trying again";
                                    break;
                                default:
                                    errorMessage = firebaseException.getMessage();
                                    break;
                            }
                        } else if (exception != null) {
                            errorMessage = exception.getMessage();
                        }
                        
                        showError("❌ " + errorMessage);
                    }
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnReset.setEnabled(!show);
        etEmail.setEnabled(!show);
        
        if (show) {
            btnReset.setText("🔄 Sending...");
            btnReset.setAlpha(0.6f);
        } else {
            btnReset.setText("🚀 Send Reset Link");
            btnReset.setAlpha(1.0f);
        }
    }

    private void showError(String message) {
        statusMessage.setText(message);
        statusMessage.setTextColor(0xFFFF5252); // Red
        statusMessage.setVisibility(View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Error: " + message);
    }

    private void showSuccess(String message) {
        statusMessage.setText(message);
        statusMessage.setTextColor(0xFF4CAF50); // Green
        statusMessage.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Reset email sent! Check your inbox.", Toast.LENGTH_LONG).show();
        Log.d(TAG, "Success: " + message);
    }

    private void hideStatusMessage() {
        statusMessage.setVisibility(View.GONE);
    }
}