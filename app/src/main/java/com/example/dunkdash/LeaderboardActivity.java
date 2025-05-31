package com.example.dunkdash;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {
    private static final String TAG = "LeaderboardActivity";
    private static final int TOP_PLAYERS_LIMIT = 50;

    private RecyclerView leaderboardRecyclerView;
    private LeaderboardAdapter adapter;
    private ProgressBar loadingProgress;
    private TextView emptyStateText;
    private ImageButton backButton;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        initViews();
        setupRecyclerView();

        // Check authentication before loading leaderboard
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            loadLeaderboard();
        } else {
            showEmptyState(true);
            emptyStateText.setText("Please log in to view leaderboard");
        }
    }

    private void initViews() {
        leaderboardRecyclerView = findViewById(R.id.leaderboard_recycler_view);
        loadingProgress = findViewById(R.id.loading_progress);
        emptyStateText = findViewById(R.id.empty_state_text);
        backButton = findViewById(R.id.back_button);

        db = FirebaseFirestore.getInstance();

        backButton.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new LeaderboardAdapter();
        leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        leaderboardRecyclerView.setAdapter(adapter);
    }

    private void loadLeaderboard() {
        if (currentUser == null) {
            Log.w(TAG, "User not authenticated");
            showEmptyState(true);
            emptyStateText.setText("Please log in to view leaderboard");
            return;
        }

        Log.d(TAG, "Loading leaderboard for user: " + currentUser.getUid());
        showLoading(true);

        db.collection("users")
                .orderBy("max_score", Query.Direction.DESCENDING)
                .limit(TOP_PLAYERS_LIMIT)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Leaderboard query successful, got " + querySnapshot.size() + " documents");
                    List<LeaderboardPlayer> players = new ArrayList<>();
                    int rank = 1;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Long maxScore = doc.getLong("max_score");
                        Long totalGames = doc.getLong("total_games");

                        // Skip users with no max score
                        if (maxScore == null || maxScore == 0) continue;

                        String nickname = doc.getString("nickname");
                        if (nickname == null || nickname.trim().isEmpty()) {
                            nickname = doc.getString("name");
                        }
                        if (nickname == null || nickname.trim().isEmpty()) {
                            nickname = "Anonymous Player";
                        }

                        LeaderboardPlayer player = new LeaderboardPlayer(
                                rank,
                                nickname,
                                maxScore,
                                totalGames != null ? totalGames : 0L
                        );
                        players.add(player);
                        rank++;
                    }

                    showLoading(false);
                    if (players.isEmpty()) {
                        showEmptyState(true);
                    } else {
                        showEmptyState(false);
                        adapter.updatePlayers(players);
                    }

                    Log.d(TAG, "Loaded " + players.size() + " players for leaderboard");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load leaderboard: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
                    showLoading(false);

                    // Check if it's a permission error
                    if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                        showEmptyState(true);
                        emptyStateText.setText("Unable to access leaderboard.\nFirestore permissions need updating.");
                        Log.e(TAG, "PERMISSION_DENIED: Update Firestore rules to allow leaderboard queries");
                    } else {
                        showEmptyState(true);
                        emptyStateText.setText("Failed to load leaderboard.\nPlease try again later.");
                    }
                });
    }

    private void showLoading(boolean show) {
        loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        leaderboardRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(boolean show) {
        emptyStateText.setVisibility(show ? View.VISIBLE : View.GONE);
        leaderboardRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
