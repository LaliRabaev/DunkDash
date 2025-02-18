package com.example.dunkdash;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private ImageView player;
    private FrameLayout leftContainer, rightContainer;
    private View topBarrierContainer, bottomBarrierContainer;
    private List<ImageView> sideCones = new ArrayList<>();

    // Game loop handler and runnable
    private Handler handler = new Handler();
    private Runnable gameRunnable;
    private final int FRAME_RATE = 16; // ~60 FPS

    // Player position and velocity (for simple physics)
    private float playerX, playerY;
    private float dx = 5f;       // horizontal velocity
    private float dy = 0f;       // vertical velocity (starts at 0)
    private final float GRAVITY = 0.5f;
    private final float JUMP_VELOCITY = -10f;

    // Flag to ensure obstacles are added once
    private boolean obstaclesAdded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        player = findViewById(R.id.player_basketball);
        leftContainer = findViewById(R.id.left_cones_container);
        rightContainer = findViewById(R.id.right_cones_container);
        topBarrierContainer = findViewById(R.id.top_barrier_container);
        bottomBarrierContainer = findViewById(R.id.bottom_barrier_container);

        // Set a click listener on the entire game area to make the ball jump (like Flappy Bird)
        View rootLayout = findViewById(R.id.rootLayout);
        rootLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dy = JUMP_VELOCITY;
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Once the views are measured, add obstacles and start the game loop
        if (hasFocus && !obstaclesAdded) {
            addSideConesRandomly();
            // Initialize player's starting position (after layout)
            playerX = player.getX();
            playerY = player.getY();
            startGameLoop();
            obstaclesAdded = true;
        }
    }

    /**
     * Adds a random number (1 to 4) of side cones for each side,
     * spacing them out so they do not overlap.
     */
    private void addSideConesRandomly() {
        Random random = new Random();

        // Clear any existing cones
        leftContainer.removeAllViews();
        rightContainer.removeAllViews();
        sideCones.clear();

        // Determine random count between 1 and 4 for each side
        int leftCount = 1 + random.nextInt(4);  // [1,4]
        int rightCount = 1 + random.nextInt(4);

        // Get container heights (in pixels)
        int leftContainerHeight = leftContainer.getHeight();
        int rightContainerHeight = rightContainer.getHeight();
        int coneHeightPx = dpToPx(120);

        // Calculate vertical gap to ensure cones do not overlap:
        int leftGap = 0;
        if (leftCount * coneHeightPx < leftContainerHeight) {
            leftGap = (leftContainerHeight - leftCount * coneHeightPx) / (leftCount + 1);
        }
        int rightGap = 0;
        if (rightCount * coneHeightPx < rightContainerHeight) {
            rightGap = (rightContainerHeight - rightCount * coneHeightPx) / (rightCount + 1);
        }

        // Add cones on the left side
        for (int i = 0; i < leftCount; i++) {
            ImageView cone = new ImageView(this);
            cone.setImageResource(R.drawable.game_cone);
            cone.setRotation(90); // face inward
            cone.setScaleType(ImageView.ScaleType.FIT_XY);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dpToPx(80), coneHeightPx);
            params.leftMargin = dpToPx(8);
            // Position: gap*(i+1) + i * coneHeight
            params.topMargin = leftGap * (i + 1) + coneHeightPx * i;
            leftContainer.addView(cone, params);
            sideCones.add(cone);
        }

        // Add cones on the right side
        for (int i = 0; i < rightCount; i++) {
            ImageView cone = new ImageView(this);
            cone.setImageResource(R.drawable.game_cone);
            cone.setRotation(-90); // face inward
            cone.setScaleType(ImageView.ScaleType.FIT_XY);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dpToPx(80), coneHeightPx);
            params.rightMargin = dpToPx(8);
            params.gravity = Gravity.END;
            params.topMargin = rightGap * (i + 1) + coneHeightPx * i;
            rightContainer.addView(cone, params);
            sideCones.add(cone);
        }
    }
    private void repositionSideCones() {
        addSideConesRandomly();
    }

    /**
     * Starts the game loop that updates the player's position and checks for collisions.
     */
    private void startGameLoop() {
        gameRunnable = new Runnable() {
            @Override
            public void run() {
                updatePlayerPosition();
                if (checkCollision()) {
                    handler.removeCallbacks(this);
                    goToFailPage();
                } else {
                    handler.postDelayed(this, FRAME_RATE);
                }
            }
        };
        handler.postDelayed(gameRunnable, FRAME_RATE);
    }

    /**
     * Updates the player's position by applying movement and gravity.
     */
    private void updatePlayerPosition() {
        // Update horizontal position
        playerX += dx;
        // Update vertical position and apply gravity
        playerY += dy;
        dy += GRAVITY;

        // Get parent dimensions (game area)
        View parent = (View) player.getParent();
        int parentWidth = parent.getWidth();
        int parentHeight = parent.getHeight();
        int playerWidth = player.getWidth();
        int playerHeight = player.getHeight();

        // Bounce off left/right edges:
        if (playerX <= 0 || playerX + playerWidth >= parentWidth) {
            dx = -dx;
            repositionSideCones();
        }

        // Clamp vertical position to avoid going off-screen (barrier collisions are checked separately)
        if (playerY < 0) {
            playerY = 0;
            dy = 0;
        }
        if (playerY + playerHeight > parentHeight) {
            playerY = parentHeight - playerHeight;
            dy = 0;
        }

        player.setX(playerX);
        player.setY(playerY);
    }

    private boolean checkCollision() {
        Rect playerRect = new Rect();
        player.getHitRect(playerRect);

        // Check collision with side cones (shrink each cone's hit rect by a margin for better accuracy)
        for (ImageView cone : sideCones) {
            Rect coneRect = new Rect();
            cone.getHitRect(coneRect);
            int margin = dpToPx(5);
            coneRect.inset(margin, margin);
            if (Rect.intersects(playerRect, coneRect)) {
                return true;
            }
        }
        // Check collision with top barrier container
        Rect topRect = new Rect();
        topBarrierContainer.getHitRect(topRect);
        if (Rect.intersects(playerRect, topRect)) {
            return true;
        }
        // Check collision with bottom barrier container
        Rect bottomRect = new Rect();
        bottomBarrierContainer.getHitRect(bottomRect);
        if (Rect.intersects(playerRect, bottomRect)) {
            return true;
        }
        return false;
    }

    /**
     * Transitions to the FailActivity if a collision occurs.
     */
    private void goToFailPage() {
        Intent intent = new Intent(GameActivity.this, FailActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(gameRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameRunnable != null) {
            handler.postDelayed(gameRunnable, FRAME_RATE);
        }
    }

    /**
     * Helper method to convert dp (density-independent pixels) to actual pixels.
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}



