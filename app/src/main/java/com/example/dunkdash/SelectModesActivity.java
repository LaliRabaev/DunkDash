package com.example.dunkdash;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectModesActivity extends AppCompatActivity {
    private static final String TAG = "SelectModesAct";

    private LinearLayout modesContainer;
    private TextView selectedModeText, userMaxScoreText;
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
    private int selectedModeId = -1;
    private String selectedModeName = "";
    private View currentlySelectedView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_modes);

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
        modesContainer = findViewById(R.id.modes_container);
        selectedModeText = findViewById(R.id.selected_mode_text);
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
                    loadModes();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed loading max_score", e);
                    showError("Error loading user data");
                });
    }

    private void loadModes() {
        modesContainer.removeAllViews();
        
        Log.d(TAG, "Starting to load game modes from Firestore...");
        
        db.collection("game-mode").get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Firestore query successful. Documents found: " + querySnapshot.size());
                    
                    if (querySnapshot.isEmpty()) {
                        Log.w(TAG, "No game modes found in 'game-mode' collection");
                        
                        // Try alternative collection name
                        Log.d(TAG, "Trying alternative collection name 'game_modes'...");
                        db.collection("game_modes").get()
                                .addOnSuccessListener(altQuerySnapshot -> {
                                    Log.d(TAG, "Alternative query found: " + altQuerySnapshot.size() + " documents");
                                    if (altQuerySnapshot.isEmpty()) {
                                        showError("No game modes available");
                                    } else {
                                        processGameModes(altQuerySnapshot);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Alternative query also failed", e);
                                    showError("Failed to load game modes");
                                });
                        return;
                    }
                    
                    processGameModes(querySnapshot);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load modes from 'game-mode' collection", e);
                    
                    // Try alternative collection name
                    Log.d(TAG, "Trying alternative collection name 'game_modes'...");
                    db.collection("game_modes").get()
                            .addOnSuccessListener(altQuerySnapshot -> {
                                Log.d(TAG, "Alternative query successful: " + altQuerySnapshot.size() + " documents");
                                if (altQuerySnapshot.isEmpty()) {
                                    showError("No game modes available");
                                } else {
                                    processGameModes(altQuerySnapshot);
                                }
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e(TAG, "Both collection queries failed", e2);
                                showError("Failed to load game modes");
                            });
                });
    }
    
    private void processGameModes(com.google.firebase.firestore.QuerySnapshot querySnapshot) {
        List<com.google.firebase.firestore.QueryDocumentSnapshot> docs = new ArrayList<>();
        
        Log.d(TAG, "Processing " + querySnapshot.size() + " game mode documents");
        
        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
            Log.d(TAG, "Game mode document: " + doc.getId() + " -> " + doc.getData());
            docs.add(doc);
        }
        
        // Sort by min_score
        Collections.sort(docs, Comparator.comparingLong(this::getMinScore));
        
        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : docs) {
            createModeCard(doc);
        }
        
        showLoading(false);
        Log.d(TAG, "Finished processing game modes");
    }

    private void createModeCard(QueryDocumentSnapshot doc) {
        Log.d(TAG, "Creating card for document: " + doc.getId() + " with data: " + doc.getData());
        
        // Check if required fields exist
        if (!doc.contains("id")) {
            Log.w(TAG, "Document missing 'id' field: " + doc.getId());
            return;
        }
        
        int id = doc.getLong("id").intValue();
        String name = doc.getString("name");
        if (name == null || name.trim().isEmpty()) {
            Log.w(TAG, "Document missing or empty 'name' field: " + doc.getId());
            name = "Unknown Mode";
        }
        
        Long speed = doc.getLong("speed");
        long minScore = getMinScore(doc);
        boolean unlocked = userMaxScore >= minScore;

        Log.d(TAG, "Processing mode: " + name + " (ID: " + id + ", Speed: " + speed + ", MinScore: " + minScore + ", Unlocked: " + unlocked + ")");

        // Create card container
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(12));
        card.setLayoutParams(cardParams);

        // Set special background based on mode type and unlock status
        setModeCardBackground(card, name, unlocked);

        // Header container (emoji + name + selection indicator)
        LinearLayout headerContainer = new LinearLayout(this);
        headerContainer.setOrientation(LinearLayout.HORIZONTAL);
        headerContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // Mode emoji
        TextView modeEmoji = new TextView(this);
        modeEmoji.setText(getModeEmoji(name));
        modeEmoji.setTextSize(32);
        LinearLayout.LayoutParams emojiParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        emojiParams.setMargins(0, 0, dpToPx(16), 0);
        modeEmoji.setLayoutParams(emojiParams);

        // Info container (name + description + status)
        LinearLayout infoContainer = new LinearLayout(this);
        infoContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        infoContainer.setLayoutParams(infoParams);

        // Mode name
        TextView nameText = new TextView(this);
        nameText.setText(name);
        nameText.setTextColor(Color.WHITE);
        nameText.setTextSize(22);
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);

        // Mode description with speed info
        TextView descriptionText = new TextView(this);
        String description = getDefaultDescription(name);
        if (speed != null) {
            description += " • Speed: " + speed;
        }
        descriptionText.setText(description);
        descriptionText.setTextColor(0xFFCCCCCC);
        descriptionText.setTextSize(14);
        descriptionText.setPadding(0, dpToPx(4), 0, dpToPx(8));

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
        statusText.setTypeface(null, android.graphics.Typeface.BOLD);

        infoContainer.addView(nameText);
        infoContainer.addView(descriptionText);
        infoContainer.addView(statusText);

        // Selection indicator
        TextView selectionIndicator = new TextView(this);
        selectionIndicator.setText("⭐");
        selectionIndicator.setTextSize(28);
        selectionIndicator.setVisibility(View.GONE);
        LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        indicatorParams.setMargins(dpToPx(8), 0, 0, 0);
        selectionIndicator.setLayoutParams(indicatorParams);

        // Add views to header
        headerContainer.addView(modeEmoji);
        headerContainer.addView(infoContainer);
        headerContainer.addView(selectionIndicator);

        // Add header to card
        card.addView(headerContainer);

        // Set click listener
        if (unlocked) {
            card.setOnClickListener(v -> selectMode(id, name, card, selectionIndicator));
        } else {
            card.setOnClickListener(v -> {
                Toast.makeText(this, "🔒 Unlock by reaching " + minScore + " points!", Toast.LENGTH_SHORT).show();
            });
        }

        modesContainer.addView(card);
        
        // Debug logging
        Log.d(TAG, "Created mode card: " + name + " (ID: " + id + ", Speed: " + speed + ", MinScore: " + minScore + ", Unlocked: " + unlocked + ")");
    }

    private void setModeCardBackground(LinearLayout card, String modeName, boolean unlocked) {
        if (!unlocked) {
            card.setBackgroundResource(R.drawable.leaderboard_bronze_background);
            return;
        }

        // Set special backgrounds based on mode type
        switch (modeName.toLowerCase()) {
            case "easy":
                card.setBackgroundResource(R.drawable.leaderboard_normal_background);
                break;
            case "medium":
                card.setBackgroundResource(R.drawable.leaderboard_silver_background);
                break;
            case "hard":
                card.setBackgroundResource(R.drawable.leaderboard_bronze_background);
                break;
            default:
                card.setBackgroundResource(R.drawable.leaderboard_normal_background);
                break;
        }
    }

    private String getModeEmoji(String modeName) {
        switch (modeName.toLowerCase()) {
            case "easy":
                return "😊";
            case "medium":
                return "😐";
            case "hard":
                return "💀";
            default:
                return "🎮";
        }
    }

    private String getDefaultDescription(String modeName) {
        switch (modeName.toLowerCase()) {
            case "easy":
                return "Perfect for beginners • Relaxed gameplay";
            case "medium":
                return "Balanced challenge • Good for improving";
            case "hard":
                return "Ultimate challenge • For skilled players only";
            default:
                return "A challenging game mode";
        }
    }

    private void selectMode(int id, String name, View cardView, View indicator) {
        // Clear previous selection
        if (currentlySelectedView != null) {
            currentlySelectedView.setVisibility(View.GONE);
            // Reset background based on mode type
            View parentCard = (View) currentlySelectedView.getParent().getParent();
            setModeCardBackground((LinearLayout) parentCard, selectedModeName, true);
        }

        // Set new selection
        selectedModeId = id;
        selectedModeName = name;
        currentlySelectedView = indicator;
        
        // Update UI
        indicator.setVisibility(View.VISIBLE);
        cardView.setBackgroundResource(R.drawable.leaderboard_current_user_background);
        
        selectedModeText.setText(name);
        selectedInfoCard.setVisibility(View.VISIBLE);
        
        saveButton.setEnabled(true);
        saveButton.setAlpha(1.0f);

        Log.d(TAG, "Selected mode: " + name + " (ID: " + id + ")");
    }

    private void saveSelection() {
        if (selectedModeId < 0) {
            Toast.makeText(this, "Please select a game mode first", Toast.LENGTH_SHORT).show();
            return;
        }

        saveButton.setEnabled(false);
        saveButton.setText("💾 Saving...");

        Map<String, Object> update = new HashMap<>();
        update.put("current_mode", selectedModeId);
        
        db.collection("users").document(userId)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ " + selectedModeName + " mode selected!", Toast.LENGTH_SHORT).show();
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
