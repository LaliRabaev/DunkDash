package com.example.dunkdash;

import java.util.Date;

public class UserProfile {
    private String nickname;
    private String email;
    private long maxScore;
    private long totalGames;
    private Date lastGameDate;
    private long lastGameScore;
    private boolean hasLastGame;

    public UserProfile(String nickname, String email, long maxScore, long totalGames) {
        this.nickname = nickname;
        this.email = email;
        this.maxScore = maxScore;
        this.totalGames = totalGames;
        this.hasLastGame = false;
    }

    public String getNickname() {
        return nickname;
    }

    public String getEmail() {
        return email;
    }

    public long getMaxScore() {
        return maxScore;
    }

    public long getTotalGames() {
        return totalGames;
    }

    public Date getLastGameDate() {
        return lastGameDate;
    }

    public long getLastGameScore() {
        return lastGameScore;
    }

    public boolean hasLastGame() {
        return hasLastGame;
    }

    public void setLastGameInfo(Date date, long score) {
        this.lastGameDate = date;
        this.lastGameScore = score;
        this.hasLastGame = true;
    }
}
