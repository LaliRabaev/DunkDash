package com.example.dunkdash;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;

public class LogoutDialog extends Dialog {
    
    private final LogoutDialogListener listener;
    
    // Simple callback interface - lets the calling activity know what the user chose
    public interface LogoutDialogListener {
        void onLogoutConfirmed(); // User wants to logout
        void onLogoutCancelled(); // User changed their mind
    }
    
    public LogoutDialog(Context context, LogoutDialogListener listener) {
        super(context);
        this.listener = listener;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // No title bar for cleaner look
        setContentView(R.layout.dialog_logout);
        
        // Allow user to dismiss by tapping outside or back button
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        
        Button cancelButton = findViewById(R.id.cancelButton);
        Button logoutButton = findViewById(R.id.logoutButton);
        
        // Cancel button - just close the dialog, no logout
        cancelButton.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onLogoutCancelled();
            }
        });
        
        // Logout button - user confirmed they want to logout
        logoutButton.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onLogoutConfirmed();
            }
        });
        
        // Handle when user presses back or taps outside - treat as cancel
        setOnCancelListener(dialog -> {
            if (listener != null) {
                listener.onLogoutCancelled();
            }
        });
    }
}