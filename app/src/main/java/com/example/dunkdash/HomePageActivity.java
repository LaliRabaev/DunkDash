package com.example.dunkdash;

import android.content.Intent;
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

    private boolean gameStarted = false;
    private ImageView homeBackground, playerBasketball;
    private TextView greetingText, currentModeTextView;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // 1) Bind dynamic ImageViews
        homeBackground   = findViewById(R.id.home_background);
        playerBasketball = findViewById(R.id.player_basketball);

        // 2) Bind user info TextViews
        greetingText = findViewById(R.id.greeting_text);
        currentModeTextView = findViewById(R.id.current_mode_text_view); // Bind mode display

        // 3) Init Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // 4) Load & apply saved selections and user info
        loadUserSelections();
        loadUserInfo();

        // 5) First tap anywhere → start game
        View root = findViewById(R.id.rootLayout);
        root.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN && !gameStarted) {
                gameStarted = true;
                startGame();
                return true;
            }
            return false;
        });

        // 6) Left column icons → open selectors
        findViewById(R.id.ball_icon).setOnClickListener(v ->
                startActivity(new Intent(this, SelectBasketballsActivity.class))
        );
        findViewById(R.id.pitches_icon).setOnClickListener(v ->
                startActivity(new Intent(this, SelectBackgroundsActivity.class))
        );
        findViewById(R.id.skull_icon).setOnClickListener(v ->
                startActivity(new Intent(this, SelectModesActivity.class))
        );
        
        // 7) Right column icons
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
        // Reset game started flag when returning to home
        gameStarted = false;
        if (currentUser == null) return;
        loadUserSelections(); // Reload preferences when returning
        loadUserInfo(); // Reload user stats when returning
    }

    private void loadUserSelections() {
        if (currentUser == null) return;
        String uid = currentUser.getUid();
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) return;
                    Long bgId   = userDoc.getLong("current_background");
                    Long ballId = userDoc.getLong("current_basketball");

                    // Background lookup
                    if (bgId != null) {
                        db.collection("backgrounds")
                                .whereEqualTo("id", bgId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(this::applyBackground);
                    }

                    // Basketball lookup
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

                    // Get user nickname or fallback to email/default
                    String nickname = userDoc.getString("nickname");
                    if (nickname == null || nickname.trim().isEmpty()) {
                        nickname = userDoc.getString("name");
                    }
                    if (nickname == null || nickname.trim().isEmpty()) {
                        String email = currentUser.getEmail();
                        if (email != null && !email.isEmpty()) {
                            // Use part before @ as nickname
                            nickname = email.split("@")[0];
                        } else {
                            nickname = "Player";
                        }
                    }

                    // Set greeting with time-based message
                    String greeting = getTimeBasedGreeting() + ", " + nickname + "!";
                    greetingText.setText(greeting);

                    Log.d(TAG, "User info loaded - Nickname: " + nickname);

                    // Load user stats (max score and total games)
                    loadUserStats(userDoc);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user info", e);
                    greetingText.setText("Hello, Player!");
                });
    }

    private void loadUserStats(DocumentSnapshot userDoc) {
        // Load max score
        Long maxScore = userDoc.getLong("max_score");
        if (maxScore != null) {
            TextView maxScoreView = findViewById(R.id.max_score_value);
            if (maxScoreView != null) {
                maxScoreView.setText(String.valueOf(maxScore));
            }
        }

        // Load total games
        Long totalGames = userDoc.getLong("total_games");
        if (totalGames != null) {
            TextView totalGamesView = findViewById(R.id.total_games_value);
            if (totalGamesView != null) {
                totalGamesView.setText(String.valueOf(totalGames));
            }
        }
    }

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

    /** Strips "drawable/" prefix and file extension, then resolves R.drawable.name */
    private int getResIdFromPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            Log.w(TAG, "Empty or null path provided");
            return 0;
        }
        String name = path.trim();
        Log.d(TAG, "Original path: " + name);
        // Remove drawable/ prefix if present
        if (name.startsWith("drawable/")) {
            name = name.substring("drawable/".length());
        }
        // Remove any file extension
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            name = name.substring(0, lastDot);
        }
        
        Log.d(TAG, "Processed resource name: " + name);
        
        int resId = getResources().getIdentifier(name, "drawable", getPackageName());
        if (resId == 0) {
            Log.w(TAG, "Resource not found for name: " + name + " in package: " + getPackageName());
        }
        return resId;
    }

    private void startGame() {
        // Clear any previous game state flags
        Intent intent = new Intent(this, GameActivity.class);
        intent.removeExtra("score");
        intent.removeExtra("continue");
        startActivity(intent);
        // Don't finish() here - let user return to home
    }
}
