package com.example.dunkdash;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
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
    private int score = 0; // Use single score variable
    private boolean gameActive = true;

    // Firestore
    private FirebaseFirestore db;
    private String userId;

    // AdMob
    private RewardedAdManager rewardedAdManager;

    // Score
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

        // tap-to-jump listener - temporarily add score increment for testing
        findViewById(R.id.rootLayout).setOnClickListener(v -> {
            if (gameActive) {
                dy = JUMP_VELOCITY;
                // Temporary: add score on tap to test display
                score++;
                updateScoreDisplay();
                Log.d("GameActivity", "Tap! Score now: " + score);
            }
        });

        // Initialize AdMob properly
        RewardedAdManager.initialize(this);
        rewardedAdManager = new RewardedAdManager();
        rewardedAdManager.loadRewardedAd(this);

        // Initialize game
        initializeGame();
    }

    private void initializeGame() {
        // Always start fresh unless continuing from ad
        boolean continueGame = getIntent().getBooleanExtra("continue", false);
        if (continueGame) {
            score = getIntent().getIntExtra("score", 0);
        } else {
            score = 0;
        }
        updateScoreDisplay();
        gameActive = true;
    }

    private void updateScoreDisplay() {   Log.d("GameActivity", "TextView updated with: Score: " + score);
        if (scoreTextView != null) {   } else {
            scoreTextView.setText("Score: " + score);            Log.e("GameActivity", "scoreTextView is null!");
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {us) {
        super.onWindowFocusChanged(hasFocus);anged(hasFocus);
        if (hasFocus && !prepared) {red) {
            startTime = System.currentTimeMillis();em.currentTimeMillis();
            addCones("left");
            addCones("right");
            prepared = true;
            playerX = player.getX();   playerX = player.getX();
            playerY = player.getY();       playerY = player.getY();
            startGameLoop();            startGameLoop();
        }
    }

    private void startGameLoop() {{
        gameLoop = () -> {
            if (gameActive) {
                updateMovement();
                if (detectCollision()) {ctCollision()) {
                    handler.removeCallbacks(gameLoop);
                    onGameFail();   onGameFail();
                } else {   } else {
                    handler.postDelayed(gameLoop, FRAME_RATE_MS);          handler.postDelayed(gameLoop, FRAME_RATE_MS);
                }
            }       }
        };        };
        handler.postDelayed(gameLoop, FRAME_RATE_MS);eLoop, FRAME_RATE_MS);
    }

    private void onGameFail() {    private void onGameFail() {
        gameActive = false;
        handler.removeCallbacks(gameLoop);

        // Show game over dialog with current score and ad availabilityover dialog with current score and ad availability
        GameOverDialog dialog = new GameOverDialog(
                this,
                this,      this,
                score, // Use the correct score variable // Use the correct score variable
                rewardedAdManager.isRewardedAdLoaded()           rewardedAdManager.isRewardedAdLoaded()
        );        );
        dialog.show();
    }

    // GameOverDialogListener implementation
    @Override
    public void onRestartGame() {ublic void onRestartGame() {
        // Save current game stats before restarting        // Save current game stats before restarting
        saveGameAndFinish();ameAndFinish();
    }

    @Override
    public void onContinueWithAd() {
        rewardedAdManager.showRewardedAd(this, new RewardedAdManager.RewardedAdCallback() {dedAdManager.RewardedAdCallback() {
            @Override
            public void onAdRewarded() { {
                // User earned reward, continue the gameme
                runOnUiThread(() -> {
                    gameActive = true;
                    // Reset player position to centerosition to center
                    resetPlayerPosition();
                    // Clear current obstacles and regenerate obstacles and regenerate
                    resetObstacles();
                    // Restart the game loop
                    startGameLoop();
                    Toast.makeText(GameActivity.this, Toast.makeText(GameActivity.this,
                            "Continue with score: " + score,               "Continue with score: " + score,
                            Toast.LENGTH_SHORT).show();                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onAdDismissed() {d() {
                // User closed the ad without completing it/ User closed the ad without completing it
                if (!gameActive) {   if (!gameActive) {
                    onRestartGame();                    onRestartGame();
                }
            }

            @Override
            public void onAdFailedToLoad() {
                Toast.makeText(GameActivity.this,ameActivity.this,
                        "Ad failed to load. Restarting game.",           "Ad failed to load. Restarting game.",
                        Toast.LENGTH_SHORT).show();             Toast.LENGTH_SHORT).show();
                onRestartGame();           onRestartGame();
            }            }
        });
    }

    private void resetPlayerPosition() {
        // Reset player to center of screen// Reset player to center of screen
        int screenWidth = findViewById(R.id.rootLayout).getWidth();etWidth();
        int screenHeight = findViewById(R.id.rootLayout).getHeight();tHeight();
        
        playerX = (screenWidth - player.getWidth()) / 2f;Width()) / 2f;
        playerY = (screenHeight - player.getHeight()) / 2f;playerY = (screenHeight - player.getHeight()) / 2f;
        dx = 8f; // Reset horizontal velocityizontal velocity
        dy = 0f; // Reset vertical velocitytical velocity
           
        player.setX(playerX);        player.setX(playerX);
        player.setY(playerY);
    }

    private void resetObstacles() {
        // Clear existing obstaclesobstacles
        removeCones(leftContainer);removeCones(leftContainer);
        removeCones(rightContainer);tainer);
        sideCones.clear();
        
        // Reset side states// Reset side states
        leftActive = true;
        rightActive = true;e;
        
        // Add new obstacles   // Add new obstacles
        addCones("left");        addCones("left");
        addCones("right");
    }

    private void saveGameAndFinish() {veGameAndFinish() {
        if (userId == null) {f (userId == null) {
            finishToHome();
            return;
        }        }
        long now = System.currentTimeMillis();
        double dur = (now - startTime) / 1000.0; 1000.0;

        DocumentReference gameRef = db.collection("games").document();"games").document();
        String gameId = gameRef.getId();f.getId();

        Map<String, Object> doc = new HashMap<>();
        doc.put("id", gameId);
        doc.put("user_id", userId);d);
        doc.put("start_date", new Timestamp(new Date(startTime)));;
        doc.put("duration", dur);        doc.put("duration", dur);
        doc.put("game_mode", 1);de", 1);
        doc.put("score", score); // Use score instead of tapCount

        gameRef.set(doc)
                .addOnSuccessListener(unused -> updateUserStats())serStats())
                .addOnFailureListener(e -> {er(e -> {
                    Toast.makeText(this, "Save game failed: " + e.getMessage(), Toast.makeText(this, "Save game failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();                       Toast.LENGTH_SHORT).show();
                    finishToHome();                    finishToHome();
                });
    }

    private void updateUserStats() {
        DocumentReference userRef = db.collection("users").document(userId);
        db.runTransaction((Transaction.Function<Void>) tx -> {
                    DocumentSnapshot snap = tx.get(userRef);                    DocumentSnapshot snap = tx.get(userRef);
                    long total = snap.contains("total_games") ? snap.getLong("total_games") : 0;nap.getLong("total_games") : 0;
                    long max   = snap.contains("max_score")   ? snap.getLong("max_score")   : 0;contains("max_score")   ? snap.getLong("max_score")   : 0;

                    tx.update(userRef, "total_games", total + 1);x.update(userRef, "total_games", total + 1);
                    if (score > max) {max) {
                        tx.update(userRef, "max_score", score);
                    }
                    return null;               return null;
                }).addOnSuccessListener(aVoid -> finishToHome())                }).addOnSuccessListener(aVoid -> finishToHome())
                .addOnFailureListener(e -> finishToHome());ener(e -> finishToHome());
    }

    private void finishToHome() {
        // Return to home page instead of FailActivity instead of FailActivity
        Intent intent = new Intent(this, HomePageActivity.class);tent = new Intent(this, HomePageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);   intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);        startActivity(intent);
        finish();h();
    }

    @Override
    protected void onPause() {rotected void onPause() {
        super.onPause();        super.onPause();
        handler.removeCallbacks(gameLoop);er.removeCallbacks(gameLoop);
    }

    @Override
    protected void onResume() {
        super.onResume();uper.onResume();
        if (gameLoop != null && gameActive) {        if (gameLoop != null && gameActive) {
            handler.postDelayed(gameLoop, FRAME_RATE_MS);E_RATE_MS);
        }

        // Load ad in advance for next failure/ Load ad in advance for next failure
        if (!rewardedAdManager.isRewardedAdLoaded()) {   if (!rewardedAdManager.isRewardedAdLoaded()) {
            rewardedAdManager.loadRewardedAd(this);            rewardedAdManager.loadRewardedAd(this);
        }
    }

    private int dpToPx(int dp) {rivate int dpToPx(int dp) {
        float d = getResources().getDisplayMetrics().density;        float d = getResources().getDisplayMetrics().density;
        return Math.round(dp * d);
    }

    // ————— Dynamic Selection Loading —————
    private void loadUserSelections() {serSelections() {
        if (userId == null) return;
        db.collection("users").document(userId)
                .get()                .get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) return;

                    Long bgId   = userDoc.getLong("current_background");oc.getLong("current_background");
                    Long ballId = userDoc.getLong("current_basketball");urrent_basketball");

                    if (bgId != null) {
                        db.collection("backgrounds")"backgrounds")
                                .whereEqualTo("id", bgId)
                                .limit(1)           .limit(1)
                                .get()                                .get()
                                .addOnSuccessListener(this::applyBackground);cessListener(this::applyBackground);
                    }

                    if (ballId != null) {
                        db.collection("basketballs")"basketballs")
                                .whereEqualTo("id", ballId)
                                .limit(1)           .limit(1)
                                .get()             .get()
                                .addOnSuccessListener(this::applyBasketball);                           .addOnSuccessListener(this::applyBasketball);
                    }                    }
                });
    }

    private void applyBackground(QuerySnapshot qs) {
        if (qs.isEmpty()) return;
        DocumentSnapshot doc = qs.getDocuments().get(0);   DocumentSnapshot doc = qs.getDocuments().get(0);
        int resId = resolveDrawable(doc.getString("image_path"));        int resId = resolveDrawable(doc.getString("image_path"));
        if (resId != 0) gameBackground.setImageResource(resId);rce(resId);
    }

    private void applyBasketball(QuerySnapshot qs) {
        if (qs.isEmpty()) return;
        DocumentSnapshot doc = qs.getDocuments().get(0);   DocumentSnapshot doc = qs.getDocuments().get(0);
        int resId = resolveDrawable(doc.getString("image_path"));        int resId = resolveDrawable(doc.getString("image_path"));
        if (resId != 0) player.setImageResource(resId);e(resId);
    }

    private int resolveDrawable(String path) {
        if (path == null) return 0;   if (path == null) return 0;
        String name = path.replaceFirst("^drawable/", "").replaceFirst("\\.png$", "");("^drawable/", "").replaceFirst("\\.png$", "");
        return getResources().getIdentifier(name, "drawable", getPackageName());        return getResources().getIdentifier(name, "drawable", getPackageName());
    }
    // —————————————————————————————————————————————————

    private void updateMovement() {eMovement() {
        playerX += dx;        playerX += dx;
        playerY += dy;
        dy += GRAVITY;

        int pw = player.getWidth(), ph = player.getHeight();        int pw = player.getWidth(), ph = player.getHeight();
        int w  = player.getRootView().getWidth();Width();
        int h  = player.getRootView().getHeight();RootView().getHeight();

        // Track side bounces for scoring
        if (playerX <= 0) {
            dx = Math.abs(dx);
            incrementScore(); // Score when bouncing off left side", "Left side bounce! Score: " + score);
            swapSide(false, true);
        } else if (playerX + pw >= w) {
            dx = -Math.abs(dx); else if (playerX + pw >= w) {
            incrementScore(); // Score when bouncing off right side            dx = -Math.abs(dx);
            swapSide(true, false);ivity", "Right side bounce! Score: " + score);
        } Score when bouncing off right side
   swapSide(true, false);
        if (playerY < 0) {
            playerY = 0; dy = 0;
        }f (playerY < 0) {
            playerY = 0; dy = 0;        if (playerY + ph > h) {
        } dy = 0;
        if (playerY + ph > h) {
            playerY = h - ph; dy = 0;
        }        player.setX(playerX);

        player.setX(playerX);
        player.setY(playerY);
    }

    private void incrementScore() {   score++;
        if (gameActive) {       // Ensure UI update happens on main thread
            score++;            runOnUiThread(this::updateScoreDisplay);
            Log.d("GameActivity", "Score incremented to: " + score);
            // Post to main thread immediately
            runOnUiThread(() -> {
                updateScoreDisplay();
                Log.d("GameActivity", "Score display updated to: " + score);false; }
            });   if ( leftAdd && !leftActive) { addCones("left"); leftActive = true; }
        }        if (!rightAdd && rightActive){ removeCones(rightContainer); rightActive = false; }
    }addCones("right"); rightActive = true; }

    private void swapSide(boolean leftAdd, boolean rightAdd) {
        if (!leftAdd && leftActive)  { removeCones(leftContainer); leftActive = false; }
        if ( leftAdd && !leftActive) { addCones("left"); leftActive = true; }ide.equals("left") ? leftContainer : rightContainer;
        if (!rightAdd && rightActive){ removeCones(rightContainer); rightActive = false; }
        if ( rightAdd && !rightActive){ addCones("right"); rightActive = true; }        int count  = 1 + new Random().nextInt(4);
    }
count + 1);
    private void addCones(String side) {
        FrameLayout container = side.equals("left") ? leftContainer : rightContainer;
        int height = container.getHeight();
        int count  = 1 + new Random().nextInt(4);
        int coneH  = dpToPx(120);
        int gap    = (height - count * coneH) / (count + 1);T_XY);
arams(dpToPx(80), coneH);
        for (int i = 0; i < count; i++) {ft") ? Gravity.START : Gravity.END;
            ImageView cone = new ImageView(this);(i + 1) + coneH * i;
            cone.setImageResource(R.drawable.game_cone);   if (side.equals("right")) p.setMarginEnd(dpToPx(8));
            cone.setRotation(side.equals("left") ? 90 : -90);       container.addView(cone, p);
            cone.setScaleType(ImageView.ScaleType.FIT_XY);            sideCones.add(cone);
            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dpToPx(80), coneH);
            p.gravity = side.equals("left") ? Gravity.START : Gravity.END;
            p.topMargin = gap * (i + 1) + coneH * i;
            if (side.equals("right")) p.setMarginEnd(dpToPx(8));rivate void removeCones(FrameLayout container) {
            container.addView(cone, p);        container.removeAllViews();
            sideCones.add(cone);.getParent() == container);
        }
    }
