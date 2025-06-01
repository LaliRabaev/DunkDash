package com.example.dunkdash;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;

public class ResetProgressDialog extends Dialog {
    
    private final ResetProgressDialogListener listener;
    
    public interface ResetProgressDialogListener {
        void onResetConfirmed();
        void onResetCancelled();
    }
    
    public ResetProgressDialog(Context context, ResetProgressDialogListener listener) {
        super(context);
        this.listener = listener;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_reset_progress);
        
        // Make dialog background transparent
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        
        Button cancelButton = findViewById(R.id.cancelButton);
        Button resetButton = findViewById(R.id.resetButton);
        
        cancelButton.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onResetCancelled();
            }
        });
        
        resetButton.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onResetConfirmed();
            }
        });
        
        // Handle back button and outside touch
        setOnCancelListener(dialog -> {
            if (listener != null) {
                listener.onResetCancelled();
            }
        });
    }
}
