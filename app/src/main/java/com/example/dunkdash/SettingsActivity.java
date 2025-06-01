package com.example.dunkdash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    private ImageButton backButton;
    private EditText nicknameInput;
    private Button saveNicknameButton, resetProgressButton, logoutButton;
    private TextView currentEmailText, statsText;
    private SwitchCompat backgroundMusicSwitch, soundEffectsSwitch, vibrationSwitch, autoSaveSwitch;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initializeViews();
        initializeFirebase();
        loadSettings();
        loadUserData();
        setupClickListeners();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        nicknameInput = findViewById(R.id.nicknameInput);
        saveNicknameButton = findViewById(R.id.saveNicknameButton);
        resetProgressButton = findViewById(R.id.resetProgressButton);
        logoutButton = findViewById(R.id.logoutButton);
        currentEmailText = findViewById(R.id.currentEmailText);
        statsText = findViewById(R.id.statsText);
        
        backgroundMusicSwitch = findViewById(R.id.backgroundMusicSwitch);
        soundEffectsSwitch = findViewById(R.id.soundEffectsSwitch);
        vibrationSwitch = findViewById(R.id.vibrationSwitch);
        autoSaveSwitch = findViewById(R.id.autoSaveSwitch);
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences("DunkDashSettings", MODE_PRIVATE);
        
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        }
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        saveNicknameButton.setOnClickListener(v -> saveNickname());
        
        resetProgressButton.setOnClickListener(v -> showResetConfirmation());
        
        logoutButton.setOnClickListener(v -> showLogoutConfirmation());
        
        // Save settings when switches are toggled
        backgroundMusicSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("background_music", isChecked);
            if (isChecked) {
                Toast.makeText(this, "🎵 Background music enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "🔇 Background music disabled", Toast.LENGTH_SHORT).show();
            }
        });
        
        soundEffectsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("sound_effects", isChecked);
            if (isChecked) {
                Toast.makeText(this, "🔉 Sound effects enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "🔇 Sound effects disabled", Toast.LENGTH_SHORT).show();
            }
        });
        
        vibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("vibration", isChecked);
            if (isChecked) {
                Toast.makeText(this, "📳 Vibration enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "📳 Vibration disabled", Toast.LENGTH_SHORT).show();
            }
        });
        
        autoSaveSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("auto_save", isChecked);
            if (isChecked) {
                Toast.makeText(this, "💾 Auto-save enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "💾 Auto-save disabled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSettings() {
        backgroundMusicSwitch.setChecked(prefs.getBoolean("background_music", true));
        soundEffectsSwitch.setChecked(prefs.getBoolean("sound_effects", true));
        vibrationSwitch.setChecked(prefs.getBoolean("vibration", true));
        autoSaveSwitch.setChecked(prefs.getBoolean("auto_save", true));
    }

    private void loadUserData() {
        if (userId == null) {
            currentEmailText.setText("📧 Email: Not logged in");
            statsText.setText("⚠️ Please log in to view statistics");
            return;
        }

        // Load email
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            currentEmailText.setText("📧 Email: " + (email != null ? email : "Unknown"));
        }

        // Load user data from Firestore
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Load current nickname
                        String nickname = doc.getString("nickname");
                        if (nickname != null && !nickname.trim().isEmpty()) {
                            nicknameInput.setHint("Current: " + nickname);
                        }

                        // Load statistics
                        long totalGames = doc.contains("total_games") ? doc.getLong("total_games") : 0;
                        long maxScore = doc.contains("max_score") ? doc.getLong("max_score") : 0;
                        int currentBackground = doc.contains("current_background") ? doc.getLong("current_background").intValue() : 1;
                        int currentBasketball = doc.contains("current_basketball") ? doc.getLong("current_basketball").intValue() : 1;

                        String stats = "🎮 Games Played: " + totalGames + "\n" +
                                      "🏆 Best Score: " + maxScore + "\n" +
                                      "🏞️ Current Background: #" + currentBackground + "\n" +
                                      "🏀 Current Basketball: #" + currentBasketball;
                        
                        statsText.setText(stats);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user data", e);
                    statsText.setText("❌ Failed to load statistics");
                });
    }

    private void saveNickname() {
        String newNickname = nicknameInput.getText().toString().trim();
        
        if (newNickname.isEmpty()) {
            Toast.makeText(this, "Please enter a nickname", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newNickname.length() > 20) {
            Toast.makeText(this, "Nickname must be 20 characters or less", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userId == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        saveNicknameButton.setEnabled(false);
        saveNicknameButton.setText("💾 Saving...");

        Map<String, Object> update = new HashMap<>();
        update.put("nickname", newNickname);

        db.collection("users").document(userId)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Nickname updated to: " + newNickname, Toast.LENGTH_SHORT).show();
                    nicknameInput.setText("");
                    nicknameInput.setHint("Current: " + newNickname);
                    saveNicknameButton.setEnabled(true);
                    saveNicknameButton.setText("💾 Save");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update nickname", e);
                    Toast.makeText(this, "❌ Failed to update nickname", Toast.LENGTH_SHORT).show();
                    saveNicknameButton.setEnabled(true);
                    saveNicknameButton.setText("💾 Save");
                });
    }

    private void saveSetting(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    private void showResetConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("🔄 Reset Progress")
                .setMessage("This will reset all your game progress including:\n\n• Your best score\n• Total games played\n• Unlocked items\n\nThis action cannot be undone. Are you sure?")
                .setPositiveButton("Reset", (dialog, which) -> resetProgress())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetProgress() {
        if (userId == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> reset = new HashMap<>();
        reset.put("total_games", 0);
        reset.put("max_score", 0);
        reset.put("current_background", 1);
        reset.put("current_basketball", 1);

        db.collection("users").document(userId)
                .update(reset)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Progress reset successfully", Toast.LENGTH_SHORT).show();
                    loadUserData(); // Refresh the displayed stats
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to reset progress", e);
                    Toast.makeText(this, "❌ Failed to reset progress", Toast.LENGTH_SHORT).show();
                });
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("🚪 Logout")
                .setMessage("Are you sure you want to logout?\n\nYou'll need to sign in again to access your account.")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        // Clear all local settings
        prefs.edit().clear().apply();
        
        // Sign out from Firebase
        mAuth.signOut();
        
        Toast.makeText(this, "👋 Logged out successfully", Toast.LENGTH_SHORT).show();
        
        // Navigate to login screen and clear task stack
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
