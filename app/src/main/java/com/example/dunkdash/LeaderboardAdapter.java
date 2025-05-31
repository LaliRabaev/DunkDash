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
    private String currentUserId;

    public void updatePlayers(List<LeaderboardPlayer> newPlayers) {
        this.players = new ArrayList<>(newPlayers);
        Log.d(TAG, "Updated players list with " + players.size() + " players, current user ID: " + currentUserId);
        notifyDataSetChanged();
    }

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
            // Handle rank/trophy display
            if (player.isTopThree()) {
                trophyText.setText(player.getTrophyEmoji());
                trophyText.setVisibility(View.VISIBLE);
                rankText.setVisibility(View.GONE);
            } else {
                trophyText.setVisibility(View.GONE);
                rankText.setText(String.valueOf(player.getRank()));
                rankText.setVisibility(View.VISIBLE);
            }

            // Set nickname and show/hide "You" indicator
            nicknameText.setText(player.getNickname());
            
            // Debug logging to check user ID matching
            Log.d("LeaderboardAdapter", "Binding player: " + player.getNickname() + 
                              " | PlayerID: '" + player.getUserId() + "'" +
                              " | CurrentUserID: '" + currentUserId + "'" +
                              " | IDs Equal: " + (currentUserId != null && currentUserId.equals(player.getUserId())) +
                              " | Player ID null: " + (player.getUserId() == null) +
                              " | Current ID null: " + (currentUserId == null));
            
            boolean isCurrentUser = currentUserId != null && 
                                  player.getUserId() != null && 
                                  currentUserId.trim().equals(player.getUserId().trim());
            
            if (isCurrentUser) {
                youIndicator.setVisibility(View.VISIBLE);
                Log.d("LeaderboardAdapter", "✓ Showing (You) indicator for: " + player.getNickname());
            } else {
                youIndicator.setVisibility(View.GONE);
            }

            maxScoreText.setText(String.valueOf(player.getMaxScore()));
            totalGamesText.setText(player.getTotalGames() + " games");

            // Set background and text colors based on rank and current user
            setBackgroundAndColors(player, currentUserId);
        }

        private void setBackgroundAndColors(LeaderboardPlayer player, String currentUserId) {
            boolean isCurrentUser = currentUserId != null && 
                                  player.getUserId() != null && 
                                  currentUserId.trim().equals(player.getUserId().trim());
            
            if (isCurrentUser) {
                // Current user gets special green background
                itemView.setBackgroundResource(R.drawable.leaderboard_current_user_background);
                maxScoreText.setTextColor(0xFF4CAF50); // Green
            } else {
                // Set background and score color based on rank
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
