
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    // UI & containers
    private ImageView player;
    private FrameLayout leftContainer, rightContainer;
    private View topBarrier, bottomBarrier;
    private View rootLayout;

    // Firestore & Auth
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String userId;

    // Game loop
    private Handler handler = new Handler();
    private Runnable gameLoop;
    private static final int FRAME_RATE_MS = 16; // ~60fps

    // Physics
    private float playerX, playerY;
    private float dx = 8f;
    private float dy = 0f;
    private static final float GRAVITY = 0.5f;
    private static final float JUMP_VELOCITY = -10f;

    // Obstacles
    private List<ImageView> sideCones = new ArrayList<>();
    private boolean leftActive = true, rightActive = true;
    private boolean prepared = false;

    // Metrics
    private long startTime;
    private int tapCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Init UI
        player      = findViewById(R.id.player_basketball);
        leftContainer  = findViewById(R.id.left_cones_container);
        rightContainer = findViewById(R.id.right_cones_container);
        topBarrier     = findViewById(R.id.top_barrier_container);
        bottomBarrier  = findViewById(R.id.bottom_barrier_container);
        rootLayout     = findViewById(R.id.rootLayout);

        // Init Firebase
        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        }

        // Jump listener & tap counter
        rootLayout.setOnClickListener(v -> {
            dy = JUMP_VELOCITY;
            tapCount++;
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !prepared) {
            // Set initial metrics
            startTime = System.currentTimeMillis();
            tapCount   = 0;

            // Populate obstacles
            addCones("left");
            addCones("right");
            prepared = true;

            // Record start position
            playerX = player.getX();
            playerY = player.getY();

            // Start loop
            startGameLoop();
        }
    }

    private void startGameLoop() {
        gameLoop = () -> {
            updateMovement();
            if (detectCollision()) {
                handler.removeCallbacks(gameLoop);
                saveGameAndFinish();
            } else {
                handler.postDelayed(gameLoop, FRAME_RATE_MS);
            }
        };
        handler.postDelayed(gameLoop, FRAME_RATE_MS);
    }

    private void updateMovement() {
        // Move
        playerX += dx;
        playerY += dy;
        dy += GRAVITY;
        // Gradually speed up
        dx *= 1.0005f;

        // Bounds & bounce
        int pw = player.getWidth(), ph = player.getHeight();
        int w  = rootLayout.getWidth(), h  = rootLayout.getHeight();

        if (playerX <= 0) {
            dx = Math.abs(dx);
            swapSide(false, true);
        } else if (playerX + pw >= w) {
            dx = -Math.abs(dx);
            swapSide(true, false);
        }
        // Vertical clamp
        if (playerY < 0) { playerY = 0; dy = 0; }
        if (playerY + ph > h) { playerY = h - ph; dy = 0; }

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
        FrameLayout container = (side.equals("left") ? leftContainer : rightContainer);
        int height = container.getHeight();
        int count  = 1 + new Random().nextInt(4);
        int coneH  = dpToPx(120);
        int gap    = (height - count*coneH) / (count+1);

        for (int i=0; i<count; i++) {
            ImageView c = new ImageView(this);
            c.setImageResource(R.drawable.game_cone);
            c.setRotation(side.equals("left") ?  90 : -90);
            c.setScaleType(ImageView.ScaleType.FIT_XY);
            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dpToPx(80), coneH);
            p.gravity = side.equals("left") ? Gravity.START : Gravity.END;
            p.topMargin = gap*(i+1) + coneH*i;
            if (side.equals("right")) p.setMarginEnd(dpToPx(8));
            container.addView(c, p);
            sideCones.add(c);
        }
    }

    private void removeCones(FrameLayout container) {
        container.removeAllViews();
        // cleanup list
        sideCones.removeIf(view -> view.getParent() == container);
    }

    private boolean detectCollision() {
        // Pixel-level for cones
        for (ImageView cone: sideCones) {
            if (pixelCollision(player, cone)) return true;
        }
        // Rect for barriers
        Rect pr = new Rect(), tr = new Rect(), br = new Rect();
        player.getHitRect(pr);
        topBarrier.getHitRect(tr);
        bottomBarrier.getHitRect(br);
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
        for (int y=0; y<ri.height(); y++) {
            for (int x=0; x<ri.width(); x++) {
                int pa = ba.getPixel(oxA+x, oyA+y);
                int pb = bb.getPixel(oxB+x, oyB+y);
                if (((pa>>24)&0xff) > alphaTh && ((pb>>24)&0xff)>alphaTh) return true;
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
        double dur = (now - startTime)/1000.0;

        DocumentReference gameRef = db.collection("games").document();
        String gameId = gameRef.getId();

        Map<String, Object> doc = new HashMap<>();
        doc.put("id",         gameId);
        doc.put("user_id",    userId);
        doc.put("start_date", new Timestamp(new Date(startTime)));
        doc.put("duration",   dur);
        doc.put("game_mode",  1);
        doc.put("score",      tapCount);

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

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(gameLoop);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameLoop != null) handler.postDelayed(gameLoop, FRAME_RATE_MS);
    }

    private int dpToPx(int dp) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(dp * d);
    }
}
