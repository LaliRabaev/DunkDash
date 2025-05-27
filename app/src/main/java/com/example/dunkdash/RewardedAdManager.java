package com.example.dunkdash;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;

public class RewardedAdManager {
    private static final String TAG = "RewardedAdManager";
    // Test ad unit ID - replace with your real ad unit ID in production
    private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";
    
    private RewardedAd rewardedAd;
    private boolean isLoading = false;
    
    public interface RewardedAdCallback {
        void onAdRewarded();
        void onAdDismissed();
        void onAdFailedToLoad();
    }
    
    // Initialize MobileAds SDK
    public static void initialize(Context context) {
        MobileAds.initialize(context, initializationStatus -> {});
    }
    
    // Load the rewarded ad
    public void loadRewardedAd(Context context) {
        if (rewardedAd != null || isLoading) {
            return;
        }
        
        isLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        
        RewardedAd.load(context, AD_UNIT_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                isLoading = false;
                rewardedAd = null;
            }
            
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                isLoading = false;
                rewardedAd = ad;
            }
        });
    }
    
    // Show the rewarded ad
    public void showRewardedAd(Activity activity, RewardedAdCallback callback) {
        if (rewardedAd == null) {
            callback.onAdFailedToLoad();
            loadRewardedAd(activity);
            return;
        }
        
        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
                loadRewardedAd(activity);
                callback.onAdDismissed();
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                rewardedAd = null;
                callback.onAdFailedToLoad();
            }
        });
        
        rewardedAd.show(activity, new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                // Handle the reward
                callback.onAdRewarded();
            }
        });
    }
    
    // Check if rewarded ad is loaded
    public boolean isRewardedAdLoaded() {
        return rewardedAd != null;
    }
}
