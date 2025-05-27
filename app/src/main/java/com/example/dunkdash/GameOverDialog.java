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
    private final boolean canShowAd;
    
    public interface GameOverDialogListener {
        void onRestartGame();
        void onContinueWithAd();
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
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_game_over);
        
        setCancelable(false);
        setCanceledOnTouchOutside(false);
        
        TextView scoreTextView = findViewById(R.id.game_over_score);
        scoreTextView.setText("Score: " + currentScore);
        
        Button restartButton = findViewById(R.id.restart_button);
        Button watchAdButton = findViewById(R.id.watch_ad_button);
        
        restartButton.setOnClickListener(v -> {
            dismiss();
            listener.onRestartGame();
        });
        
        // Only show the ad button if ads are available
        if (canShowAd) {
            watchAdButton.setVisibility(View.VISIBLE);
            watchAdButton.setOnClickListener(v -> {
                dismiss();
                listener.onContinueWithAd();
            });
        } else {
            watchAdButton.setVisibility(View.GONE);
        }
    }
}
