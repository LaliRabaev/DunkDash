package com.example.dunkdash;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AudioTestActivity extends AppCompatActivity {
    private AudioManager audioManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create simple layout programmatically for testing
        setContentView(R.layout.activity_audio_test);
        
        audioManager = AudioManager.getInstance(this);
        
        setupTestButtons();
        
        // Start background music automatically
        Intent musicIntent = new Intent(this, MusicService.class);
        musicIntent.setAction("START_MUSIC");
        startService(musicIntent);
    }
    
    private void setupTestButtons() {
        Button shootButton = findViewById(R.id.testShootButton);
        Button scoreButton = findViewById(R.id.testScoreButton);
        Button missButton = findViewById(R.id.testMissButton);
        Button clickButton = findViewById(R.id.testClickButton);
        Button gameOverButton = findViewById(R.id.testGameOverButton);
        Button musicToggle = findViewById(R.id.testMusicToggle);
        
        // Check what audio files are available
        checkAudioFiles();
        
        shootButton.setOnClickListener(v -> {
            audioManager.playSound(AudioManager.SOUND_SHOOT);
            Toast.makeText(this, "Shoot sound played", Toast.LENGTH_SHORT).show();
        });
        
        scoreButton.setOnClickListener(v -> {
            audioManager.playSound(AudioManager.SOUND_SCORE);
            Toast.makeText(this, "Score sound played", Toast.LENGTH_SHORT).show();
        });
        
        missButton.setOnClickListener(v -> {
            audioManager.playSound(AudioManager.SOUND_MISS);
            Toast.makeText(this, "Miss sound played", Toast.LENGTH_SHORT).show();
        });
        
        clickButton.setOnClickListener(v -> {
            audioManager.playSound(AudioManager.SOUND_BUTTON_CLICK);
            Toast.makeText(this, "Click sound played", Toast.LENGTH_SHORT).show();
        });
        
        gameOverButton.setOnClickListener(v -> {
            audioManager.playSound(AudioManager.SOUND_GAME_OVER);
            Toast.makeText(this, "Game over sound played", Toast.LENGTH_SHORT).show();
        });
        
        musicToggle.setOnClickListener(v -> {
            Intent musicIntent = new Intent(this, MusicService.class);
            musicIntent.setAction("START_MUSIC");
            startService(musicIntent);
            Toast.makeText(this, "Background music started", Toast.LENGTH_SHORT).show();
        });
    }

    private void checkAudioFiles() {
        String[] audioFiles = {"background_music", "bg_music", "music", "background", 
                              "shoot_sound", "score_sound", "miss_sound", "click_sound", "game_over_sound"};
        
        StringBuilder foundFiles = new StringBuilder("Found audio files:\n");
        boolean foundAny = false;
        
        for (String fileName : audioFiles) {
            int resId = getResources().getIdentifier(fileName, "raw", getPackageName());
            if (resId != 0) {
                foundFiles.append("✓ ").append(fileName).append("\n");
                foundAny = true;
            }
        }
        
        if (!foundAny) {
            foundFiles.append("❌ No audio files found in res/raw directory");
        }
        
        Toast.makeText(this, foundFiles.toString(), Toast.LENGTH_LONG).show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioManager.cleanup();
    }
}
