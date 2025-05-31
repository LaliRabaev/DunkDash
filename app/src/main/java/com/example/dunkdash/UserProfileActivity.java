package com.example.dunkdash;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserProfileActivity extends AppCompatActivity {
    private static final String TAG = "UserProfileActivity";

    private TextView nicknameText, emailText, maxScoreText, totalGamesText;
    private TextView lastGameDateText, lastGameScoreText;
    private View lastGameCard, noLastGameCard;
    private ProgressBar loadingProgress;
    private TextView errorText;
    private ScrollView profileContent;
    private ImageButton backButton;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        initViews();
        
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        if (currentUser != null) {
            loadUserProfile();
        } else {
            showError("Please log in to view profile");
        }
    }

    private void initViews() {
        nicknameText = findViewById(R.id.nickname_text);
        emailText = findViewById(R.id.email_text);
        maxScoreText = findViewById(R.id.max_score_text);
        totalGamesText = findViewById(R.id.total_games_text);
        lastGameDateText = findViewById(R.id.last_game_date_text);
        lastGameScoreText = findViewById(R.id.last_game_score_text);
        lastGameCard = findViewById(R.id.last_game_card);
        noLastGameCard = findViewById(R.id.no_last_game_card);
        loadingProgress = findViewById(R.id.loading_progress);
        errorText = findViewById(R.id.error_text);
        profileContent = findViewById(R.id.profile_content);
        backButton = findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> finish());
    }

    private void loadUserProfile() {
        showLoading(true);
        String uid = currentUser.getUid();
        Log.d(TAG, "Loading profile for user: " + uid);

        // Load user data from users collection
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) {
                        showError("User profile not found");
                        return;
                    }

                    // Get user info
                    String nickname = userDoc.getString("nickname");
                    if (nickname == null || nickname.trim().isEmpty()) {
                        nickname = userDoc.getString("name");
                    }
                    if (nickname == null || nickname.trim().isEmpty()) {
                        String email = currentUser.getEmail();
                        if (email != null && !email.isEmpty()) {
                            nickname = email.split("@")[0];
                        } else {
                            nickname = "Player";
                        }
                    }

                    String email = currentUser.getEmail();
                    if (email == null || email.trim().isEmpty()) {
                        email = "No email provided";
                    }

                    Long maxScore = userDoc.getLong("max_score");
                    Long totalGames = userDoc.getLong("total_games");

                    UserProfile profile = new UserProfile(
                            nickname,
                            email,
                            maxScore != null ? maxScore : 0L,
                            totalGames != null ? totalGames : 0L
                    );

                    // Load last game info
                    loadLastGameInfo(profile);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user profile", e);
                    showError("Failed to load profile");
                });
    }

    private void loadLastGameInfo(UserProfile profile) {
        String uid = currentUser.getUid();
        Log.d(TAG, "Querying games collection for user_id: " + uid);
        
        // Query games without ordering to avoid index requirement
        // We'll sort client-side instead
        db.collection("games")
                .whereEqualTo("user_id", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Games query completed. Found " + querySnapshot.size() + " documents");
                    
                    if (!querySnapshot.isEmpty()) {
                        // Find the most recent game by comparing timestamps client-side
                        QueryDocumentSnapshot mostRecentGame = null;
                        com.google.firebase.Timestamp mostRecentTimestamp = null;
                        
                        for (QueryDocumentSnapshot gameDoc : querySnapshot) {
                            Log.d(TAG, "Game document data: " + gameDoc.getData());
                            
                            // Try different possible field names for timestamp
                            com.google.firebase.Timestamp timestamp = gameDoc.getTimestamp("start_date");
                            if (timestamp == null) {
                                timestamp = gameDoc.getTimestamp("startDate");
                            }
                            if (timestamp == null) {
                                timestamp = gameDoc.getTimestamp("date");
                            }
                            if (timestamp == null) {
                                timestamp = gameDoc.getTimestamp("created_at");
                            }
                            
                            if (timestamp != null) {
                                if (mostRecentTimestamp == null || timestamp.compareTo(mostRecentTimestamp) > 0) {
                                    mostRecentTimestamp = timestamp;
                                    mostRecentGame = gameDoc;
                                }
                            }
                        }
                        
                        if (mostRecentGame != null && mostRecentTimestamp != null) {
                            // Try different possible field names for score
                            Long score = mostRecentGame.getLong("score");
                            if (score == null) {
                                score = mostRecentGame.getLong("final_score");
                            }
                            if (score == null) {
                                score = mostRecentGame.getLong("game_score");
                            }
                            
                            Log.d(TAG, "Most recent game - timestamp: " + mostRecentTimestamp + ", score: " + score);
                            
                            if (score != null) {
                                Date gameDate = mostRecentTimestamp.toDate();
                                profile.setLastGameInfo(gameDate, score);
                                Log.d(TAG, "Successfully set last game info: " + gameDate + ", score: " + score);
                            } else {
                                Log.w(TAG, "Could not extract score from most recent game document");
                                Log.w(TAG, "Available fields: " + mostRecentGame.getData().keySet());
                            }
                        } else {
                            Log.w(TAG, "No games with valid timestamps found");
                        }
                    } else {
                        Log.d(TAG, "No games found for user " + uid);
                        
                        // Let's also try a broader query to see if there are any games with different user_id field names
                        db.collection("games")
                                .limit(5)
                                .get()
                                .addOnSuccessListener(allGamesSnapshot -> {
                                    Log.d(TAG, "Sample games in collection (" + allGamesSnapshot.size() + " total):");
                                    for (QueryDocumentSnapshot doc : allGamesSnapshot) {
                                        Log.d(TAG, "Game doc: " + doc.getData());
                                    }
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to query sample games", e));
                    }
                    
                    // Display the profile regardless of last game info
                    displayProfile(profile);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load games: " + e.getMessage(), e);
                    
                    // Try alternative query with different user field name
                    Log.d(TAG, "Trying alternative query with 'userId' field...");
                    db.collection("games")
                            .whereEqualTo("userId", uid)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                Log.d(TAG, "Alternative query found " + querySnapshot.size() + " documents");
                                
                                if (!querySnapshot.isEmpty()) {
                                    // Find the most recent game by comparing timestamps client-side
                                    QueryDocumentSnapshot mostRecentGame = null;
                                    com.google.firebase.Timestamp mostRecentTimestamp = null;
                                    
                                    for (QueryDocumentSnapshot gameDoc : querySnapshot) {
                                        com.google.firebase.Timestamp timestamp = gameDoc.getTimestamp("start_date");
                                        if (timestamp == null) {
                                            timestamp = gameDoc.getTimestamp("startDate");
                                        }
                                        if (timestamp == null) {
                                            timestamp = gameDoc.getTimestamp("date");
                                        }
                                        
                                        if (timestamp != null) {
                                            if (mostRecentTimestamp == null || timestamp.compareTo(mostRecentTimestamp) > 0) {
                                                mostRecentTimestamp = timestamp;
                                                mostRecentGame = gameDoc;
                                            }
                                        }
                                    }
                                    
                                    if (mostRecentGame != null) {
                                        Long score = mostRecentGame.getLong("score");
                                        if (score != null && mostRecentTimestamp != null) {
                                            Date gameDate = mostRecentTimestamp.toDate();
                                            profile.setLastGameInfo(gameDate, score);
                                            Log.d(TAG, "Found game with alternative query: " + gameDate + ", score: " + score);
                                        }
                                    }
                                }
                                displayProfile(profile);
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e(TAG, "Alternative query also failed", e2);
                                displayProfile(profile);
                            });
                });
    }

    private void displayProfile(UserProfile profile) {
        showLoading(false);
        
        // Set user info
        nicknameText.setText(profile.getNickname());
        emailText.setText(profile.getEmail());
        
        // Set stats
        maxScoreText.setText(String.valueOf(profile.getMaxScore()));
        totalGamesText.setText(String.valueOf(profile.getTotalGames()));
        
        // Set last game info
        if (profile.hasLastGame()) {
            lastGameCard.setVisibility(View.VISIBLE);
            noLastGameCard.setVisibility(View.GONE);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
            String formattedDate = dateFormat.format(profile.getLastGameDate());
            
            lastGameDateText.setText(formattedDate);
            lastGameScoreText.setText(String.valueOf(profile.getLastGameScore()));
        } else {
            lastGameCard.setVisibility(View.GONE);
            noLastGameCard.setVisibility(View.VISIBLE);
        }
        
        profileContent.setVisibility(View.VISIBLE);
        Log.d(TAG, "Profile displayed successfully");
    }

    private void showLoading(boolean show) {
        loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        profileContent.setVisibility(show ? View.GONE : View.VISIBLE);
        errorText.setVisibility(View.GONE);
    }

    private void showError(String message) {
        loadingProgress.setVisibility(View.GONE);
        profileContent.setVisibility(View.GONE);
        errorText.setVisibility(View.VISIBLE);
        errorText.setText(message);
        Log.e(TAG, "Showing error: " + message);
    }
}