te boolean detectCollision() {
    private void removeCones(FrameLayout container) {
        container.removeAllViews();(player, cone)) return true;
        sideCones.removeIf(view -> view.getParent() == container);
    } br = new Rect();

    private boolean detectCollision() {   topBarrierContainer.getHitRect(tr);
        for (ImageView cone : sideCones) {        bottomBarrierContainer.getHitRect(br);
            if (pixelCollision(player, cone)) return true;br);
        }
        Rect pr = new Rect(), tr = new Rect(), br = new Rect();
        player.getHitRect(pr);
        topBarrierContainer.getHitRect(tr);
        bottomBarrierContainer.getHitRect(br);                !(b.getDrawable() instanceof BitmapDrawable)) return false;
        return Rect.intersects(pr, tr) || Rect.intersects(pr, br);();
    }e)b.getDrawable()).getBitmap();

    private boolean pixelCollision(ImageView a, ImageView b) { new Rect();
        if (!(a.getDrawable() instanceof BitmapDrawable) ||        a.getGlobalVisibleRect(ra);
                !(b.getDrawable() instanceof BitmapDrawable)) return false;eRect(rb);
        Bitmap ba = ((BitmapDrawable)a.getDrawable()).getBitmap();
        Bitmap bb = ((BitmapDrawable)b.getDrawable()).getBitmap();

        Rect ra = new Rect(), rb = new Rect(), ri = new Rect();- ra.top;
        a.getGlobalVisibleRect(ra);top;
        b.getGlobalVisibleRect(rb);
        if (!ri.setIntersect(ra, rb)) return false;
ixel(oxA + x, oyA + y);
        int alphaTh = 50;nt pb = bb.getPixel(oxB + x, oyB + y);
        int oxA = ri.left - ra.left, oyA = ri.top - ra.top;   if (((pa >> 24) & 0xff) > alphaTh && ((pb >> 24) & 0xff) > alphaTh) {
        int oxB = ri.left - rb.left, oyB = ri.top - rb.top;           return true;
        for (int y = 0; y < ri.height(); y++) {
            for (int x = 0; x < ri.width(); x++) {       }
                int pa = ba.getPixel(oxA + x, oyA + y);       }









}    }        return false;        }            }                }                    return true;                if (((pa >> 24) & 0xff) > alphaTh && ((pb >> 24) & 0xff) > alphaTh) {                int pb = bb.getPixel(oxB + x, oyB + y);        return false;
    }
}