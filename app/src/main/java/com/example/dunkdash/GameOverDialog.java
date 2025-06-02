package com.example.dunkdash;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class GameOverDialog extends Dialog {
    
    private final GameOverDialogListener listener;
    private final int currentScore;
    private final boolean canShowAd; // Whether we have a loaded ad ready to show
    
    // Simple callback for what happens when user makes their choice
    public interface GameOverDialogListener {
        void onRestartGame();     // Start completely over
        void onContinueWithAd();  // Watch ad to keep playing with current score
    }
    
    public GameOverDialog(Context context, GameOverDialogListener listener, 
                          int currentScore, boolean canShowAd) {
        super(context);
        this.listener = listener;
        this.currentScore = currentScore;
        this.canShowAd = canShowAd;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // Clean look without title bar
        setContentView(R.layout.dialog_game_over);
        
        // Don't let user dismiss this dialog - they need to make a choice
        setCancelable(false);
        setCanceledOnTouchOutside(false);
        
        // Show their final score
        TextView scoreTextView = findViewById(R.id.game_over_score);
        scoreTextView.setText("Score: " + currentScore);
        
        Button restartButton = findViewById(R.id.restart_button);
        Button watchAdButton = findViewById(R.id.watch_ad_button);
        
        // Restart from beginning - always available
        restartButton.setOnClickListener(v -> {
            dismiss();
            listener.onRestartGame();
        });
        
        // Continue with ad - only show if we actually have an ad loaded
        // No point showing the button if we can't deliver on the promise
        if (canShowAd) {
            watchAdButton.setVisibility(View.VISIBLE);
            watchAdButton.setOnClickListener(v -> {
                dismiss();
                listener.onContinueWithAd();
            });
        } else {
            // Hide the button if no ad is available
            watchAdButton.setVisibility(View.GONE);
        }
    }
}