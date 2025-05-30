package com.example.dunkdash;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectModes extends AppCompatActivity {
    private GridLayout modesGrid;
    private TextView selectedModeText;
    private Button saveButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String userId;
    private String selectedModeId = null;
    private String selectedModeName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_modes);

        modesGrid = findViewById(R.id.modes_grid);
        selectedModeText = findViewById(R.id.selected_mode_text);
        saveButton = findViewById(R.id.save_button);
        saveButton.setEnabled(false);

        // Return button logic
        Button returnButton = findViewById(R.id.button_return_home);
        returnButton.setOnClickListener(v -> {
            Intent intent = new Intent(SelectModes.this, HomePageActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

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

        loadModes();
    }

    private void loadModes() {
        modesGrid.removeAllViews();
        db.collection("modes")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<QueryDocumentSnapshot> docs = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        docs.add(doc);
                    }
                    Collections.sort(docs, Comparator.comparing(d -> d.getString("name")));

                    for (QueryDocumentSnapshot doc : docs) {
                        final String modeId = doc.getId();
                        final String modeName = doc.getString("name");
                        String iconPath = doc.getString("icon_path"); // optional
                        int resId = R.drawable.splash_screen;
                        if (iconPath != null) {
                            String resName = iconPath.replaceFirst("^drawable/", "");
                            int dot = resName.lastIndexOf('.');
                            if (dot > 0) resName = resName.substring(0, dot);
                            int foundResId = getResources().getIdentifier(resName, "drawable", getPackageName());
                            if (foundResId != 0) resId = foundResId;
                        }

                        FrameLayout container = new FrameLayout(SelectModes.this);
                        GridLayout.LayoutParams glParams = new GridLayout.LayoutParams();
                        glParams.width = GridLayout.LayoutParams.WRAP_CONTENT;
                        glParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
                        glParams.setGravity(Gravity.CENTER);
                        container.setLayoutParams(glParams);

                        ImageView iv = new ImageView(SelectModes.this);
                        int size = dpToPx(90);
                        FrameLayout.LayoutParams ivParams = new FrameLayout.LayoutParams(size, size);
                        ivParams.gravity = Gravity.CENTER;
                        iv.setLayoutParams(ivParams);
                        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        iv.setImageResource(resId);

                        container.setOnClickListener(v -> {
                            selectedModeId = modeId;
                            selectedModeName = modeName;
                            selectedModeText.setText("Selected: " + modeName);
                            saveButton.setEnabled(true);
                            highlightSelection(container);
                        });

                        container.addView(iv);

                        // Add label below icon
                        TextView label = new TextView(SelectModes.this);
                        label.setText(modeName);
                        label.setTextColor(Color.BLACK);
                        label.setTextSize(16f);
                        label.setGravity(Gravity.CENTER);
                        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.WRAP_CONTENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT
                        );
                        labelParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                        labelParams.bottomMargin = dpToPx(4);
                        label.setLayoutParams(labelParams);
                        container.addView(label);

                        modesGrid.addView(container);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this, "Error fetching modes: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void highlightSelection(FrameLayout selected) {
        for (int i = 0; i < modesGrid.getChildCount(); i++) {
            FrameLayout v = (FrameLayout) modesGrid.getChildAt(i);
            if (v == selected) {
                GradientDrawable border = new GradientDrawable();
                border.setColor(Color.TRANSPARENT);
                border.setStroke(dpToPx(4), Color.parseColor("#3F51B5"));
                border.setCornerRadius(dpToPx(8));
                v.setBackground(border);
            } else {
                v.setBackground(null);
            }
        }
    }

    private void saveSelection() {
        if (selectedModeId == null) return;
        Map<String, Object> update = new HashMap<>();
        update.put("current_mode", selectedModeId);
        db.collection("users").document(userId)
                .update(update)
                .addOnSuccessListener(aVoid -> Toast.makeText(
                        this, "Mode saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(
                        this, "Error saving mode: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private int dpToPx(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }
}
