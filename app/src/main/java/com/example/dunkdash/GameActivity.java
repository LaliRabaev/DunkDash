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

    // Core UI elements that change during gameplay
    private ImageView gameBackground;
    private ImageView player;

    // Obstacle containers - left and right sides have different cone patterns
    private FrameLayout leftContainer, rightContainer;
    private View topBarrierContainer, bottomBarrierContainer;
    private final List<ImageView> sideCones = new ArrayList<>();

    // Game loop runs at ~60fps for smooth animation
    private Handler handler = new Handler();
    private Runnable gameLoop;
    private static final int FRAME_RATE_MS = 16; // About 60 FPS

    // Physics constants - tweaked through lots of playtesting
    private float playerX, playerY, dx = 8f, dy = 0f;
    private float baseDx = 8f; // Base horizontal speed before difficulty scaling
    private static final float GRAVITY = 0.5f, JUMP_VELOCITY = -10f;

    // Game state tracking
    private boolean leftActive = true, rightActive = true, prepared = false;
    private long startTime;
    private int score = 0;
    private boolean gameActive = true;
    
    // Prevent double-scoring when bouncing off walls
    private boolean justBouncedLeft = false, justBouncedRight = false;

    // Firebase for saving game data
    private FirebaseFirestore db;
    private String userId;

    // Ad system for continue feature
    private RewardedAdManager rewardedAdManager;

    // UI elements
    private TextView scoreTextView;
    private TextView gameModeTextView;

    // Game difficulty system - gets harder over time
    private int gameMode = 1; // 1=Easy, 2=Medium, 3=Hard, 4=Extreme
    private float speedMultiplier = 1.0f;
    private static final float[] GAME_MODE_SPEEDS = {1.0f, 1.3f, 1.6f, 2.0f}; // Speed multipliers

    // Progressive difficulty within each game mode
    private int difficultyLevel = 1;
    private long lastDifficultyUpdate = 0;
    private static final long DIFFICULTY_INTERVAL_MS = 8000; // Increase every 8 seconds
    private static final int MAX_DIFFICULTY = 15;
    private float speedIncreasePerLevel = 0.1f; // Speed gradually increases

    // Countdown system for game start
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

        // Bind all the UI elements
        gameBackground       = findViewById(R.id.game_background);
        player               = findViewById(R.id.player_basketball);
        scoreTextView        = findViewById(R.id.score_text_view);
        gameModeTextView     = findViewById(R.id.game_mode_text_view);

        // Obstacle containers
        leftContainer        = findViewById(R.id.left_cones_container);
        rightContainer       = findViewById(R.id.right_cones_container);
        topBarrierContainer  = findViewById(R.id.top_barrier_container);
        bottomBarrierContainer = findViewById(R.id.bottom_barrier_container);

        // Countdown overlay
        countdownOverlay = findViewById(R.id.countdown_overlay);
        countdownText = findViewById(R.id.countdown_text);

        // Firebase setup
        db = FirebaseFirestore.getInstance();
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u != null) userId = u.getUid();

        // Load user's selected background and basketball
        loadUserSelections();

        // Figure out what game mode we should use
        gameMode = getIntent().getIntExtra("game_mode", -1);

        if (gameMode == -1) {
            // No mode specified, load from user's saved preference
            loadGameModeFromDatabase();
        } else {
            Log.d("GameActivity", "Game mode from intent: " + gameMode);
            initializeSpeedForGameMode();
        }

        // Tap to jump - only works when game is running (not during countdown)
        findViewById(R.id.rootLayout).setOnClickListener(v -> {
            if (gameActive && !isCountingDown) {
                dy = JUMP_VELOCITY;
                // Important: Don't score on tap! Only score on side bounces
                Log.d("GameActivity", "Tap! Jump only, score stays: " + score);
            }
        });

        // Set up ad system for continue feature
        RewardedAdManager.initialize(this);
        rewardedAdManager = new RewardedAdManager();
        rewardedAdManager.loadRewardedAd(this);

        initializeGame();
    }

    private void loadGameModeFromDatabase() {
        if (userId == null) {
            Log.w("GameActivity", "No user ID, using default game mode");
            gameMode = 1;
            initializeSpeedForGameMode();
            return;
        }

        Log.d("GameActivity", "Loading game mode from database for user: " + userId);

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        if (userDoc.contains("current_game_mode")) {
                            Long modeFromDb = userDoc.getLong("current_game_mode");
                            if (modeFromDb != null) {
                                gameMode = modeFromDb.intValue();
                                Log.d("GameActivity", "Loaded game mode from database: " + gameMode);
                            } else {
                                Log.w("GameActivity", "current_game_mode field is null, using default");
                                gameMode = 1;
                            }
                        } else {
                            Log.w("GameActivity", "current_game_mode field doesn't exist, using default");
                            gameMode = 1;
                        }
                    } else {
                        Log.w("GameActivity", "User document doesn't exist, using default mode");
                        gameMode = 1;
                    }
                    initializeSpeedForGameMode();
                })
                .addOnFailureListener(e -> {
                    Log.e("GameActivity", "Failed to load game mode from database", e);
                    gameMode = 1; // Safe fallback
                    initializeSpeedForGameMode();
                });
    }

    private void initializeSpeedForGameMode() {
        // Validate game mode is in expected range
        if (gameMode < 1 || gameMode > 4) {
            Log.w("GameActivity", "Invalid game mode: " + gameMode + ", using default");
            gameMode = 1;
        }

        // Set base speed based on selected difficulty
        speedMultiplier = GAME_MODE_SPEEDS[gameMode - 1];
        baseDx = 8f * speedMultiplier;
        dx = dx > 0 ? baseDx : -baseDx; // Keep direction but update speed

        Log.d("GameActivity", "Initialized - Game mode: " + gameMode + ", Speed multiplier: " + speedMultiplier + ", Base speed: " + baseDx);

        updateGameModeDisplay();
    }

    // Start 3-2-1-GO countdown before game begins
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
                    // Countdown finished, start the actual game
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
        // Main game loop - runs every 16ms for smooth 60fps animation
        gameLoop = () -> {
            if (gameActive) {
                updateMovement();
                if (detectCollision()) {
                    // Game over! Stop the loop and show options
                    handler.removeCallbacks(gameLoop);
                    onGameFail();
                } else {
                    // Keep going
                    handler.postDelayed(gameLoop, FRAME_RATE_MS);
                }
            }
        };
        handler.postDelayed(gameLoop, FRAME_RATE_MS);
    }

    private void onGameFail() {
        gameActive = false;
        handler.removeCallbacks(gameLoop);

        // Show game over dialog with restart and continue options
        GameOverDialog dialog = new GameOverDialog(
                this,
                this,
                score,
                rewardedAdManager.isRewardedAdLoaded() // Only show ad option if we have an ad ready
        );
        dialog.show();
    }

    // GameOverDialogListener implementation
    @Override
    public void onRestartGame() {
        // Save the game stats and go back to home
        saveGameAndFinish();
    }

    @Override
    public void onContinueWithAd() {
        // Show rewarded ad, if user completes it they can continue playing
        rewardedAdManager.showRewardedAd(this, new RewardedAdManager.RewardedAdCallback() {
            @Override
            public void onAdRewarded() {
                // User watched the ad! Let them continue with their current score
                runOnUiThread(() -> {
                    gameActive = true;
                    resetPlayerPosition();
                    resetObstacles();

                    // Another countdown before resuming
                    startCountdown(() -> startGameLoop());

                    Toast.makeText(GameActivity.this,
                            "Continue with score: " + score,
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onAdDismissed() {
                // User closed ad without watching - treat as restart
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
        // Put player back in center when continuing after ad
        int screenWidth = findViewById(R.id.rootLayout).getWidth();
        int screenHeight = findViewById(R.id.rootLayout).getHeight();

        playerX = (screenWidth - player.getWidth()) / 2f;
        playerY = (screenHeight - player.getHeight()) / 2f;

        // Use current speed (including difficulty progression)
        float currentSpeed = getCurrentSpeed();
        dx = dx > 0 ? currentSpeed : -currentSpeed;
        dy = 0f;

        // Reset bounce detection
        justBouncedLeft = false;
        justBouncedRight = false;

        player.setX(playerX);
        player.setY(playerY);

        Log.d("GameActivity", "Reset player position with speed: " + currentSpeed + " for mode: " + gameMode);
    }

    private void resetObstacles() {
        // Clear all obstacles and regenerate them
        removeCones(leftContainer);
        removeCones(rightContainer);
        sideCones.clear();

        // Reset side states
        leftActive = true;
        rightActive = true;

        // Reset difficulty progression
        difficultyLevel = 1;
        lastDifficultyUpdate = 0;
        initializeSpeedForGameMode();

        // Add fresh obstacles
        addCones("left");
        addCones("right");
    }

    private void updateMovement() {
        // Don't move during countdown
        if (isCountingDown) {
            return;
        }

        // Gradually make game harder as time passes
        updateDifficulty();

        // Update player position
        playerX += dx;
        playerY += dy;
        dy += GRAVITY; // Gravity pulls player down

        int pw = player.getWidth(), ph = player.getHeight();
        int w  = player.getRootView().getWidth();
        int h  = player.getRootView().getHeight();

        // Check for side bounces - this is how player scores points!
        if (playerX <= 0) {
            dx = Math.abs(getCurrentSpeed()); // Bounce right
            // Only score if we haven't just bounced (prevents double-scoring)
            if (!justBouncedLeft) {
                Log.d("GameActivity", "Left side bounce! Score: " + score);
                incrementScore();
                justBouncedLeft = true;
                justBouncedRight = false;
            }
            swapSide(false, true); // Update obstacle pattern
        } else if (playerX + pw >= w) {
            dx = -Math.abs(getCurrentSpeed()); // Bounce left
            if (!justBouncedRight) {
                Log.d("GameActivity", "Right side bounce! Score: " + score);
                incrementScore();
                justBouncedRight = true;
                justBouncedLeft = false;
            }
            swapSide(true, false);
        } else {
            // Reset bounce flags when away from sides
            if (playerX > 10 && playerX + pw < w - 10) {
                justBouncedLeft = false;
                justBouncedRight = false;
            }
        }

        // Keep player on screen vertically
        if (playerY < 0) {
            playerY = 0; dy = 0;
        }
        if (playerY + ph > h) {
            playerY = h - ph; dy = 0;
        }

        // Apply position to the actual view
        player.setX(playerX);
        player.setY(playerY);
    }

    private float getCurrentSpeed() {
        // Speed increases with difficulty level for extra challenge
        float difficultySpeedBoost = (difficultyLevel - 1) * speedIncreasePerLevel;
        return baseDx + (baseDx * difficultySpeedBoost);
    }

    private void updateDifficulty() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;

        // Check if it's time to crank up the difficulty
        if (elapsedTime - lastDifficultyUpdate >= DIFFICULTY_INTERVAL_MS && difficultyLevel < MAX_DIFFICULTY) {
            difficultyLevel++;
            lastDifficultyUpdate = elapsedTime;

            // Update speed and regenerate obstacles for new challenge
            float newSpeed = getCurrentSpeed();
            dx = dx > 0 ? newSpeed : -newSpeed;

            // Higher difficulties get more frequent obstacle changes
            if (difficultyLevel > 5) {
                // Chaos mode - constantly changing patterns
                if (leftActive) {
                    removeCones(leftContainer);
                    addCones("left");
                }
                if (rightActive) {
                    removeCones(rightContainer);
                    addCones("right");
                }
            } else {
                // Standard regeneration
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

            // Give player feedback about difficulty increase
            runOnUiThread(() -> {
                Toast.makeText(this, "Difficulty Level " + difficultyLevel + "!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void incrementScore() {
        if (gameActive) {
            score++;
            Log.d("GameActivity", "Score incremented to: " + score);
            // Update UI immediately on main thread
            runOnUiThread(() -> {
                updateScoreDisplay();
                Log.d("GameActivity", "Score display updated to: " + score);
            });
        }
    }

    private void addCones(String side) {
        FrameLayout container = side.equals("left") ? leftContainer : rightContainer;
        int height = container.getHeight();

        // Cone count scales with difficulty and game mode
        // Harder modes start with more cones, difficulty adds even more over time
        int baseMinCones = Math.max(2, gameMode); // At least 2, harder modes get more
        int baseMaxCones = Math.max(3, gameMode + 2);

        int minCones = Math.min(baseMinCones + (difficultyLevel - 1) / 2, 10);
        int maxCones = Math.min(baseMaxCones + (difficultyLevel - 1) / 2, 12);
        int count = minCones + new Random().nextInt(maxCones - minCones + 1);

        count = Math.max(2, count); // Always at least 2 cones

        // Cone size shrinks with difficulty - creates tighter passages
        int baseConeHeight = gameMode <= 2 ? 120 : 100; // Smaller for hard modes
        int coneH = dpToPx(baseConeHeight - (difficultyLevel - 1) * 2);
        coneH = Math.max(dpToPx(60), coneH); // Don't make them too tiny

        // Gap between cones gets smaller with difficulty
        int baseGap = dpToPx(Math.max(60, 120 - (gameMode - 1) * 15));
        int difficultyReduction = (difficultyLevel - 1) * dpToPx(12);
        int minGap = dpToPx(15); // Minimum gap for playability
        int gap = Math.max(minGap, baseGap - difficultyReduction);

        // Make sure cones fit in container
        int totalConeSpace = count * coneH + (count + 1) * gap;
        if (totalConeSpace > height) {
            gap = Math.max(minGap, (height - count * coneH) / (count + 1));
        }

        // Generate challenging but fair cone positions
        List<Integer> positions = generateConePositions(height, count, coneH, gap);

        // Create and place the cone views
        for (int i = 0; i < count; i++) {
            ImageView cone = new ImageView(this);
            cone.setImageResource(R.drawable.game_cone);
            cone.setRotation(side.equals("left") ? 90 : -90); // Point inward
            cone.setScaleType(ImageView.ScaleType.FIT_XY);

            int coneWidth = dpToPx(Math.max(50, 80 - (difficultyLevel - 1)));
            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(coneWidth, coneH);
            p.gravity = side.equals("left") ? Gravity.START : Gravity.END;
            p.topMargin = positions.get(i);

            if (side.equals("right")) p.setMarginEnd(dpToPx(8));
            container.addView(cone, p);
            sideCones.add(cone);
        }

        Log.d("GameActivity", "Added " + count + " cones to " + side + " side (difficulty: " + difficultyLevel + ", game mode: " + gameMode + ")");
    }

    private List<Integer> generateConePositions(int containerHeight, int coneCount, int coneHeight, int gap) {
        List<Integer> positions = new ArrayList<>();
        Random random = new Random();

        // Game mode affects when complex patterns start appearing
        int complexityThreshold = Math.max(1, 4 - gameMode); // Hard modes get complex sooner

        if (difficultyLevel <= complexityThreshold) {
            // Early game: Simple, evenly spaced cones - learn the mechanics
            for (int i = 0; i < coneCount; i++) {
                positions.add(gap * (i + 1) + coneHeight * i);
            }
        } else if (difficultyLevel <= complexityThreshold + 3) {
            // Mid game: Some variation to keep it interesting
            generateIrregularSpacing(positions, containerHeight, coneCount, coneHeight, gap, random);
        } else if (difficultyLevel <= complexityThreshold + 8) {
            // High difficulty: Challenging patterns that require skill
            generateChallengingPatterns(positions, containerHeight, coneCount, coneHeight, gap, random);
        } else {
            // Extreme mode: Maximum difficulty - for the pros
            generateExtremePatterns(positions, containerHeight, coneCount, coneHeight, gap, random);
        }

        return positions;
    }

    private void generateIrregularSpacing(List<Integer> positions, int containerHeight, int coneCount, int coneHeight, int gap, Random random) {
        // Mix up the spacing but keep it reasonable
        int usableHeight = containerHeight - coneCount * coneHeight - gap * 2;
        int currentY = gap;

        for (int i = 0; i < coneCount; i++) {
            if (i == 0) {
                positions.add(currentY);
            } else {
                // Vary the gap size randomly
                int minSpacing = gap / 3;
                int maxSpacing = gap * 2;
                int spacing = minSpacing + random.nextInt(maxSpacing - minSpacing + 1);
                currentY += coneHeight + spacing;

                // Don't go off the bottom
                if (currentY + coneHeight > containerHeight - gap) {
                    currentY = containerHeight - gap - coneHeight;
                }
                positions.add(currentY);
            }
        }
    }

    private void generateChallengingPatterns(List<Integer> positions, int containerHeight, int coneCount, int coneHeight, int gap, Random random) {
        // Rotate through different challenging pattern types
        if (difficultyLevel % 3 == 0) {
            generateNarrowPassages(positions, containerHeight, coneCount, coneHeight, gap, random);
        } else if (difficultyLevel % 3 == 1) {
            generateEdgeClusters(positions, containerHeight, coneCount, coneHeight, gap);
        } else {
            generateZigzagPattern(positions, containerHeight, coneCount, coneHeight, gap);
        }
    }

    private void generateExtremePatterns(List<Integer> positions, int containerHeight, int coneCount, int coneHeight, int gap, Random random) {
        // Multiple narrow passages - very challenging but still playable
        int passageCount = Math.min(3, difficultyLevel - 10);
        int passageHeight = containerHeight / (passageCount + 1);

        for (int passage = 0; passage < passageCount; passage++) {
            int passageStart = passage * passageHeight + passageHeight / 3;
            int passageEnd = passageStart + passageHeight / 6; // Very narrow

            // Fill the spaces between passages with cones
            int conesForThisSection = coneCount / passageCount;
            for (int i = 0; i < conesForThisSection && positions.size() < coneCount; i++) {
                int y;
                do {
                    y = random.nextInt(passageHeight) + passage * passageHeight;
                } while (y >= passageStart && y <= passageEnd); // Avoid the passage area

                if (y + coneHeight < containerHeight - gap) {
                    positions.add(y);
                }
            }
        }

        // Fill any remaining slots randomly
        while (positions.size() < coneCount) {
            int y = random.nextInt(containerHeight - coneHeight - gap * 2) + gap;
            boolean validPosition = true;

            // Make sure new cone doesn't overlap existing ones
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
        // Create one narrow passage in the middle, fill top and bottom with cones
        int passageStart = containerHeight / 3;
        int passageEnd = 2 * containerHeight / 3;
        int passageWidth = Math.max(coneHeight, containerHeight / 8);

        // Top section
        int topCones = coneCount / 2;
        for (int i = 0; i < topCones; i++) {
            positions.add(gap + i * (coneHeight + gap / 4));
        }

        // Bottom section
        int bottomCones = coneCount - topCones;
        for (int i = 0; i < bottomCones; i++) {
            int y = passageEnd + gap + i * (coneHeight + gap / 4);
            if (y + coneHeight < containerHeight - gap) {
                positions.add(y);
            }
        }
    }

    private void generateEdgeClusters(List<Integer> positions, int containerHeight, int coneCount, int coneHeight, int gap) {
        // Cluster cones at top and bottom, leave middle more open
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
        // Alternate between top and bottom - creates a zigzag effect
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

    private boolean detectCollision() {
        // Skip collision during countdown - player isn't really playing yet
        if (isCountingDown) {
            return false;
        }

        // Check collision with side cones
        for (ImageView cone : sideCones) {
            if (pixelCollision(player, cone)) return true;
        }
        
        // Check collision with top/bottom barriers
        Rect pr = new Rect(), tr = new Rect(), br = new Rect();
        player.getHitRect(pr);
        topBarrierContainer.getHitRect(tr);
        bottomBarrierContainer.getHitRect(br);
        return Rect.intersects(pr, tr) || Rect.intersects(pr, br);
    }

    private boolean pixelCollision(ImageView a, ImageView b) {
        // Pixel-perfect collision detection - more accurate than just rectangles
        // This prevents the frustrating "I wasn't touching it!" moments
        if (!(a.getDrawable() instanceof BitmapDrawable) ||
                !(b.getDrawable() instanceof BitmapDrawable)) return false;
        Bitmap ba = ((BitmapDrawable)a.getDrawable()).getBitmap();
        Bitmap bb = ((BitmapDrawable)b.getDrawable()).getBitmap();

        Rect ra = new Rect(), rb = new Rect(), ri = new Rect();
        a.getGlobalVisibleRect(ra);
        b.getGlobalVisibleRect(rb);
        if (!ri.setIntersect(ra, rb)) return false;

        int alphaTh = 50; // Transparency threshold
        int oxA = ri.left - ra.left, oyA = ri.top - ra.top;
        int oxB = ri.left - rb.left, oyB = ri.top - rb.top;
        
        // Check each overlapping pixel for actual collision
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