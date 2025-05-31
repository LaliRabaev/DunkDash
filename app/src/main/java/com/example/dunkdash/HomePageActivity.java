package com.example.dunkdash;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

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
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // 1) Bind dynamic ImageViews
        homeBackground   = findViewById(R.id.home_background);
        playerBasketball = findViewById(R.id.player_basketball);

        // 2) Init Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // 3) Load & apply saved selections
        loadUserSelections();

        // 4) First tap anywhere → start game
        View root = findViewById(R.id.rootLayout);
        root.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN && !gameStarted) {
                gameStarted = true;
                startGame();
                return true;
            }
            return false;
        });

        // 5) Left column icons → open selectors
        findViewById(R.id.ball_icon).setOnClickListener(v ->
                startActivity(new Intent(this, SelectBasketballsActivity.class))
        );
        findViewById(R.id.pitches_icon).setOnClickListener(v ->
                startActivity(new Intent(this, SelectBackgroundsActivity.class))
        );
        findViewById(R.id.skull_icon).setOnClickListener(v ->
                startActivity(new Intent(this, SelectModesActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset game started flag when returning to home
        gameStarted = false;
        loadUserSelections(); // Reload preferences when returning
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
                    Long modeId = userDoc.getLong("current_mode");

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

                    // Mode lookup (if needed for UI updates)
                    if (modeId != null) {
                        db.collection("game-mode")
                                .whereEqualTo("id", modeId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(this::applyMode);
                    }
                });
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
        // Store mode info for game if needed
        // Could update UI elements based on selected mode
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
        Intent intent = new Intent(this, GameActivity.class);
        // Clear any previous game state flags
        intent.removeExtra("continue");
        intent.removeExtra("score");
        startActivity(intent);
        // Don't finish() here - let user return to home
    }
}
