package com.example.dunkdash;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectBasketballsActivity extends AppCompatActivity {
    private static final String TAG = "SelectBasketballsAct";

    private LinearLayout basketballsContainer;
    private TextView selectedBallText, userMaxScoreText;
    private LinearLayout selectedInfoCard;
    private Button saveButton;
    private ProgressBar loadingProgress;
    private TextView errorText;
    private ScrollView contentContainer;
    private ImageButton backButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String userId;
    private long userMaxScore;
    private int selectedBallId = -1;
    private String selectedBallName = "";
    private View currentlySelectedView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_basketballs);

        initViews();
        
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        
        if (user == null) {
            Log.e(TAG, "User not authenticated");
            showError("Please sign in first");
            return;
        }
        
        userId = user.getUid();
        loadUserMaxScore();
    }

    private void initViews() {
        basketballsContainer = findViewById(R.id.basketballs_container);
        selectedBallText = findViewById(R.id.selected_ball_text);
        userMaxScoreText = findViewById(R.id.user_max_score_text);
        selectedInfoCard = findViewById(R.id.selected_info_card);
        saveButton = findViewById(R.id.save_button);
        loadingProgress = findViewById(R.id.loading_progress);
        errorText = findViewById(R.id.error_text);
        contentContainer = findViewById(R.id.content_container);
        backButton = findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> saveSelection());
    }

    private void loadUserMaxScore() {
        showLoading(true);
        
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    userMaxScore = doc.contains("max_score") ? doc.getLong("max_score") : 0;
                    userMaxScoreText.setText(String.valueOf(userMaxScore));
                    Log.d(TAG, "User max_score=" + userMaxScore);
                    loadBasketballs();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed loading max_score", e);
                    showError("Error loading user data");
                });
    }

    private void loadBasketballs() {
        basketballsContainer.removeAllViews();
        
        db.collection("basketballs").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<QueryDocumentSnapshot> docs = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        docs.add(doc);
                    }
                    
                    // Sort by min_score
                    Collections.sort(docs, Comparator.comparingLong(this::getMinScore));
                    
                    for (QueryDocumentSnapshot doc : docs) {
                        createBasketballCard(doc);
                    }
                    
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load basketballs", e);
                    showError("Failed to load basketballs");
                });
    }

    private void createBasketballCard(QueryDocumentSnapshot doc) {
        int id = doc.getLong("id").intValue();
        String name = doc.getString("name");
        String path = doc.getString("image_path");
        long minScore = getMinScore(doc);
        boolean unlocked = userMaxScore >= minScore;

        // Get drawable resource
        String resName = path.replaceFirst("^drawable/", "");
        int dot = resName.lastIndexOf('.');
        if (dot > 0) resName = resName.substring(0, dot);
        int resId = getResources().getIdentifier(resName, "drawable", getPackageName());
        
        if (resId == 0) {
            Log.w(TAG, "Drawable not found: " + path);
            return;
        }

        // Create card container
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(8));
        card.setLayoutParams(cardParams);

        // Set background based on unlock status
        if (unlocked) {
            card.setBackgroundResource(R.drawable.leaderboard_normal_background);
        } else {
            card.setBackgroundResource(R.drawable.leaderboard_bronze_background);
        }

        // Basketball image
        ImageView ballImage = new ImageView(this);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dpToPx(60), dpToPx(60));
        imageParams.setMargins(0, 0, dpToPx(16), 0);
        ballImage.setLayoutParams(imageParams);
        ballImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ballImage.setImageResource(resId);
        
        if (!unlocked) {
            ballImage.setAlpha(0.5f);
        }

        // Info container
        LinearLayout infoContainer = new LinearLayout(this);
        infoContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        infoContainer.setLayoutParams(infoParams);

        // Basketball name
        TextView nameText = new TextView(this);
        nameText.setText(name);
        nameText.setTextColor(Color.WHITE);
        nameText.setTextSize(18);
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);

        // Status text
        TextView statusText = new TextView(this);
        if (unlocked) {
            statusText.setText("✅ Unlocked");
            statusText.setTextColor(0xFF4CAF50); // Green
        } else {
            statusText.setText("🔒 Requires " + minScore + " points");
            statusText.setTextColor(0xFFFFB74D); // Orange
        }
        statusText.setTextSize(14);

        infoContainer.addView(nameText);
        infoContainer.addView(statusText);

        // Selection indicator
        TextView selectionIndicator = new TextView(this);
        selectionIndicator.setText("⭐");
        selectionIndicator.setTextSize(24);
        selectionIndicator.setVisibility(View.GONE);
        LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        indicatorParams.setMargins(dpToPx(8), 0, 0, 0);
        selectionIndicator.setLayoutParams(indicatorParams);

        // Add views to card
        card.addView(ballImage);
        card.addView(infoContainer);
        card.addView(selectionIndicator);

        // Set click listener
        if (unlocked) {
            card.setOnClickListener(v -> selectBasketball(id, name, card, selectionIndicator));
        } else {
            card.setOnClickListener(v -> {
                Toast.makeText(this, "🔒 Unlock by reaching " + minScore + " points!", Toast.LENGTH_SHORT).show();
            });
        }

        basketballsContainer.addView(card);
    }

    private void selectBasketball(int id, String name, View cardView, View indicator) {
        // Clear previous selection
        if (currentlySelectedView != null) {
            currentlySelectedView.setVisibility(View.GONE);
            // Reset background to normal
            View parentCard = (View) currentlySelectedView.getParent();
            parentCard.setBackgroundResource(R.drawable.leaderboard_normal_background);
        }

        // Set new selection
        selectedBallId = id;
        selectedBallName = name;
        currentlySelectedView = indicator;
        
        // Update UI
        indicator.setVisibility(View.VISIBLE);
        cardView.setBackgroundResource(R.drawable.leaderboard_current_user_background);
        
        selectedBallText.setText(name);
        selectedInfoCard.setVisibility(View.VISIBLE);
        
        saveButton.setEnabled(true);
        saveButton.setAlpha(1.0f);

        Log.d(TAG, "Selected basketball: " + name + " (ID: " + id + ")");
    }

    private void saveSelection() {
        if (selectedBallId < 0) {
            Toast.makeText(this, "Please select a basketball first", Toast.LENGTH_SHORT).show();
            return;
        }

        saveButton.setEnabled(false);
        saveButton.setText("💾 Saving...");

        Map<String, Object> update = new HashMap<>();
        update.put("current_basketball", selectedBallId);
        
        db.collection("users").document(userId)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ " + selectedBallName + " selected!", Toast.LENGTH_SHORT).show();
                    finish(); // Return to previous screen
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save selection", e);
                    Toast.makeText(this, "❌ Failed to save selection", Toast.LENGTH_SHORT).show();
                    saveButton.setEnabled(true);
                    saveButton.setText("💾 Save Selection");
                });
    }

    private long getMinScore(QueryDocumentSnapshot doc) {
        if (!doc.contains("min_score")) {
            return 0;
        }
        
        Object minScoreObj = doc.get("min_score");
        if (minScoreObj instanceof Number) {
            return ((Number) minScoreObj).longValue();
        } else if (minScoreObj instanceof String) {
            try {
                return Long.parseLong((String) minScoreObj);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid min_score string: " + minScoreObj);
                return 0;
            }
        }
        return 0;
    }

    private void showLoading(boolean show) {
        loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        contentContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        errorText.setVisibility(View.GONE);
    }

    private void showError(String message) {
        loadingProgress.setVisibility(View.GONE);
        contentContainer.setVisibility(View.GONE);
        errorText.setVisibility(View.VISIBLE);
        errorText.setText(message);
        Log.e(TAG, "Showing error: " + message);
    }

    private int dpToPx(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }
}