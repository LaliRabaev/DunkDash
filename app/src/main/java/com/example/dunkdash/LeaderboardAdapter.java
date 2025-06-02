package com.example.dunkdash;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
    private static final String TAG = "LeaderboardAdapter";
    private List<LeaderboardPlayer> players = new ArrayList<>();
    private String currentUserId; // So we can highlight the current user

    // Update the entire list when new data comes from Firebase
    public void updatePlayers(List<LeaderboardPlayer> newPlayers) {
        this.players = new ArrayList<>(newPlayers);
        Log.d(TAG, "Updated players list with " + players.size() + " players, current user ID: " + currentUserId);
        notifyDataSetChanged(); // Tell RecyclerView to refresh everything
    }

    // Set the current user's ID so we can show "(You)" next to their entry
    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
        Log.d(TAG, "Set current user ID to: " + userId);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard_player, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardPlayer player = players.get(position);
        holder.bind(player, currentUserId);
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView rankText;
        private TextView nicknameText;
        private TextView maxScoreText;
        private TextView totalGamesText;
        private TextView trophyText;
        private TextView youIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.rank_text);
            nicknameText = itemView.findViewById(R.id.nickname_text);
            maxScoreText = itemView.findViewById(R.id.max_score_text);
            totalGamesText = itemView.findViewById(R.id.total_games_text);
            trophyText = itemView.findViewById(R.id.trophy_text);
            youIndicator = itemView.findViewById(R.id.you_indicator);
        }

        public void bind(LeaderboardPlayer player, String currentUserId) {
            // Show trophy for top 3, regular rank number for everyone else
            if (player.isTopThree()) {
                trophyText.setText(player.getTrophyEmoji());
                trophyText.setVisibility(View.VISIBLE);
                rankText.setVisibility(View.GONE);
            } else {
                trophyText.setVisibility(View.GONE);
                rankText.setText(String.valueOf(player.getRank()));
                rankText.setVisibility(View.VISIBLE);
            }

            nicknameText.setText(player.getNickname());
            
            // Debug logging to figure out why "(You)" indicator might not show
            // This was probably added to fix a bug where users couldn't find themselves
            Log.d("LeaderboardAdapter", "Binding player: " + player.getNickname() + 
                              " | PlayerID: '" + player.getUserId() + "'" +
                              " | CurrentUserID: '" + currentUserId + "'" +
                              " | IDs Equal: " + (currentUserId != null && currentUserId.equals(player.getUserId())) +
                              " | Player ID null: " + (player.getUserId() == null) +
                              " | Current ID null: " + (currentUserId == null));
            
            // Check if this is the current user - need to be careful with null checks and whitespace
            boolean isCurrentUser = currentUserId != null && 
                                  player.getUserId() != null && 
                                  currentUserId.trim().equals(player.getUserId().trim());
            
            // Show "(You)" indicator so user can easily find themselves
            if (isCurrentUser) {
                youIndicator.setVisibility(View.VISIBLE);
                Log.d("LeaderboardAdapter", "✓ Showing (You) indicator for: " + player.getNickname());
            } else {
                youIndicator.setVisibility(View.GONE);
            }

            maxScoreText.setText(String.valueOf(player.getMaxScore()));
            totalGamesText.setText(player.getTotalGames() + " games");

            // Set colors and backgrounds based on rank and if it's current user
            setBackgroundAndColors(player, currentUserId);
        }

        private void setBackgroundAndColors(LeaderboardPlayer player, String currentUserId) {
            boolean isCurrentUser = currentUserId != null && 
                                  player.getUserId() != null && 
                                  currentUserId.trim().equals(player.getUserId().trim());
            
            if (isCurrentUser) {
                // Current user gets special green highlighting - easy to spot yourself
                itemView.setBackgroundResource(R.drawable.leaderboard_current_user_background);
                maxScoreText.setTextColor(0xFF4CAF50); // Green
            } else {
                // Everyone else gets rank-based colors - gold/silver/bronze for top 3
                switch (player.getRank()) {
                    case 1:
                        itemView.setBackgroundResource(R.drawable.leaderboard_gold_background);
                        maxScoreText.setTextColor(0xFFFFD700); // Gold
                        break;
                    case 2:
                        itemView.setBackgroundResource(R.drawable.leaderboard_silver_background);
                        maxScoreText.setTextColor(0xFFC0C0C0); // Silver
                        break;
                    case 3:
                        itemView.setBackgroundResource(R.drawable.leaderboard_bronze_background);
                        maxScoreText.setTextColor(0xFFCD7F32); // Bronze
                        break;
                    default:
                        itemView.setBackgroundResource(R.drawable.leaderboard_normal_background);
                        maxScoreText.setTextColor(0xFFCCCCCC); // Light gray
                        break;
                }
            }
        }
    }
}