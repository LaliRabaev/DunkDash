package com.example.dunkdash;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;

public class LogoutDialog extends Dialog {
    
    private final LogoutDialogListener listener;
    
    public interface LogoutDialogListener {
        void onLogoutConfirmed();
        void onLogoutCancelled();
    }
    
    public LogoutDialog(Context context, LogoutDialogListener listener) {
        super(context);
        this.listener = listener;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_logout);
        
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        
        Button cancelButton = findViewById(R.id.cancelButton);
        Button logoutButton = findViewById(R.id.logoutButton);
        
        cancelButton.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onLogoutCancelled();
            }
        });
        
        logoutButton.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onLogoutConfirmed();
            }
        });
        
        // Handle back button and outside touch
        setOnCancelListener(dialog -> {
            if (listener != null) {
                listener.onLogoutCancelled();
            }
        });
    }
}
