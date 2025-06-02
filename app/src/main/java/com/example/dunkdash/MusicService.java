package com.example.dunkdash;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    
    public static final String ACTION_START_MUSIC = "START_MUSIC";
    public static final String ACTION_PAUSE_MUSIC = "PAUSE_MUSIC";
    public static final String ACTION_STOP_MUSIC = "STOP_MUSIC";
    
    private MediaPlayer mediaPlayer;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("DunkDashSettings", MODE_PRIVATE);
        Log.d(TAG, "MusicService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            Log.d(TAG, "Received action: " + action);
            
            if (ACTION_START_MUSIC.equals(action)) {
                startMusic();
            } else if (ACTION_PAUSE_MUSIC.equals(action)) {
                pauseMusic();
            } else if (ACTION_STOP_MUSIC.equals(action)) {
                stopMusic();
                stopSelf();
            }
        }
        
        return START_STICKY; // Restart if killed by system
    }

    private void startMusic() {
        // Check if music is enabled in settings
        boolean musicEnabled = prefs.getBoolean("background_music", true);
        if (!musicEnabled) {
            Log.d(TAG, "Music disabled in settings, not starting");
            return;
        }
        
        if (mediaPlayer == null) {
            try {
                // Try to find background music file
                String[] possibleNames = {"background_music", "bg_music", "music", "background"};
                int resId = 0;
                
                for (String name : possibleNames) {
                    resId = getResources().getIdentifier(name, "raw", getPackageName());
                    if (resId != 0) {
                        Log.d(TAG, "Found background music file: " + name);
                        break;
                    }
                }
                
                if (resId != 0) {
                    mediaPlayer = MediaPlayer.create(this, resId);
                    if (mediaPlayer != null) {
                        mediaPlayer.setLooping(true);
                        mediaPlayer.setVolume(0.3f, 0.3f); // Lower volume for background
                        Log.d(TAG, "MediaPlayer created successfully");
                    } else {
                        Log.e(TAG, "Failed to create MediaPlayer");
                        return;
                    }
                } else {
                    Log.w(TAG, "No background music file found");
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating MediaPlayer", e);
                return;
            }
        }
        
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.start();
                Log.d(TAG, "Background music started");
            } catch (Exception e) {
                Log.e(TAG, "Error starting music", e);
            }
        }
    }

    private void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.pause();
                Log.d(TAG, "Background music paused");
            } catch (Exception e) {
                Log.e(TAG, "Error pausing music", e);
            }
        }
    }

    private void stopMusic() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
                Log.d(TAG, "Background music stopped and released");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping music", e);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // We don't provide binding
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMusic();
        Log.d(TAG, "MusicService destroyed");
    }
}
