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
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private ImageView player;
    private FrameLayout leftContainer, rightContainer;
    private View topBarrierContainer, bottomBarrierContainer;
    // Combined list of all side cones (from both left and right)
    private List<ImageView> sideCones = new ArrayList<>();

    // Game loop handler and runnable
    private Handler handler = new Handler();
    private Runnable gameRunnable;
    private final int FRAME_RATE = 16; // ~60 FPS

    // Player position and velocity
    private float playerX, playerY;
    // Increase horizontal speed (was 5f, now 8f)
    private float dx = 8f;
    private float dy = 0f;
    private final float GRAVITY = 0.5f;
    private final float JUMP_VELOCITY = -10f;

    // Flags to track which side's cones are active
    private boolean leftConesActive = true;
    private boolean rightConesActive = true;
    // To add obstacles only once when views are measured
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

        // Tap anywhere to make the ball jump
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
        // Once views are measured, add obstacles and start the game loop.
        if (hasFocus && !obstaclesAdded) {
            // Initially add cones on both sides.
            addConesForSide("left");
            addConesForSide("right");
            obstaclesAdded = true;
            // Initialize the player's starting position.
            playerX = player.getX();
            playerY = player.getY();
            startGameLoop();
        }
    }

    /**
     * Adds a random number (1 to 4) of cones to the specified side,
     * spaced evenly so they do not overlap.
     *
     * @param side "left" or "right"
     */
    private void addConesForSide(String side) {
        Random random = new Random();
        int count = 1 + random.nextInt(4); // random count from 1 to 4
        int coneHeightPx = dpToPx(120);
        int containerHeight;
        FrameLayout container;
        if (side.equals("left")) {
            container = leftContainer;
            containerHeight = leftContainer.getHeight();
        } else {
            container = rightContainer;
            containerHeight = rightContainer.getHeight();
        }
        int gap = 0;
        if (count * coneHeightPx < containerHeight) {
            gap = (containerHeight - count * coneHeightPx) / (count + 1);
        }
        for (int i = 0; i < count; i++) {
            ImageView cone = new ImageView(this);
            cone.setImageResource(R.drawable.game_cone);
            // Rotate cones so they face inward.
            if (side.equals("left")) {
                cone.setRotation(90);
            } else {
                cone.setRotation(-90);
            }
            cone.setScaleType(ImageView.ScaleType.FIT_XY);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dpToPx(80), coneHeightPx);
            if (side.equals("left")) {
                params.leftMargin = dpToPx(8);
            } else {
                params.rightMargin = dpToPx(8);
                params.gravity = Gravity.END;
            }
            params.topMargin = gap * (i + 1) + coneHeightPx * i;
            container.addView(cone, params);
            sideCones.add(cone);
        }
        // Mark the side as active.
        if (side.equals("left")) {
            leftConesActive = true;
        } else {
            rightConesActive = true;
        }
    }

    /**
     * Removes all cones from the specified side.
     *
     * @param side "left" or "right"
     */
    private void removeConesForSide(String side) {
        if (side.equals("left")) {
            leftContainer.removeAllViews();
            leftConesActive = false;
            // Remove from the combined list.
            for (int i = sideCones.size() - 1; i >= 0; i--) {
                if (sideCones.get(i).getParent() == leftContainer) {
                    sideCones.remove(i);
                }
            }
        } else if (side.equals("right")) {
            rightContainer.removeAllViews();
            rightConesActive = false;
            for (int i = sideCones.size() - 1; i >= 0; i--) {
                if (sideCones.get(i).getParent() == rightContainer) {
                    sideCones.remove(i);
                }
            }
        }
    }

    /**
     * Starts the game loop.
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
     * Updates the player's position, applies gravity, and handles bounces.
     */
    private void updatePlayerPosition() {
        playerX += dx;
        playerY += dy;
        dy += GRAVITY;

        View parent = (View) player.getParent();
        int parentWidth = parent.getWidth();
        int parentHeight = parent.getHeight();
        int playerWidth = player.getWidth();
        int playerHeight = player.getHeight();

        // Horizontal bounce logic.
        if (playerX <= 0) { // Ball touches left edge.
            dx = -dx;
            // Remove left-side cones if present.
            if (leftConesActive) {
                removeConesForSide("left");
            }
            // Ensure right-side cones are present.
            if (!rightConesActive) {
                addConesForSide("right");
            }
        } else if (playerX + playerWidth >= parentWidth) { // Ball touches right edge.
            dx = -dx;
            // Remove right-side cones if present.
            if (rightConesActive) {
                removeConesForSide("right");
            }
            // Ensure left-side cones are present.
            if (!leftConesActive) {
                addConesForSide("left");
            }
        }

        // Clamp vertical position.
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

    /**
     * Checks for collisions between the ball and obstacles.
     * Uses pixel-perfect collision for side cones and rectangular collision for barriers.
     */
    private boolean checkCollision() {
        // Check side cones.
        for (ImageView cone : sideCones) {
            if (pixelCollision(player, cone)) {
                return true;
            }
        }
        // Check top barrier.
        Rect playerRect = new Rect();
        player.getHitRect(playerRect);
        Rect topRect = new Rect();
        topBarrierContainer.getHitRect(topRect);
        if (Rect.intersects(playerRect, topRect)) {
            return true;
        }
        // Check bottom barrier.
        Rect bottomRect = new Rect();
        bottomBarrierContainer.getHitRect(bottomRect);
        if (Rect.intersects(playerRect, bottomRect)) {
            return true;
        }
        return false;
    }

    /**
     * Performs pixel-perfect collision detection between the ball and a cone.
     */
    private boolean pixelCollision(ImageView ballView, ImageView coneView) {
        if (!(ballView.getDrawable() instanceof BitmapDrawable) ||
                !(coneView.getDrawable() instanceof BitmapDrawable)) {
            return false;
        }
        Bitmap ballBitmap = ((BitmapDrawable) ballView.getDrawable()).getBitmap();
        Bitmap coneBitmap = ((BitmapDrawable) coneView.getDrawable()).getBitmap();

        // Get global visible rectangles.
        Rect ballRect = new Rect();
        ballView.getGlobalVisibleRect(ballRect);
        Rect coneRect = new Rect();
        coneView.getGlobalVisibleRect(coneRect);

        // Compute intersection.
        Rect intersectRect = new Rect();
        if (!intersectRect.setIntersect(ballRect, coneRect)) {
            return false;
        }

        // Compute offsets.
        int ballOffsetX = intersectRect.left - ballRect.left;
        int ballOffsetY = intersectRect.top - ballRect.top;
        int coneOffsetX = intersectRect.left - coneRect.left;
        int coneOffsetY = intersectRect.top - coneRect.top;

        int alphaThreshold = 50; // tweak as needed

        for (int y = 0; y < intersectRect.height(); y++) {
            for (int x = 0; x < intersectRect.width(); x++) {
                if (ballOffsetX + x >= ballBitmap.getWidth() || ballOffsetY + y >= ballBitmap.getHeight() ||
                        coneOffsetX + x >= coneBitmap.getWidth() || coneOffsetY + y >= coneBitmap.getHeight()) {
                    continue;
                }
                int ballPixel = ballBitmap.getPixel(ballOffsetX + x, ballOffsetY + y);
                int conePixel = coneBitmap.getPixel(coneOffsetX + x, coneOffsetY + y);
                int ballAlpha = (ballPixel >> 24) & 0xff;
                int coneAlpha = (conePixel >> 24) & 0xff;
                if (ballAlpha > alphaThreshold && coneAlpha > alphaThreshold) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Transitions to the FailActivity.
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
     * Converts dp to pixels.
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
