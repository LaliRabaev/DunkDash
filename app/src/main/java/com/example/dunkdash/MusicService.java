package com.example.dunkdash;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private AudioManager audioManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = AudioManager.getInstance(this);
        Log.d(TAG, "Music service created");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("START_MUSIC".equals(action)) {
                audioManager.startBackgroundMusic();
            } else if ("PAUSE_MUSIC".equals(action)) {
                audioManager.pauseBackgroundMusic();
            } else if ("STOP_MUSIC".equals(action)) {
                audioManager.stopBackgroundMusic();
                stopSelf();
            }
        }
        return START_STICKY; // Restart if killed
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (audioManager != null) {
            audioManager.cleanup();
        }
        Log.d(TAG, "Music service destroyed");
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
}
