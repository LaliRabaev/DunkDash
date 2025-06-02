package com.example.dunkdash;

// Simple data holder for a player's leaderboard entry
// Contains everything we need to display them in the list
public class LeaderboardPlayer {
    private int rank;
    private String nickname;
    private long maxScore;
    private long totalGames;
    private String userId; // For identifying current user

    public LeaderboardPlayer(int rank, String nickname, long maxScore, long totalGames, String userId) {
        this.rank = rank;
        this.nickname = nickname;
        this.maxScore = maxScore;
        this.totalGames = totalGames;
        this.userId = userId;
    }

    // Basic getters - nothing fancy here
    public int getRank() {
        return rank;
    }

    public String getNickname() {
        return nickname;
    }

    public long getMaxScore() {
        return maxScore;
    }

    public long getTotalGames() {
        return totalGames;
    }

    public String getUserId() {
        return userId;
    }

    // Helper to check if this player gets a trophy instead of a number
    public boolean isTopThree() {
        return rank <= 3;
    }

    // Get the appropriate trophy emoji for top 3 players
    // Makes the leaderboard more fun and visual
    public String getTrophyEmoji() {
        switch (rank) {
            case 1: return "🥇"; // Gold medal
            case 2: return "🥈"; // Silver medal  
            case 3: return "🥉"; // Bronze medal
            default: return ""; // No trophy for others
        }
    }
}