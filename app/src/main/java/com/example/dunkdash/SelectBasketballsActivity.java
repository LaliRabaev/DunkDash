package com.example.dunkdash;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectBasketballsActivity extends AppCompatActivity {
    private static final String TAG = "SelectBasketballsAct";

    private GridLayout ballsGrid;
    private TextView selectedBallText;
    private Button saveButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String userId;
    private long userMaxScore;
    private int selectedBallId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_basketballs);

        ballsGrid = findViewById(R.id.balls_grid);
        selectedBallText = findViewById(R.id.selected_ball_text);
        saveButton = findViewById(R.id.save_button);
        saveButton.setEnabled(false);

        // Add return button logic
        Button returnButton = findViewById(R.id.button_return_home);
        returnButton.setOnClickListener(v -> {
            Intent intent = new Intent(SelectBasketballsActivity.this, HomePageActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        Button btnReturn = findViewById(R.id.btn_return);
        btnReturn.setOnClickListener(v -> finish());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = user.getUid();

        saveButton.setOnClickListener(v -> saveSelection());

        loadUserMaxScore();
    }

    private void loadUserMaxScore() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    userMaxScore = doc.contains("max_score") ? doc.getLong("max_score") : 0;
                    Log.d(TAG, "User max_score=" + userMaxScore);
                    loadBasketballs();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed loading max_score", e);
                    Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadBasketballs() {
        ballsGrid.removeAllViews();
        db.collection("basketballs").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            List<QueryDocumentSnapshot> docs = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                docs.add(doc);
                            }
                            Collections.sort(docs, Comparator.comparingLong(d -> d.contains("min_score") ? d.getLong("min_score") : Long.MAX_VALUE));

                            for (QueryDocumentSnapshot doc : docs) {
                                int id = doc.getLong("id").intValue();
                                String name = doc.getString("name");
                                String path = doc.getString("image_path");
                                long minScore = doc.contains("min_score") ? doc.getLong("min_score") : Long.MAX_VALUE;
                                boolean unlocked = userMaxScore >= minScore;

                                String resName = path.replaceFirst("^drawable/", "");
                                int dot = resName.lastIndexOf('.');
                                if (dot > 0) resName = resName.substring(0, dot);
                                int resId = getResources().getIdentifier(resName, "drawable", getPackageName());
                                if (resId == 0) {
                                    Log.w(TAG, "Drawable not found: " + path);
                                    continue;
                                }

                                FrameLayout container = new FrameLayout(SelectBasketballsActivity.this);
                                GridLayout.LayoutParams glParams = new GridLayout.LayoutParams();
                                glParams.width = GridLayout.LayoutParams.WRAP_CONTENT;
                                glParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
                                glParams.setGravity(Gravity.CENTER);
                                glParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                                container.setLayoutParams(glParams);

                                ImageView iv = new ImageView(SelectBasketballsActivity.this);
                                int size = dpToPx(90);
                                FrameLayout.LayoutParams ivParams = new FrameLayout.LayoutParams(size, size);
                                ivParams.gravity = Gravity.CENTER;
                                iv.setLayoutParams(ivParams);
                                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                iv.setImageResource(resId);

                                if (unlocked) {
                                    iv.setAlpha(1f);
                                    iv.setOnClickListener(v -> {
                                        selectedBallId = id;
                                        selectedBallText.setText("Selected: " + name);
                                        saveButton.setEnabled(true);
                                        highlightSelection(container);
                                    });
                                } else {
                                    iv.setAlpha(0.3f);
                                    iv.setOnClickListener(v -> Toast.makeText(SelectBasketballsActivity.this,
                                            "Locked until score: " + minScore,
                                            Toast.LENGTH_SHORT).show());
                                }

                                container.addView(iv);
                                ballsGrid.addView(container);
                            }
                        } else {
                            Toast.makeText(SelectBasketballsActivity.this,
                                    "Failed to load options", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(SelectBasketballsActivity.this,
                        "Error fetching balls: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void highlightSelection(View selected) {
        for (int i = 0; i < ballsGrid.getChildCount(); i++) {
            View v = ballsGrid.getChildAt(i);
            if (v == selected) {
                GradientDrawable border = new GradientDrawable();
                border.setShape(GradientDrawable.RECTANGLE);
                border.setStroke(dpToPx(4), Color.parseColor("#F57C00"));
                border.setCornerRadius(dpToPx(8));
                v.setBackground(border);
            } else {
                v.setBackground(null);
            }
        }
    }

    private void saveSelection() {
        if (selectedBallId < 0) return;
        Map<String, Object> update = new HashMap<>();
        update.put("current_basketball", selectedBallId);
        db.collection("users").document(userId)
                .update(update)
                .addOnSuccessListener(aVoid -> Toast.makeText(this,
                        "Selection saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Error saving selection: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private int dpToPx(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }
}