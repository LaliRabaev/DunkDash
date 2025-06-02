package com.example.dunkdash;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;

public class ImagePickerDialog extends Dialog {
    
    private final ImagePickerDialogListener listener;
    
    public interface ImagePickerDialogListener {
        void onCameraSelected();
        void onGallerySelected();
        void onCancelled();
    }
    
    public ImagePickerDialog(Context context, ImagePickerDialogListener listener) {
        super(context);
        this.listener = listener;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_image_picker);
        
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        
        Button cameraButton = findViewById(R.id.cameraButton);
        Button galleryButton = findViewById(R.id.galleryButton);
        Button cancelButton = findViewById(R.id.cancelButton);
        
        cameraButton.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onCameraSelected();
            }
        });
        
        galleryButton.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onGallerySelected();
            }
        });
        
        cancelButton.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onCancelled();
            }
        });
        
        // Handle back button and outside touch
        setOnCancelListener(dialog -> {
            if (listener != null) {
                listener.onCancelled();
            }
        });
    }
}
