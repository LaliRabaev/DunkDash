package com.example.dunkdash;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity implements LogoutDialog.LogoutDialogListener, ResetProgressDialog.ResetProgressDialogListener, ImagePickerDialog.ImagePickerDialogListener {
    private static final String TAG = "SettingsActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private ImageButton backButton;
    private EditText nicknameInput;
    private Button saveNicknameButton, resetProgressButton, logoutButton, changePictureButton;
    private TextView currentEmailText, statsText;
    private SwitchCompat backgroundMusicSwitch, soundEffectsSwitch;
    private CircleImageView profileImageView;
    private ProgressBar profileUploadProgress;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private SharedPreferences prefs;
    private String userId;
    private File tempImageFile;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initializeViews();
        initializeFirebase();
        initializeActivityLaunchers();
        loadSettings();
        loadUserData();
        setupClickListeners();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        nicknameInput = findViewById(R.id.nicknameInput);
        saveNicknameButton = findViewById(R.id.saveNicknameButton);
        resetProgressButton = findViewById(R.id.resetProgressButton);
        logoutButton = findViewById(R.id.logoutButton);
        changePictureButton = findViewById(R.id.changePictureButton);
        currentEmailText = findViewById(R.id.currentEmailText);
        statsText = findViewById(R.id.statsText);
        profileImageView = findViewById(R.id.profileImageView);

        backgroundMusicSwitch = findViewById(R.id.backgroundMusicSwitch);
        soundEffectsSwitch = findViewById(R.id.soundEffectsSwitch);
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences("DunkDashSettings", MODE_PRIVATE);

        // Initialize Firebase Storage with better error handling
        try {
            storage = FirebaseStorage.getInstance();
            
            // Check if storage bucket is configured
            String bucketUrl = storage.getApp().getOptions().getStorageBucket();
            if (bucketUrl == null || bucketUrl.isEmpty()) {
                Log.e(TAG, "Firebase Storage bucket not configured");
                Toast.makeText(this, "Profile pictures unavailable - Storage not configured", Toast.LENGTH_LONG).show();
                disableProfilePictureFeature();
                return;
            }
            
            storageRef = storage.getReference();
            Log.d(TAG, "Firebase Storage initialized successfully with bucket: " + bucketUrl);
            
            // Test storage connectivity
            testStorageConnection();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase Storage", e);
            Toast.makeText(this, "Profile pictures unavailable - Storage error", Toast.LENGTH_LONG).show();
            disableProfilePictureFeature();
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        }
    }

    private void testStorageConnection() {
        // Try to list files in root to test connectivity
        storageRef.listAll()
                .addOnSuccessListener(listResult -> {
                    Log.d(TAG, "Storage connectivity test successful");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Storage connectivity test failed", e);
                    if (e.getMessage() != null && e.getMessage().contains("storage bucket")) {
                        Toast.makeText(this, "❌ Firebase Storage not properly configured", Toast.LENGTH_LONG).show();
                        disableProfilePictureFeature();
                    }
                });
    }

    private void disableProfilePictureFeature() {
        if (changePictureButton != null) {
            changePictureButton.setEnabled(false);
            changePictureButton.setText("📸 Not Available");
            changePictureButton.setAlpha(0.5f);
        }
        storage = null;
        storageRef = null;
    }

    private void initializeActivityLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            uploadImageToFirebase(imageUri);
                        }
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (tempImageFile != null && tempImageFile.exists()) {
                            Uri imageUri = Uri.fromFile(tempImageFile);
                            uploadImageToFirebase(imageUri);
                        }
                    }
                }
        );
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        saveNicknameButton.setOnClickListener(v -> saveNickname());

        resetProgressButton.setOnClickListener(v -> showResetDialog());

        logoutButton.setOnClickListener(v -> showLogoutDialog());

        changePictureButton.setOnClickListener(v -> showImagePickerDialog());

        backgroundMusicSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("background_music", isChecked);
            if (isChecked) {
                Toast.makeText(this, "🎵 Background music enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "🔇 Background music disabled", Toast.LENGTH_SHORT).show();
            }
        });

        soundEffectsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("sound_effects", isChecked);
            if (isChecked) {
                Toast.makeText(this, "🔉 Sound effects enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "🔇 Sound effects disabled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showImagePickerDialog() {
        if (storage == null || storageRef == null) {
            Toast.makeText(this, "❌ Profile pictures not available.\n\nFirebase Storage needs to be configured in your Firebase project.", Toast.LENGTH_LONG).show();
            return;
        }
        ImagePickerDialog dialog = new ImagePickerDialog(this, this);
        dialog.show();
    }

    @Override
    public void onCameraSelected() {
        if (checkCameraPermission()) {
            openCamera();
        } else {
            requestCameraPermission();
        }
    }

    @Override
    public void onGallerySelected() {
        openGallery();
    }

    @Override
    public void onCancelled() {
        Log.d(TAG, "Image picker cancelled");
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        try {
            tempImageFile = File.createTempFile("profile_", ".jpg", getCacheDir());
            Uri imageUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", tempImageFile);

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            cameraLauncher.launch(cameraIntent);
        } catch (IOException e) {
            Log.e(TAG, "Error creating temp file for camera", e);
            Toast.makeText(this, "Error opening camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (userId == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (storage == null || storageRef == null) {
            Toast.makeText(this, "❌ Storage not available. Please check Firebase configuration.", Toast.LENGTH_LONG).show();
            return;
        }

        changePictureButton.setEnabled(false);
        changePictureButton.setText("📤 Uploading...");

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            Bitmap resizedBitmap = resizeBitmap(bitmap, 300, 300);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] imageData = baos.toByteArray();

            // Create a unique filename with timestamp
            String fileName = "profile_" + userId + "_" + System.currentTimeMillis() + ".jpg";
            
            // Try different storage paths to see which works
            StorageReference profileImageRef = storageRef.child("profile_pictures/" + fileName);

            Log.d(TAG, "Attempting upload to: " + profileImageRef.getPath());
            Log.d(TAG, "Storage bucket URL: " + storage.getApp().getOptions().getStorageBucket());
            Log.d(TAG, "Image data size: " + imageData.length + " bytes");

            profileImageRef.putBytes(imageData)
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d(TAG, "Image uploaded successfully to: " + taskSnapshot.getMetadata().getPath());
                        
                        profileImageRef.getDownloadUrl()
                                .addOnSuccessListener(downloadUri -> {
                                    Log.d(TAG, "Download URL obtained: " + downloadUri.toString());
                                    updateProfilePictureUrl(downloadUri.toString());
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to get download URL", e);
                                    Toast.makeText(this, "❌ Upload succeeded but failed to get URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    resetUploadButton();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to upload image", e);
                        handleUploadError(e);
                        resetUploadButton();
                    })
                    .addOnProgressListener(taskSnapshot -> {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        Log.d(TAG, "Upload progress: " + (int)progress + "%");
                        
                        // Update button with progress
                        changePictureButton.setText("📤 " + (int)progress + "%");
                    });

        } catch (IOException e) {
            Log.e(TAG, "Error processing image", e);
            Toast.makeText(this, "❌ Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            resetUploadButton();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during upload", e);
            Toast.makeText(this, "❌ Unexpected error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            resetUploadButton();
        }
    }

    private void handleUploadError(Exception e) {
        String errorMessage = e.getMessage();
        Log.e(TAG, "Upload error details: " + errorMessage);
        
        if (errorMessage != null) {
            if (errorMessage.contains("storage bucket") || errorMessage.contains("Not Found")) {
                Toast.makeText(this, "❌ Firebase Storage not configured.\n\nPlease enable Storage in your Firebase Console:\n1. Go to Firebase Console\n2. Click Storage\n3. Click 'Get started'\n4. Set up storage rules", Toast.LENGTH_LONG).show();
                disableProfilePictureFeature();
            } else if (errorMessage.contains("permission") || errorMessage.contains("unauthorized")) {
                Toast.makeText(this, "❌ Permission denied.\n\nCheck Firebase Storage security rules.", Toast.LENGTH_LONG).show();
            } else if (errorMessage.contains("network") || errorMessage.contains("timeout")) {
                Toast.makeText(this, "❌ Network error. Please check your internet connection.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "❌ Upload failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "❌ Upload failed with unknown error", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float ratioBitmap = (float) width / (float) height;
        float ratioMax = (float) maxWidth / (float) maxHeight;

        int finalWidth = maxWidth;
        int finalHeight = maxHeight;

        if (ratioMax > ratioBitmap) {
            finalWidth = (int) ((float) maxHeight * ratioBitmap);
        } else {
            finalHeight = (int) ((float) maxWidth / ratioBitmap);
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true);
    }

    private void updateProfilePictureUrl(String imageUrl) {
        Map<String, Object> update = new HashMap<>();
        update.put("profile_picture_url", imageUrl);

        db.collection("users").document(userId)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Profile picture updated!", Toast.LENGTH_SHORT).show();

                    Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_default_profile)
                            .error(R.drawable.ic_default_profile)
                            .into(profileImageView);

                    resetUploadButton();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update profile picture URL", e);
                    Toast.makeText(this, "❌ Failed to save profile picture", Toast.LENGTH_SHORT).show();
                    resetUploadButton();
                });
    }

    private void resetUploadButton() {
        changePictureButton.setEnabled(true);
        changePictureButton.setText("📸 Change Picture");
    }

    private void loadSettings() {
        backgroundMusicSwitch.setChecked(prefs.getBoolean("background_music", true));
        soundEffectsSwitch.setChecked(prefs.getBoolean("sound_effects", true));
    }

    private void loadUserData() {
        if (userId == null) {
            currentEmailText.setText("📧 Email: Not logged in");
            statsText.setText("⚠️ Please log in to view statistics");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            currentEmailText.setText("📧 Email: " + (email != null ? email : "Unknown"));
        }

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String nickname = doc.getString("nickname");
                        if (nickname != null && !nickname.trim().isEmpty()) {
                            nicknameInput.setHint("Current: " + nickname);
                        }

                        String profilePictureUrl = doc.getString("profile_picture_url");
                        if (profilePictureUrl != null && !profilePictureUrl.trim().isEmpty()) {
                            Glide.with(this)
                                    .load(profilePictureUrl)
                                    .placeholder(R.drawable.ic_default_profile)
                                    .error(R.drawable.ic_default_profile)
                                    .into(profileImageView);
                        }

                        long totalGames = doc.contains("total_games") ? doc.getLong("total_games") : 0;
                        long maxScore = doc.contains("max_score") ? doc.getLong("max_score") : 0;
                        int currentBackground = doc.contains("current_background") ? doc.getLong("current_background").intValue() : 1;
                        int currentBasketball = doc.contains("current_basketball") ? doc.getLong("current_basketball").intValue() : 1;

                        String stats = "🎮 Games Played: " + totalGames + "\n" +
                                "🏆 Best Score: " + maxScore + "\n" +
                                "🏞️ Current Background: #" + currentBackground + "\n" +
                                "🏀 Current Basketball: #" + currentBasketball;

                        statsText.setText(stats);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user data", e);
                    statsText.setText("❌ Failed to load statistics");
                });
    }

    private void saveNickname() {
        String newNickname = nicknameInput.getText().toString().trim();

        if (newNickname.isEmpty()) {
            Toast.makeText(this, "Please enter a nickname", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newNickname.length() > 20) {
            Toast.makeText(this, "Nickname must be 20 characters or less", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userId == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        saveNicknameButton.setEnabled(false);
        saveNicknameButton.setText("💾 Saving...");

        Map<String, Object> update = new HashMap<>();
        update.put("nickname", newNickname);

        db.collection("users").document(userId)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Nickname updated to: " + newNickname, Toast.LENGTH_SHORT).show();
                    nicknameInput.setText("");
                    nicknameInput.setHint("Current: " + newNickname);
                    saveNicknameButton.setEnabled(true);
                    saveNicknameButton.setText("💾 Save");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update nickname", e);
                    Toast.makeText(this, "❌ Failed to update nickname", Toast.LENGTH_SHORT).show();
                    saveNicknameButton.setEnabled(true);
                    saveNicknameButton.setText("💾 Save");
                });
    }

    private void saveSetting(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    private void showResetDialog() {
        ResetProgressDialog dialog = new ResetProgressDialog(this, this);
        dialog.show();
    }

    private void showLogoutDialog() {
        LogoutDialog dialog = new LogoutDialog(this, this);
        dialog.show();
    }

    @Override
    public void onLogoutConfirmed() {
        performLogout();
    }

    @Override
    public void onLogoutCancelled() {
        Log.d(TAG, "Logout cancelled by user");
    }

    @Override
    public void onResetConfirmed() {
        performReset();
    }

    @Override
    public void onResetCancelled() {
        Log.d(TAG, "Reset cancelled by user");
    }

    private void performReset() {
        if (userId == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> reset = new HashMap<>();
        reset.put("total_games", 0);
        reset.put("max_score", 0);
        reset.put("current_background", 1);
        reset.put("current_basketball", 1);

        db.collection("users").document(userId)
                .update(reset)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Progress reset successfully", Toast.LENGTH_SHORT).show();
                    loadUserData();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to reset progress", e);
                    Toast.makeText(this, "❌ Failed to reset progress", Toast.LENGTH_SHORT).show();
                });
    }

    private void performLogout() {
        prefs.edit().clear().apply();

        mAuth.signOut();

        Toast.makeText(this, "👋 Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
