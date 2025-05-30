package com.example.dunkdash;

import android.content.Intent;
import android.os.Bundle;
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

    private void applyBackground(QuerySnapshot qs) {
        if (qs.isEmpty()) return;
        DocumentSnapshot doc = qs.getDocuments().get(0);
        String path = doc.getString("image_path");
        int resId = getResIdFromPath(path);
        if (resId != 0) homeBackground.setImageResource(resId);
    }

    private void applyBasketball(QuerySnapshot qs) {
        if (qs.isEmpty()) return;
        DocumentSnapshot doc = qs.getDocuments().get(0);
        String path = doc.getString("image_path");
        int resId = getResIdFromPath(path);
        if (resId != 0) playerBasketball.setImageResource(resId);
    }

    /** Strips "drawable/" prefix and ".png", then resolves R.drawable.name */
    private int getResIdFromPath(String path) {
        if (path == null) return 0;
        String name = path;
        if (name.startsWith("drawable/")) {
            name = name.substring("drawable/".length());
        }
        if (name.endsWith(".png")) {
            name = name.substring(0, name.length() - 4);
        }
        return getResources().getIdentifier(name, "drawable", getPackageName());
    }

    private void startGame() {
        startActivity(new Intent(this, GameActivity.class));
        finish();
    }
}
