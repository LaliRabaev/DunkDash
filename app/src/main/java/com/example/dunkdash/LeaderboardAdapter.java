package com.example.dunkdash;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
    private List<LeaderboardPlayer> players = new ArrayList<>();

    public void updatePlayers(List<LeaderboardPlayer> newPlayers) {
        this.players = new ArrayList<>(newPlayers);
        notifyDataSetChanged();
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
        holder.bind(player);
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

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.rank_text);
            nicknameText = itemView.findViewById(R.id.nickname_text);
            maxScoreText = itemView.findViewById(R.id.max_score_text);
            totalGamesText = itemView.findViewById(R.id.total_games_text);
            trophyText = itemView.findViewById(R.id.trophy_text);
        }

        public void bind(LeaderboardPlayer player) {
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
            maxScoreText.setText(String.valueOf(player.getMaxScore()));
            totalGamesText.setText(player.getTotalGames() + " games");

            // Highlight top 3 players
            if (player.isTopThree()) {
                itemView.setBackgroundResource(R.drawable.leaderboard_top_background);
            } else {
                itemView.setBackgroundResource(R.drawable.leaderboard_normal_background);
            }
        }
    }
}
