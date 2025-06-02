package com.example.dunkdash;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class AudioManager {
    private static final String TAG = "AudioManager";
    private static AudioManager instance;
    
    private Context context;
    private SharedPreferences prefs;
    private MediaPlayer backgroundMusicPlayer;
    private SoundPool soundPool;
    private Map<String, Integer> soundEffects;
    
    // Sound effect IDs
    public static final String SOUND_SHOOT = "shoot";
    public static final String SOUND_SCORE = "score";
    public static final String SOUND_MISS = "miss";
    public static final String SOUND_BUTTON_CLICK = "button_click";
    public static final String SOUND_GAME_OVER = "game_over";
    
    private AudioManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences("DunkDashSettings", Context.MODE_PRIVATE);
        this.soundEffects = new HashMap<>();
        initializeSoundPool();
    }
    
    public static synchronized AudioManager getInstance(Context context) {
        if (instance == null) {
            instance = new AudioManager(context);
        }
        return instance;
    }
    
    private void initializeSoundPool() {
        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(5);
        soundPool = builder.build();
        
        // Load sound effects (you'll need to add these audio files to res/raw/)
        // For now, using system sounds as placeholders
        try {
            soundEffects.put(SOUND_SHOOT, soundPool.load(context, R.raw.shoot_sound, 1));
            soundEffects.put(SOUND_SCORE, soundPool.load(context, R.raw.score_sound, 1));
            soundEffects.put(SOUND_MISS, soundPool.load(context, R.raw.miss_sound, 1));
            soundEffects.put(SOUND_BUTTON_CLICK, soundPool.load(context, R.raw.button_click, 1));
            soundEffects.put(SOUND_GAME_OVER, soundPool.load(context, R.raw.game_over_sound, 1));
        } catch (Exception e) {
            Log.e(TAG, "Error loading sound effects", e);
        }
    }
    
    public void startBackgroundMusic() {
        if (!isMusicEnabled()) return;
        
        try {
            if (backgroundMusicPlayer == null) {
                backgroundMusicPlayer = MediaPlayer.create(context, R.raw.background_music);
                if (backgroundMusicPlayer != null) {
                    backgroundMusicPlayer.setLooping(true);
                    backgroundMusicPlayer.setVolume(0.3f, 0.3f); // Lower volume for background
                }
            }
            
            if (backgroundMusicPlayer != null && !backgroundMusicPlayer.isPlaying()) {
                backgroundMusicPlayer.start();
                Log.d(TAG, "Background music started");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting background music", e);
        }
    }
    
    public void pauseBackgroundMusic() {
        if (backgroundMusicPlayer != null && backgroundMusicPlayer.isPlaying()) {
            backgroundMusicPlayer.pause();
            Log.d(TAG, "Background music paused");
        }
    }
    
    public void stopBackgroundMusic() {
        if (backgroundMusicPlayer != null) {
            if (backgroundMusicPlayer.isPlaying()) {
                backgroundMusicPlayer.stop();
            }
            backgroundMusicPlayer.release();
            backgroundMusicPlayer = null;
            Log.d(TAG, "Background music stopped and released");
        }
    }
    
    public void playSound(String soundType) {
        if (!isSoundEffectsEnabled()) return;
        
        Integer soundId = soundEffects.get(soundType);
        if (soundId != null && soundPool != null) {
            soundPool.play(soundId, 0.7f, 0.7f, 1, 0, 1.0f);
        }
    }
    
    public void updateMusicSetting(boolean enabled) {
        if (enabled) {
            startBackgroundMusic();
        } else {
            pauseBackgroundMusic();
        }
    }
    
    private boolean isMusicEnabled() {
        return prefs.getBoolean("background_music", true);
    }
    
    private boolean isSoundEffectsEnabled() {
        return prefs.getBoolean("sound_effects", true);
    }
    
    public void cleanup() {
        stopBackgroundMusic();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
