package com.example.dunkdash;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.media.ToneGenerator;
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
    private ToneGenerator toneGenerator;
    
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
        initializeToneGenerator();
    }
    
    public static synchronized AudioManager getInstance(Context context) {
        if (instance == null) {
            instance = new AudioManager(context);
        }
        return instance;
    }
    
    private void initializeToneGenerator() {
        try {
            toneGenerator = new ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 50);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing tone generator", e);
        }
    }
    
    private void initializeSoundPool() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
                
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();
        
        // Load actual sound files if they exist
        loadSoundEffects();
        
        Log.d(TAG, "Sound pool initialized");
    }

    private void loadSoundEffects() {
        try {
            // Try to load sound files from res/raw directory
            int shootId = context.getResources().getIdentifier("shoot_sound", "raw", context.getPackageName());
            int scoreId = context.getResources().getIdentifier("score_sound", "raw", context.getPackageName());
            int missId = context.getResources().getIdentifier("miss_sound", "raw", context.getPackageName());
            int clickId = context.getResources().getIdentifier("click_sound", "raw", context.getPackageName());
            int gameOverId = context.getResources().getIdentifier("game_over_sound", "raw", context.getPackageName());
            
            if (shootId != 0) {
                soundEffects.put(SOUND_SHOOT, soundPool.load(context, shootId, 1));
                Log.d(TAG, "Loaded shoot sound");
            } else {
                soundEffects.put(SOUND_SHOOT, -1);
            }
            
            if (scoreId != 0) {
                soundEffects.put(SOUND_SCORE, soundPool.load(context, scoreId, 1));
                Log.d(TAG, "Loaded score sound");
            } else {
                soundEffects.put(SOUND_SCORE, -1);
            }
            
            if (missId != 0) {
                soundEffects.put(SOUND_MISS, soundPool.load(context, missId, 1));
                Log.d(TAG, "Loaded miss sound");
            } else {
                soundEffects.put(SOUND_MISS, -1);
            }
            
            if (clickId != 0) {
                soundEffects.put(SOUND_BUTTON_CLICK, soundPool.load(context, clickId, 1));
                Log.d(TAG, "Loaded click sound");
            } else {
                soundEffects.put(SOUND_BUTTON_CLICK, -1);
            }
            
            if (gameOverId != 0) {
                soundEffects.put(SOUND_GAME_OVER, soundPool.load(context, gameOverId, 1));
                Log.d(TAG, "Loaded game over sound");
            } else {
                soundEffects.put(SOUND_GAME_OVER, -1);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading sound effects", e);
            // Initialize with fallback values
            soundEffects.put(SOUND_SHOOT, -1);
            soundEffects.put(SOUND_SCORE, -1);
            soundEffects.put(SOUND_MISS, -1);
            soundEffects.put(SOUND_BUTTON_CLICK, -1);
            soundEffects.put(SOUND_GAME_OVER, -1);
        }
    }
    
    public void startBackgroundMusic() {
        if (!isMusicEnabled()) {
            Log.d(TAG, "Background music disabled in settings");
            return;
        }
        
        try {
            if (backgroundMusicPlayer == null) {
                // Try multiple possible names for background music file
                String[] possibleNames = {"background_music", "bg_music", "music", "background"};
                int resId = 0;
                
                for (String name : possibleNames) {
                    resId = context.getResources().getIdentifier(name, "raw", context.getPackageName());
                    if (resId != 0) {
                        Log.d(TAG, "Found background music file: " + name);
                        break;
                    }
                }
                
                if (resId != 0) {
                    backgroundMusicPlayer = MediaPlayer.create(context, resId);
                    if (backgroundMusicPlayer != null) {
                        backgroundMusicPlayer.setLooping(true);
                        backgroundMusicPlayer.setVolume(0.5f, 0.5f);
                        Log.d(TAG, "Background music MediaPlayer created successfully");
                    } else {
                        Log.e(TAG, "Failed to create MediaPlayer for background music");
                        return;
                    }
                } else {
                    Log.w(TAG, "No background music file found in res/raw directory");
                    return;
                }
            }
            
            if (backgroundMusicPlayer != null && !backgroundMusicPlayer.isPlaying()) {
                backgroundMusicPlayer.start();
                Log.d(TAG, "Background music started successfully");
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
        if (!isSoundEffectsEnabled()) {
            Log.d(TAG, "Sound effects disabled in settings");
            return;
        }
        
        Integer soundId = soundEffects.get(soundType);
        if (soundId != null && soundId != -1) {
            try {
                soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
                Log.d(TAG, "Played sound effect: " + soundType);
            } catch (Exception e) {
                Log.e(TAG, "Error playing sound effect: " + soundType, e);
                playToneFallback(soundType);
            }
        } else {
            Log.d(TAG, "Sound file not found for: " + soundType + ", using tone fallback");
            playToneFallback(soundType);
        }
    }
    
    private void playToneFallback(String soundType) {
        if (toneGenerator == null) return;
        
        try {
            switch (soundType) {
                case SOUND_SHOOT:
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100);
                    break;
                case SOUND_SCORE:
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                    break;
                case SOUND_MISS:
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 150);
                    break;
                case SOUND_BUTTON_CLICK:
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 50);
                    break;
                case SOUND_GAME_OVER:
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 300);
                    break;
            }
            Log.d(TAG, "Played tone fallback for: " + soundType);
        } catch (Exception e) {
            Log.e(TAG, "Error playing tone", e);
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
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }
}