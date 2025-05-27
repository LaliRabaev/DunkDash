package com.example.dunkdash;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.util.Arrays;

public class RewardedAdManager {
    private static final String TAG = "RewardedAdManager";
    private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"; // Test ad unit ID

    private RewardedAd rewardedAd;
    private boolean isLoading = false;

    public interface RewardedAdCallback {
        void onAdRewarded();
        void onAdDismissed();
        void onAdFailedToLoad();
    }

    public static void initialize(Context context) {
        // Initialize the Mobile Ads SDK
        MobileAds.initialize(context, initializationStatus -> {
            Log.d(TAG, "MobileAds initialized");
        });

        // Set test device IDs for development
        RequestConfiguration configuration = new RequestConfiguration.Builder()
                .setTestDeviceIds(Arrays.asList("ABCDEF012345"))
                .build();
        MobileAds.setRequestConfiguration(configuration);
    }

    public void loadRewardedAd(Context context) {
        if (isLoading || isRewardedAdLoaded()) {
            return;
        }

        isLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(context, AD_UNIT_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.d(TAG, "Ad failed to load: " + loadAdError.getMessage());
                rewardedAd = null;
                isLoading = false;
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                Log.d(TAG, "Ad was loaded");
                rewardedAd = ad;
                isLoading = false;
            }
        });
    }

    public boolean isRewardedAdLoaded() {
        return rewardedAd != null;
    }

    // Changed parameter type from Context to Activity
    public void showRewardedAd(Activity activity, RewardedAdCallback callback) {
        if (!isRewardedAdLoaded()) {
            Log.d(TAG, "Ad not loaded yet");
            callback.onAdFailedToLoad();
            return;
        }

        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad was dismissed");
                callback.onAdDismissed();
                // Load the next ad
                loadRewardedAd(activity);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                Log.d(TAG, "Ad failed to show: " + adError.getMessage());
                rewardedAd = null;
                callback.onAdFailedToLoad();
                // Load the next ad
                loadRewardedAd(activity);
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed fullscreen content");
            }
        });

        rewardedAd.show(activity, new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                Log.d(TAG, "User earned reward");
                callback.onAdRewarded();
                rewardedAd = null; // Now it's safe to set to null after reward is earned
            }
        });
    }
}
