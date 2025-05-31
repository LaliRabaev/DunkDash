package com.example.dunkdash;

public class LeaderboardPlayer {
    private int rank;
    private String nickname;
    private long maxScore;
    private long totalGames;

    public LeaderboardPlayer(int rank, String nickname, long maxScore, long totalGames) {
        this.rank = rank;
        this.nickname = nickname;
        this.maxScore = maxScore;
        this.totalGames = totalGames;
    }

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

    public boolean isTopThree() {
        return rank <= 3;
    }

    public String getTrophyEmoji() {
        switch (rank) {
            case 1: return "🥇";
            case 2: return "🥈";
            case 3: return "🥉";
            default: return "";
        }
    }
}
