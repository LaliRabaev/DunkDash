package com.example.dunkdash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

public class FailActivity extends AppCompatActivity {
    
    private static final String TAG = "FailActivity";
    private TextView scoreTextView;
    private Button restartButton;
    private Button watchAdButton;
    private int currentScore;
    private RewardedAd rewardedAd;
    private final String AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"; // Test ad unit ID
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fail);
        
        // Initialize AdMob
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                loadRewardedAd();
            }
        });
        
        // Get score from intent
        currentScore = getIntent().getIntExtra("score", 0);
        
        // Initialize UI elements
        scoreTextView = findViewById(R.id.scoreTextView);
        restartButton = findViewById(R.id.restart_button);
        watchAdButton = findViewById(R.id.watchAdButton);
        
        // Set actual score
        scoreTextView.setText("Score: " + currentScore);
        
        // Setup restart button - Improved implementation
        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show toast for debugging
                Toast.makeText(FailActivity.this, "Returning to Home Page...", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Restart button clicked, navigating to HomePageActivity");
                
                // Use a handler to add a slight delay before navigating
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        navigateToHomePage();
                    }
                }, 300); // 300ms delay to ensure UI responsiveness
            }
        });
        
        // Setup watch ad button
        watchAdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRewardedAd();
            }
        });
    }
    
    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, AD_UNIT_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                // Ad failed to load
                rewardedAd = null;
                watchAdButton.setEnabled(false);
                watchAdButton.setAlpha(0.5f);
            }
            
            @Override
            public void onAdLoaded(RewardedAd ad) {
                rewardedAd = ad;
                watchAdButton.setEnabled(true);
                watchAdButton.setAlpha(1.0f);
            }
        });
    }
    
    private void showRewardedAd() {
        if (rewardedAd != null) {
            rewardedAd.show(this, rewardItem -> {
                // User earned reward, continue the game
                continueGame();
            });
        } else {
            Toast.makeText(this, "Ad not ready yet. Please try again.", Toast.LENGTH_SHORT).show();
            loadRewardedAd();
        }
    }
    
    private void restartGame() {
        navigateToHomePage(); // Use the same reliable method
    }
    
    private void continueGame() {
        Intent intent = new Intent(FailActivity.this, GameActivity.class);
        intent.putExtra("continue", true);
        intent.putExtra("score", currentScore);
        startActivity(intent);
        finish();
    }
    
    // New method for reliable navigation to home page
    private void navigateToHomePage() {
        try {
            // Create a new task and clear any existing activities
            Intent intent = new Intent(FailActivity.this, HomePageActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                           Intent.FLAG_ACTIVITY_CLEAR_TASK |
                           Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            
            // Force activity transition animation to complete
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            
            // Make sure this activity is finished
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to home page: " + e.getMessage());
            // Fallback navigation method
            Intent intent = new Intent(FailActivity.this, HomePageActivity.class);
            startActivity(intent);
            finish();
        }
    }
    
    // Override back button to use our safe navigation method
    @Override
    public void onBackPressed() {
        navigateToHomePage();
    }
}