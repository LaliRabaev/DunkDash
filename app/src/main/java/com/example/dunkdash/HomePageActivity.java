package com.example.dunkdash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class HomePageActivity extends AppCompatActivity {
    private static final String TAG = "HomePageActivity";

    private boolean gameStarted = false; // Prevent multiple game starts from rapid tapping
    private ImageView homeBackground, playerBasketball;
    private TextView greetingText, currentModeTextView;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        prefs = getSharedPreferences("DunkDashSettings", MODE_PRIVATE);

        // Bind the dynamic UI elements that change based on user selections
        homeBackground   = findViewById(R.id.home_background);
        playerBasketball = findViewById(R.id.player_basketball);

        // User info display
        greetingText = findViewById(R.id.greeting_text);
        currentModeTextView = findViewById(R.id.current_mode_text_view);

        // Firebase setup
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Load user's selected basketball, background, etc.
        loadUserSelections();
        loadUserInfo();

        // Start music right away if enabled - creates nice atmosphere
        startBackgroundMusic();

        // Tap anywhere to start game - simple and intuitive
        View root = findViewById(R.id.rootLayout);
        root.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN && !gameStarted) {
                gameStarted = true;
                startGame();
                return true;
            }
            return false;
        });

        // Left column icons - customization options
        findViewById(R.id.ball_icon).setOnClickListener(v ->
                startActivity(new Intent(this, SelectBasketballsActivity.class))
        );
        findViewById(R.id.pitches_icon).setOnClickListener(v ->
                startActivity(new Intent(this, SelectBackgroundsActivity.class))
        );
        findViewById(R.id.skull_icon).setOnClickListener(v ->
                startActivity(new Intent(this, SelectModesActivity.class))
        );
        
        // Right column icons - user features
        findViewById(R.id.cup_icon).setOnClickListener(v -> {
            Log.d(TAG, "Cup icon clicked!");
            startActivity(new Intent(this, UserProfileActivity.class));
        });
        findViewById(R.id.leaderboard_icon).setOnClickListener(v -> {
            Log.d(TAG, "Leaderboard icon clicked!");
            startActivity(new Intent(this, LeaderboardActivity.class));
        });
        findViewById(R.id.setting_icon).setOnClickListener(v -> {
            Log.d(TAG, "Settings icon clicked!");
            startActivity(new Intent(this, SettingsActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset game flag when user comes back - allows starting new game
        gameStarted = false;
        if (currentUser == null) return;
        
        // Reload everything in case user changed settings
        loadUserSelections();
        loadUserInfo();
        
        // Restart music if they have it enabled
        startBackgroundMusic();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Don't stop music here - let it continue playing in background
    }

    private void startBackgroundMusic() {
        // Respect user's music preference
        boolean musicEnabled = prefs.getBoolean("background_music", true);
        if (musicEnabled) {
            Intent musicIntent = new Intent(this, MusicService.class);
            musicIntent.setAction(MusicService.ACTION_START_MUSIC);
            startService(musicIntent);
            Log.d(TAG, "Started background music service");
        } else {
            Log.d(TAG, "Background music disabled in settings");
        }
    }

    private void loadUserSelections() {
        if (currentUser == null) return;
        String uid = currentUser.getUid();
        
        // Get user's current selections from Firebase
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) return;
                    Long bgId   = userDoc.getLong("current_background");
                    Long ballId = userDoc.getLong("current_basketball");

                    // Look up actual background image and apply it
                    if (bgId != null) {
                        db.collection("backgrounds")
                                .whereEqualTo("id", bgId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(this::applyBackground);
                    }

                    // Same for basketball
                    if (ballId != null) {
                        db.collection("basketballs")
                                .whereEqualTo("id", ballId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(this::applyBasketball);
                    }
                });
    }

    private void loadUserInfo() {
        if (currentUser == null) {
            greetingText.setText("Hello, Guest!");
            return;
        }

        String uid = currentUser.getUid();
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) {
                        greetingText.setText("Hello, Player!");
                        return;
                    }

                    // Try to get a nice display name with fallbacks
                    String nickname = userDoc.getString("nickname");
                    if (nickname == null || nickname.trim().isEmpty()) {
                        nickname = userDoc.getString("name");
                    }
                    if (nickname == null || nickname.trim().isEmpty()) {
                        String email = currentUser.getEmail();
                        if (email != null && !email.isEmpty()) {
                            // Use part before @ as nickname - better than showing full email
                            nickname = email.split("@")[0];
                        } else {
                            nickname = "Player";
                        }
                    }

                    // Add time-based greeting for personal touch
                    String greeting = getTimeBasedGreeting() + ", " + nickname + "!";
                    greetingText.setText(greeting);

                    Log.d(TAG, "User info loaded - Nickname: " + nickname);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user info", e);
                    greetingText.setText("Hello, Player!"); // Safe fallback
                });
    }

    // Add some personality with time-based greetings
    private String getTimeBasedGreeting() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        
        if (hour >= 5 && hour < 12) {
            return "Good Morning";
        } else if (hour >= 12 && hour < 17) {
            return "Good Afternoon";
        } else if (hour >= 17 && hour < 22) {
            return "Good Evening";
        } else {
            return "Good Night";
        }
    }

    private void applyBackground(QuerySnapshot qs) {
        if (qs.isEmpty()) return;
        DocumentSnapshot doc = qs.getDocuments().get(0);
        String path = doc.getString("image_path");
        Log.d(TAG, "Applying background path: " + path);
        int resId = getResIdFromPath(path);
        Log.d(TAG, "Resolved background resource ID: " + resId);
        if (resId != 0) {
            homeBackground.setImageResource(resId);
        } else {
            Log.w(TAG, "Background resource not found for path: " + path);
        }
    }

    private void applyBasketball(QuerySnapshot qs) {
        if (qs.isEmpty()) return;
        DocumentSnapshot doc = qs.getDocuments().get(0);
        String path = doc.getString("image_path");
        Log.d(TAG, "Applying basketball path: " + path);
        int resId = getResIdFromPath(path);
        Log.d(TAG, "Resolved basketball resource ID: " + resId);
        if (resId != 0) {
            playerBasketball.setImageResource(resId);
        } else {
            Log.w(TAG, "Basketball resource not found for path: " + path);
        }
    }

    private void applyMode(QuerySnapshot qs) {
        if (qs.isEmpty()) return;
        DocumentSnapshot doc = qs.getDocuments().get(0);
        Long modeId = doc.getLong("id");
        if (modeId != null) {
            updateCurrentModeDisplay(modeId.intValue());
        }
    }

    private void updateCurrentModeDisplay(int mode) {
        // Mode display removed from home page - method kept for compatibility
        Log.d(TAG, "Current mode: " + getModeDisplayText(mode) + " (not displayed on home page)");
    }

    private String getModeDisplayText(int mode) {
        switch (mode) {
            case 1:
                return "😊 Easy";
            case 2:
                return "😐 Medium";
            case 3:
                return "💀 Hard";
            case 4:
                return "🔥 Extreme";
            default:
                return "🎮 Mode " + mode;
        }
    }

    // Convert Firebase image path to actual Android resource
    // This took some debugging to get right!
    private int getResIdFromPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            Log.w(TAG, "Empty or null path provided");
            return 0;
        }
        String name = path.trim();
        Log.d(TAG, "Original path: " + name);
        
        // Strip drawable/ prefix if it exists
        if (name.startsWith("drawable/")) {
            name = name.substring("drawable/".length());
        }
        
        // Remove file extension if present
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            name = name.substring(0, lastDot);
        }
        
        Log.d(TAG, "Processed resource name: " + name);
        
        // Look up the actual resource ID
        int resId = getResources().getIdentifier(name, "drawable", getPackageName());
        if (resId == 0) {
            Log.w(TAG, "Resource not found for name: " + name + " in package: " + getPackageName());
        }
        return resId;
    }

    private void startGame() {
        // Clear any leftover game state and start fresh
        Intent intent = new Intent(this, GameActivity.class);
        intent.removeExtra("score");
        intent.removeExtra("continue");
        startActivity(intent);
        // Don't finish() - let user come back to home easily
    }
}