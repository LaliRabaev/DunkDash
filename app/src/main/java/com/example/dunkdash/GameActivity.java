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

    // Physics - now with game mode scaling
    private float playerX, playerY, dx = 8f, dy = 0f;
    private float baseDx = 8f; // Store base speed for scaling
    private static final float GRAVITY = 0.5f, JUMP_VELOCITY = -10f;

    // State
    private boolean leftActive = true, rightActive = true, prepared = false;
    private long startTime;
    private int score = 0; // Use single score variable
    private boolean gameActive = true;
    // Add bounce detection to prevent multiple scoring
    private boolean justBouncedLeft = false, justBouncedRight = false;

    // Firestore
    private FirebaseFirestore db;
    private String userId;

    // AdMob
    private RewardedAdManager rewardedAdManager;

    // Score
    private TextView scoreTextView;

    // Add game mode and speed scaling
    private int gameMode = 1; // Default to mode 1
    private float speedMultiplier = 1.0f;
    private static final float[] GAME_MODE_SPEEDS = {1.0f, 1.3f, 1.6f, 2.0f}; // Speed for modes 1-4

    // Enhanced difficulty tracking
    private int difficultyLevel = 1;
    private long lastDifficultyUpdate = 0;
    private static final long DIFFICULTY_INTERVAL_MS = 8000; // Increase difficulty every 8 seconds (faster progression)
    private static final int MAX_DIFFICULTY = 15; // Increased max difficulty
    private float speedIncreasePerLevel = 0.1f; // Speed increases over time

    // Add countdown tracking
    private FrameLayout countdownOverlay;
    private TextView countdownText;
    private boolean isCountingDown = false;
    private int countdownValue = 3;
    private Handler countdownHandler = new Handler();
    private Runnable countdownRunnable;

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

        // bind countdown overlay
        countdownOverlay = findViewById(R.id.countdown_overlay);
        countdownText = findViewById(R.id.countdown_text);

        // Firebase
        db = FirebaseFirestore.getInstance();
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u != null) userId = u.getUid();

        // load dynamic selections
        loadUserSelections();

        // Get game mode from intent or database
        gameMode = getIntent().getIntExtra("game_mode", 1);

        // Load game mode from database if not provided in intent
        if (gameMode == 1) { // Default value, try to load from DB
            loadGameModeFromDatabase();
        } else {
            initializeSpeedForGameMode();
        }

        // tap-to-jump listener - only work when game is active and not counting down
        findViewById(R.id.rootLayout).setOnClickListener(v -> {
            if (gameActive && !isCountingDown) {
                dy = JUMP_VELOCITY;
                // NO score increment here - only on side bounces
                Log.d("GameActivity", "Tap! Jump only, score stays: " + score);
            }
        });

        // Initialize AdMob properly
        RewardedAdManager.initialize(this);
        rewardedAdManager = new RewardedAdManager();
        rewardedAdManager.loadRewardedAd(this);

        // Initialize game
        initializeGame();
    }

    private void loadGameModeFromDatabase() {
        if (userId == null) {
            initializeSpeedForGameMode();
            return;
        }

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists() && userDoc.contains("current_game_mode")) {
                        Long modeFromDb = userDoc.getLong("current_game_mode");
                        if (modeFromDb != null) {
                            gameMode = modeFromDb.intValue();
                        }
                    }
                    initializeSpeedForGameMode();
                })
                .addOnFailureListener(e -> {
                    Log.w("GameActivity", "Failed to load game mode from database", e);
                    initializeSpeedForGameMode();
                });
    }

    private void initializeSpeedForGameMode() {
        // Validate game mode
        if (gameMode < 1 || gameMode > 4) {
            gameMode = 1;
        }

        // Set speed multiplier based on game mode
        speedMultiplier = GAME_MODE_SPEEDS[gameMode - 1];
        baseDx = 8f * speedMultiplier;
        dx = dx > 0 ? baseDx : -baseDx; // Maintain direction

        Log.d("GameActivity", "Game mode: " + gameMode + ", Speed multiplier: " + speedMultiplier);
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

    private void updateScoreDisplay() {
        if (scoreTextView != null) {
            scoreTextView.setText("Score: " + score);
            Log.d("GameActivity", "TextView updated with: Score: " + score);
        } else {
            Log.e("GameActivity", "scoreTextView is null!");
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !prepared) {
            startTime = System.currentTimeMillis();
            addCones("left");
            addCones("right");
            prepared = true;
            playerX = player.getX();
            playerY = player.getY();

            // Start countdown instead of immediately starting game
            startCountdown(() -> startGameLoop());
        }
    }

    private void startCountdown(Runnable onComplete) {
        isCountingDown = true;
        countdownValue = 3;
        countdownOverlay.setVisibility(View.VISIBLE);
        countdownText.setText(String.valueOf(countdownValue));

        Log.d("GameActivity", "Starting countdown...");

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (countdownValue > 1) {
                    countdownValue--;
                    countdownText.setText(String.valueOf(countdownValue));
                    countdownHandler.postDelayed(this, 1000);
                } else if (countdownValue == 1) {
                    countdownText.setText("GO!");
                    countdownValue--;
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    // Countdown finished
                    isCountingDown = false;
                    countdownOverlay.setVisibility(View.GONE);
                    Log.d("GameActivity", "Countdown finished, starting game");
                    onComplete.run();
                }
            }
        };

        countdownHandler.postDelayed(countdownRunnable, 1000);
    }

    private void startGameLoop() {
        gameLoop = () -> {
            if (gameActive) {
                updateMovement();
                if (detectCollision()) {
                    handler.removeCallbacks(gameLoop);
                    onGameFail();
                } else {
                    handler.postDelayed(gameLoop, FRAME_RATE_MS);
                }
            }
        };
        handler.postDelayed(gameLoop, FRAME_RATE_MS);
    }

    private void onGameFail() {
        gameActive = false;
        handler.removeCallbacks(gameLoop);

        // Show game over dialog with current score and ad availability
        GameOverDialog dialog = new GameOverDialog(
                this,
                this,
                score, // Use the correct score variable
                rewardedAdManager.isRewardedAdLoaded()
        );
        dialog.show();
    }

    // GameOverDialogListener implementation
    @Override
    public void onRestartGame() {
        // Save current game stats before restarting
        saveGameAndFinish();
    }

    @Override
    public void onContinueWithAd() {
        rewardedAdManager.showRewardedAd(this, new RewardedAdManager.RewardedAdCallback() {
            @Override
            public void onAdRewarded() {
                // User earned reward, continue the game
                runOnUiThread(() -> {
                    gameActive = true;
                    // Reset player position to center
                    resetPlayerPosition();
                    // Clear current obstacles and regenerate
                    resetObstacles();

                    // Start countdown before continuing game
                    startCountdown(() -> startGameLoop());

                    Toast.makeText(GameActivity.this,
                            "Continue with score: " + score,
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onAdDismissed() {
                // User closed the ad without completing it
                if (!gameActive) {
                    onRestartGame();
                }
            }

            @Override
            public void onAdFailedToLoad() {
                Toast.makeText(GameActivity.this,
                        "Ad failed to load. Restarting game.",
                        Toast.LENGTH_SHORT).show();
                onRestartGame();
            }
        });
    }

    private void resetPlayerPosition() {
        // Reset player to center of screen
        int screenWidth = findViewById(R.id.rootLayout).getWidth();
        int screenHeight = findViewById(R.id.rootLayout).getHeight();

        playerX = (screenWidth - player.getWidth()) / 2f;
        playerY = (screenHeight - player.getHeight()) / 2f;
        dx = 8f; // Reset horizontal velocity
        dy = 0f; // Reset vertical velocity

        // Reset bounce flags
        justBouncedLeft = false;
        justBouncedRight = false;

        player.setX(playerX);
        player.setY(playerY);
    }

    private void resetObstacles() {
        // Clear existing obstacles
        removeCones(leftContainer);
        removeCones(rightContainer);
        sideCones.clear();

        // Reset side states
        leftActive = true;
        rightActive = true;

        // Reset difficulty and speed when restarting
        difficultyLevel = 1;
        lastDifficultyUpdate = 0;
        initializeSpeedForGameMode(); // Reset to base speed for game mode

        // Add new obstacles
        addCones("left");
        addCones("right");
    }

    private void saveGameAndFinish() {
        if (userId == null) {
            finishToHome();
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
        doc.put("game_mode", gameMode); // Save the actual game mode used
        doc.put("score", score);
        doc.put("difficulty_reached", difficultyLevel); // Track max difficulty reached

        gameRef.set(doc)
                .addOnSuccessListener(unused -> updateUserStats())
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Save game failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finishToHome();
                });
    }

    private void updateUserStats() {
        DocumentReference userRef = db.collection("users").document(userId);
        db.runTransaction((Transaction.Function<Void>) tx -> {
                    DocumentSnapshot snap = tx.get(userRef);
                    long total = snap.contains("total_games") ? snap.getLong("total_games") : 0;
                    long max   = snap.contains("max_score")   ? snap.getLong("max_score")   : 0;

                    tx.update(userRef, "total_games", total + 1);
                    if (score > max) {
                        tx.update(userRef, "max_score", score);
                    }
                    return null;
                }).addOnSuccessListener(aVoid -> finishToHome())
                .addOnFailureListener(e -> finishToHome());
    }

    private void finishToHome() {
        // Return to home page instead of FailActivity
        Intent intent = new Intent(this, HomePageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(gameLoop);
        countdownHandler.removeCallbacks(countdownRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameLoop != null && gameActive && !isCountingDown) {
            handler.postDelayed(gameLoop, FRAME_RATE_MS);
        }

        // Load ad in advance for next failure
        if (!rewardedAdManager.isRewardedAdLoaded()) {
            rewardedAdManager.loadRewardedAd(this);
        }
    }

    private int dpToPx(int dp) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(dp * d);
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

    private void updateMovement() {
        // Don't update movement during countdown
        if (isCountingDown) {
            return;
        }

        // Update difficulty based on time
        updateDifficulty();

        playerX += dx;
        playerY += dy;
        dy += GRAVITY;

        int pw = player.getWidth(), ph = player.getHeight();
        int w  = player.getRootView().getWidth();
        int h  = player.getRootView().getHeight();

        // Track side bounces for scoring with bounce detection
        if (playerX <= 0) {
            dx = Math.abs(getCurrentSpeed());
            // Only score if we haven't just bounced left
            if (!justBouncedLeft) {
                Log.d("GameActivity", "Left side bounce! Score: " + score);
                incrementScore();
                justBouncedLeft = true;
                justBouncedRight = false; // Reset right bounce flag
            }
            swapSide(false, true);
        } else if (playerX + pw >= w) {
            dx = -Math.abs(getCurrentSpeed());
            // Only score if we haven't just bounced right
            if (!justBouncedRight) {
                Log.d("GameActivity", "Right side bounce! Score: " + score);
                incrementScore();
                justBouncedRight = true;
                justBouncedLeft = false; // Reset left bounce flag
            }
            swapSide(true, false);
        } else {
            // Reset bounce flags when not touching sides
            if (playerX > 10 && playerX + pw < w - 10) {
                justBouncedLeft = false;
                justBouncedRight = false;
            }
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

    private float getCurrentSpeed() {
        // Calculate current speed based on base speed, game mode, and difficulty progression
        float difficultySpeedBoost = (difficultyLevel - 1) * speedIncreasePerLevel;
        return baseDx + (baseDx * difficultySpeedBoost);
    }

    private void updateDifficulty() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;

        // Check if it's time to increase difficulty
        if (elapsedTime - lastDifficultyUpdate >= DIFFICULTY_INTERVAL_MS && difficultyLevel < MAX_DIFFICULTY) {
            difficultyLevel++;
            lastDifficultyUpdate = elapsedTime;

            // Update speed based on new difficulty
            float newSpeed = getCurrentSpeed();
            dx = dx > 0 ? newSpeed : -newSpeed;

            // More frequent obstacle regeneration at higher difficulties
            if (difficultyLevel > 5) {
                // Regenerate obstacles more frequently for dynamic challenge
                if (leftActive) {
                    removeCones(leftContainer);
                    addCones("left");
                }
                if (rightActive) {
                    removeCones(rightContainer);
                    addCones("right");
                }
            } else {
                // Standard regeneration for lower difficulties
                if (leftActive) {
                    removeCones(leftContainer);
                    addCones("left");
                }
                if (rightActive) {
                    removeCones(rightContainer);
                    addCones("right");
                }
            }

            Log.d("GameActivity", "Difficulty increased to level: " + difficultyLevel + ", Speed: " + newSpeed);

            // Show difficulty increase feedback
            runOnUiThread(() -> {
                Toast.makeText(this, "Difficulty Level " + difficultyLevel + "!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void incrementScore() {
        if (gameActive) {
            score++;
            Log.d("GameActivity", "Score incremented to: " + score);
            // Post to main thread immediately
            runOnUiThread(() -> {
                updateScoreDisplay();
                Log.d("GameActivity", "Score display updated to: " + score);
            });
        }
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

        // Enhanced difficulty-based cone count with game mode consideration
        int baseMinCones = Math.max(1, gameMode - 1); // Harder modes start with more cones
        int baseMaxCones = Math.max(2, gameMode + 1);

        int minCones = Math.min(baseMinCones + (difficultyLevel - 1) / 2, 8);
        int maxCones = Math.min(baseMaxCones + (difficultyLevel - 1) / 2, 12);
        int count = minCones + new Random().nextInt(maxCones - minCones + 1);

        // Cone size varies with difficulty and game mode
        int baseConeHeight = gameMode <= 2 ? 120 : 100; // Smaller cones for harder modes
        int coneH = dpToPx(baseConeHeight - (difficultyLevel - 1) * 2); // Gradually smaller cones
        coneH = Math.max(dpToPx(60), coneH); // Minimum cone size

        // More aggressive gap reduction for higher difficulties and game modes
        int baseGap = dpToPx(Math.max(60, 120 - (gameMode - 1) * 15));
        int difficultyReduction = (difficultyLevel - 1) * dpToPx(12);
        int minGap = dpToPx(15); // Tighter minimum gap
        int gap = Math.max(minGap, baseGap - difficultyReduction);

        // Calculate if we need to adjust cone spacing
        int totalConeSpace = count * coneH + (count + 1) * gap;
        if (totalConeSpace > height) {
            gap = Math.max(minGap, (height - count * coneH) / (count + 1));
        }

        List<Integer> positions = generateConePositions(height, count, coneH, gap);

        for (int i = 0; i < count; i++) {
            ImageView cone = new ImageView(this);
            cone.setImageResource(R.drawable.game_cone);
            cone.setRotation(side.equals("left") ? 90 : -90);
            cone.setScaleType(ImageView.ScaleType.FIT_XY);

            // Cone width also varies with difficulty
            int coneWidth = dpToPx(Math.max(50, 80 - (difficultyLevel - 1)));
            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(coneWidth, coneH);
            p.gravity = side.equals("left") ? Gravity.START : Gravity.END;
            p.topMargin = positions.get(i);

            if (side.equals("right")) p.setMarginEnd(dpToPx(8));
            container.addView(cone, p);
            sideCones.add(cone);
        }
    }

    private List<Integer> generateConePositions(int containerHeight, int coneCount, int coneHeight, int gap) {
        List<Integer> positions = new ArrayList<>();
        Random random = new Random();

        // Game mode affects starting pattern complexity
        int complexityThreshold = Math.max(1, 4 - gameMode); // Harder modes have complex patterns sooner

        if (difficultyLevel <= complexityThreshold) {
            // Early levels: Simple evenly spaced cones
            for (int i = 0; i < coneCount; i++) {
                positions.add(gap * (i + 1) + coneHeight * i);
            }
        } else if (difficultyLevel <= complexityThreshold + 3) {
            // Mid levels: Some clustering and irregular spacing
            generateIrregularSpacing(positions, containerHeight, coneCount, coneHeight, gap, random);
        } else if (difficultyLevel <= complexityThreshold + 8) {
            // High levels: Create challenging patterns
            generateChallengingPatterns(positions, containerHeight, coneCount, coneHeight, gap, random);
        } else {
            // Extreme levels: Maximum difficulty patterns
            generateExtremePatterns(positions, containerHeight, coneCount, coneHeight, gap, random);
        }

        return positions;
    }

    private void generateIrregularSpacing(List<Integer> positions, int containerHeight, int coneCount, int coneHeight, int gap, Random random) {
        int usableHeight = containerHeight - coneCount * coneHeight - gap * 2;
        int currentY = gap;

        for (int i = 0; i < coneCount; i++) {
            if (i == 0) {
                positions.add(currentY);
            } else {
                int minSpacing = gap / 3;
                int maxSpacing = gap * 2;
                int spacing = minSpacing + random.nextInt(maxSpacing - minSpacing + 1);
                currentY += coneHeight + spacing;

                if (currentY + coneHeight > containerHeight - gap) {
                    currentY = containerHeight - gap - coneHeight;
                }
                positions.add(currentY);
            }
        }
    }

    private void generateChallengingPatterns(List<Integer> positions, int containerHeight, int coneCount, int coneHeight, int gap, Random random) {
        if (difficultyLevel % 3 == 0) {
            // Create narrow passages
            generateNarrowPassages(positions, containerHeight, coneCount, coneHeight, gap, random);
        } else if (difficultyLevel % 3 == 1) {
            // Cluster at edges
            generateEdgeClusters(positions, containerHeight, coneCount, coneHeight, gap);
        } else {
            // Zigzag pattern
            generateZigzagPattern(positions, containerHeight, coneCount, coneHeight, gap);
        }
    }

    private void generateExtremePatterns(List<Integer> positions, int containerHeight, int coneCount, int coneHeight, int gap, Random random) {
        // Multiple narrow passages with irregular spacing
        int passageCount = Math.min(3, difficultyLevel - 10);
        int passageHeight = containerHeight / (passageCount + 1);

        for (int passage = 0; passage < passageCount; passage++) {
            int passageStart = passage * passageHeight + passageHeight / 3;
            int passageEnd = passageStart + passageHeight / 6; // Very narrow passages

            // Fill areas between passages with cones
            int conesForThisSection = coneCount / passageCount;
            for (int i = 0; i < conesForThisSection && positions.size() < coneCount; i++) {
                int y;
                do {
                    y = random.nextInt(passageHeight) + passage * passageHeight;
                } while (y >= passageStart && y <= passageEnd);

                if (y + coneHeight < containerHeight - gap) {
                    positions.add(y);
                }
            }
        }

        // Fill any remaining cone slots randomly in safe areas
        while (positions.size() < coneCount) {
            int y = random.nextInt(containerHeight - coneHeight - gap * 2) + gap;
            boolean validPosition = true;

            for (int existingY : positions) {
                if (Math.abs(y - existingY) < coneHeight + gap / 3) {
                    validPosition = false;
                    break;
                }
            }

            if (validPosition) {
                positions.add(y);
            }
        }
    }

    private void generateNarrowPassages(List<Integer> positions, int containerHeight, int coneCount, int coneHeight, int gap, Random random) {
        int passageStart = containerHeight / 3;
        int passageEnd = 2 * containerHeight / 3;
        int passageWidth = Math.max(coneHeight, containerHeight / 8); // Very narrow

        // Fill top area
        int topCones = coneCount / 2;
        for (int i = 0; i < topCones; i++) {
            int y = gap + i * (coneHeight + gap / 4);
            if (y + coneHeight < passageStart) {
                positions.add(y);
            }
        }

        // Fill bottom area
        int bottomCones = coneCount - topCones;
        for (int i = 0; i < bottomCones; i++) {
            int y = passageEnd + gap + i * (coneHeight + gap / 4);
            if (y + coneHeight < containerHeight - gap) {
                positions.add(y);
            }
        }
    }

    private void generateEdgeClusters(List<Integer> positions, int containerHeight, int coneCount, int coneHeight, int gap) {
        int conesPerCluster = coneCount / 2;
        int remainingCones = coneCount % 2;

        // Top cluster
        for (int i = 0; i < conesPerCluster; i++) {
            positions.add(gap + i * (coneHeight + gap / 4));
        }

        // Bottom cluster
        int bottomStart = containerHeight - gap - (conesPerCluster + remainingCones) * coneHeight - (conesPerCluster + remainingCones - 1) * gap / 4;
        for (int i = 0; i < conesPerCluster + remainingCones; i++) {
            positions.add(bottomStart + i * (coneHeight + gap / 4));
        }
    }

    private void generateZigzagPattern(List<Integer> positions, int containerHeight, int coneCount, int coneHeight, int gap) {
        boolean topSide = true;
        int currentY = gap;

        for (int i = 0; i < coneCount; i++) {
            if (topSide) {
                positions.add(currentY);
                currentY += coneHeight * 2 + gap;
            } else {
                int bottomY = containerHeight - gap - coneHeight - (i / 2) * (coneHeight + gap);
                positions.add(Math.max(currentY, bottomY));
            }
            topSide = !topSide;
        }
    }

    private void removeCones(FrameLayout container) {
        container.removeAllViews();
        sideCones.removeIf(view -> view.getParent() == container);
    }

    private boolean detectCollision() {
        // Skip collision detection during countdown
        if (isCountingDown) {
            return false;
        }

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
}