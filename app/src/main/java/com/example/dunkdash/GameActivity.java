package com.example.dunkdash;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameActivity extends AppCompatActivity implements GameOverDialog.GameOverDialogListener {

    // Dynamic views
    private ImageView gameBackground;
    private ImageView player;

    // Obstacles
    private FrameLayout leftContainer, rightContainer;
    private View topBarrierContainer, bottomBarrierContainer;
    private final List<ImageView> sideCones = new ArrayList<>();

    // Game loop
    private Handler handler = new Handler();
    private Runnable gameLoop;
    private static final int FRAME_RATE_MS = 16;

    // Physics
    private float playerX, playerY, dx = 8f, dy = 0f;
    private static final float GRAVITY = 0.5f, JUMP_VELOCITY = -10f;

    // State
    private boolean leftActive = true, rightActive = true, prepared = false;
    private long startTime;
    private int tapCount;

    // Firestore
    private FirebaseFirestore db;
    private String userId;

    // AdMob
    private RewardedAdManager rewardedAdManager;
    private int currentScore = 0;
    private boolean gameActive = true;

    // Score
    private int score;
    private TextView scoreTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // bind dynamic views
        gameBackground       = findViewById(R.id.game_background);
        player               = findViewById(R.id.player_basketball);
        scoreTextView        = findViewById(R.id.score_text_view);

        // bind obstacles
        leftContainer        = findViewById(R.id.left_cones_container);
        rightContainer       = findViewById(R.id.right_cones_container);
        topBarrierContainer  = findViewById(R.id.top_barrier_container);
        bottomBarrierContainer = findViewById(R.id.bottom_barrier_container);

        // Firebase
        db = FirebaseFirestore.getInstance();
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u != null) userId = u.getUid();

        // load dynamic selections
        loadUserSelections();

        // tap-to-jump listener
        findViewById(R.id.rootLayout).setOnClickListener(v -> {
            dy = JUMP_VELOCITY;
            tapCount++;
        });

        // Initialize AdMob
        RewardedAdManager.initialize(this);
        rewardedAdManager = new RewardedAdManager();
        rewardedAdManager.loadRewardedAd(this);

        // Initialize game
        initializeGame();
    }

    private void initializeGame() {
        // Check if continuing from an ad
        boolean continueGame = getIntent().getBooleanExtra("continue", false);
        if (continueGame) {
            // Restore the previous score
            score = getIntent().getIntExtra("score", 0);
            updateScoreDisplay();
            // Additional logic to reset game state but keep the score
        } else {
            // Normal game initialization
            score = 0;
            updateScoreDisplay();
            // Other initialization
        }

        // Rest of your initialization code
    }

    private void updateScoreDisplay() {
        // Update your score TextView
        scoreTextView.setText("Score: " + score);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !prepared) {
            startTime = System.currentTimeMillis();
            tapCount  = 0;
            addCones("left");
            addCones("right");
            prepared = true;
            playerX = player.getX();
            playerY = player.getY();
            startGameLoop();
        }
    }

    // ————— Dynamic Selection Loading —————
    private void loadUserSelections() {
        if (userId == null) return;
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) return;

                    Long bgId   = userDoc.getLong("current_background");
                    Long ballId = userDoc.getLong("current_basketball");

                    if (bgId != null) {
                        db.collection("backgrounds")
                                .whereEqualTo("id", bgId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(this::applyBackground);
                    }

                    if (ballId != null) {
                        db.collection("basketballs")
                                .whereEqualTo("id", ballId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(this::applyBasketball);
                    }
                });
    }

    private void applyBackground(QuerySnapshot qs) {
        if (qs.isEmpty()) return;
        DocumentSnapshot doc = qs.getDocuments().get(0);
        int resId = resolveDrawable(doc.getString("image_path"));
        if (resId != 0) gameBackground.setImageResource(resId);
    }

    private void applyBasketball(QuerySnapshot qs) {
        if (qs.isEmpty()) return;
        DocumentSnapshot doc = qs.getDocuments().get(0);
        int resId = resolveDrawable(doc.getString("image_path"));
        if (resId != 0) player.setImageResource(resId);
    }

    private int resolveDrawable(String path) {
        if (path == null) return 0;
        String name = path.replaceFirst("^drawable/", "").replaceFirst("\\.png$", "");
        return getResources().getIdentifier(name, "drawable", getPackageName());
    }
    // ————————————————————————————————

    private void startGameLoop() {
        gameLoop = () -> {
            updateMovement();
            if (detectCollision()) {
                handler.removeCallbacks(gameLoop);
                onGameFail();
            } else {
                handler.postDelayed(gameLoop, FRAME_RATE_MS);
            }
        };
        handler.postDelayed(gameLoop, FRAME_RATE_MS);
    }

    private void updateMovement() {
        playerX += dx;
        playerY += dy;
        dy += GRAVITY;

        int pw = player.getWidth(), ph = player.getHeight();
        int w  = player.getRootView().getWidth();
        int h  = player.getRootView().getHeight();

        if (playerX <= 0) {
            dx = Math.abs(dx);
            swapSide(false, true);
        } else if (playerX + pw >= w) {
            dx = -Math.abs(dx);
            swapSide(true, false);
        }

        if (playerY < 0) {
            playerY = 0; dy = 0;
        }
        if (playerY + ph > h) {
            playerY = h - ph; dy = 0;
        }

        player.setX(playerX);
        player.setY(playerY);
    }

    private void swapSide(boolean leftAdd, boolean rightAdd) {
        if (!leftAdd && leftActive)  { removeCones(leftContainer); leftActive = false; }
        if ( leftAdd && !leftActive) { addCones("left"); leftActive = true; }
        if (!rightAdd && rightActive){ removeCones(rightContainer); rightActive = false; }
        if ( rightAdd && !rightActive){ addCones("right"); rightActive = true; }
    }

    private void addCones(String side) {
        FrameLayout container = side.equals("left") ? leftContainer : rightContainer;
        int height = container.getHeight();
        int count  = 1 + new Random().nextInt(4);
        int coneH  = dpToPx(120);
        int gap    = (height - count * coneH) / (count + 1);

        for (int i = 0; i < count; i++) {
            ImageView cone = new ImageView(this);
            cone.setImageResource(R.drawable.game_cone);
            cone.setRotation(side.equals("left") ? 90 : -90);
            cone.setScaleType(ImageView.ScaleType.FIT_XY);
            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dpToPx(80), coneH);
            p.gravity = side.equals("left") ? Gravity.START : Gravity.END;
            p.topMargin = gap * (i + 1) + coneH * i;
            if (side.equals("right")) p.setMarginEnd(dpToPx(8));
            container.addView(cone, p);
            sideCones.add(cone);
        }
    }

    private void removeCones(FrameLayout container) {
        container.removeAllViews();
        sideCones.removeIf(view -> view.getParent() == container);
    }

    private boolean detectCollision() {
        for (ImageView cone : sideCones) {
            if (pixelCollision(player, cone)) return true;
        }
        Rect pr = new Rect(), tr = new Rect(), br = new Rect();
        player.getHitRect(pr);
        topBarrierContainer.getHitRect(tr);
        bottomBarrierContainer.getHitRect(br);
        return Rect.intersects(pr, tr) || Rect.intersects(pr, br);
    }

    private boolean pixelCollision(ImageView a, ImageView b) {
        if (!(a.getDrawable() instanceof BitmapDrawable) ||
                !(b.getDrawable() instanceof BitmapDrawable)) return false;
        Bitmap ba = ((BitmapDrawable)a.getDrawable()).getBitmap();
        Bitmap bb = ((BitmapDrawable)b.getDrawable()).getBitmap();

        Rect ra = new Rect(), rb = new Rect(), ri = new Rect();
        a.getGlobalVisibleRect(ra);
        b.getGlobalVisibleRect(rb);
        if (!ri.setIntersect(ra, rb)) return false;

        int alphaTh = 50;
        int oxA = ri.left - ra.left, oyA = ri.top - ra.top;
        int oxB = ri.left - rb.left, oyB = ri.top - rb.top;
        for (int y = 0; y < ri.height(); y++) {
            for (int x = 0; x < ri.width(); x++) {
                int pa = ba.getPixel(oxA + x, oyA + y);
                int pb = bb.getPixel(oxB + x, oyB + y);
                if (((pa >> 24) & 0xff) > alphaTh && ((pb >> 24) & 0xff) > alphaTh) {
                    return true;
                }
            }
        }
        return false;
    }

    private void saveGameAndFinish() {
        if (userId == null) {
            finishFail();
            return;
        }
        long now = System.currentTimeMillis();
        double dur = (now - startTime) / 1000.0;

        DocumentReference gameRef = db.collection("games").document();
        String gameId = gameRef.getId();

        Map<String, Object> doc = new HashMap<>();
        doc.put("id", gameId);
        doc.put("user_id", userId);
        doc.put("start_date", new Timestamp(new Date(startTime)));
        doc.put("duration", dur);
        doc.put("game_mode", 1);
        doc.put("score", tapCount);

        gameRef.set(doc)
                .addOnSuccessListener(unused -> updateUserStats())
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Save game failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finishFail();
                });
    }

    private void updateUserStats() {
        DocumentReference userRef = db.collection("users").document(userId);
        db.runTransaction((Transaction.Function<Void>) tx -> {
                    DocumentSnapshot snap = tx.get(userRef);
                    long total = snap.contains("total_games") ? snap.getLong("total_games") : 0;
                    long max   = snap.contains("max_score")   ? snap.getLong("max_score")   : 0;

                    tx.update(userRef, "total_games", total + 1);
                    if (tapCount > max) {
                        tx.update(userRef, "max_score", tapCount);
                    }
                    return null;
                }).addOnSuccessListener(aVoid -> finishFail())
                .addOnFailureListener(e -> finishFail());
    }

    private void finishFail() {
        startActivity(new Intent(this, FailActivity.class));
        finish();
    }

    private void handleGameOver() {
        Intent intent = new Intent(GameActivity.this, FailActivity.class);
        intent.putExtra("score", score);
        startActivity(intent);
        finish();
    }

    private void onGameFail() {
        gameActive = false;

        // Show game over dialog with option to continue via ad
        GameOverDialog dialog = new GameOverDialog(
                this,
                this,
                currentScore,
                rewardedAdManager.isRewardedAdLoaded()
        );
        dialog.show();
    }

    // GameOverDialogListener implementation
    @Override
    public void onRestartGame() {
        // Reset game state
        currentScore = 0;
        gameActive = true;
        // Additional reset logic...
    }

    @Override
    public void onContinueWithAd() {
        rewardedAdManager.showRewardedAd(this, new RewardedAdManager.RewardedAdCallback() {
            @Override
            public void onAdRewarded() {
                // User earned reward, continue the game without resetting score
                gameActive = true;
                // Resume game from current position...
                runOnUiThread(() -> Toast.makeText(GameActivity.this,
                        "Continue with current score: " + currentScore,
                        Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onAdDismissed() {
                // User closed the ad without completing it - handle accordingly
                if (!gameActive) {
                    // If game is still not active, restart
                    onRestartGame();
                }
            }

            @Override
            public void onAdFailedToLoad() {
                // Ad failed to load, restart game as fallback
                Toast.makeText(GameActivity.this,
                        "Ad failed to load. Restarting game.",
                        Toast.LENGTH_SHORT).show();
                onRestartGame();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(gameLoop);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameLoop != null) handler.postDelayed(gameLoop, FRAME_RATE_MS);

        // Load ad in advance for next failure
        if (!rewardedAdManager.isRewardedAdLoaded()) {
            rewardedAdManager.loadRewardedAd(this);
        }
    }

    private int dpToPx(int dp) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(dp * d);
    }
}