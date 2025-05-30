package com.example.dunkdash;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class SelectModesActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_modes);

        // TODO: Populate modes from Firestore and handle selection

        Button btnReturn = findViewById(R.id.btn_return);
        btnReturn.setOnClickListener(v -> finish());
    }
}
