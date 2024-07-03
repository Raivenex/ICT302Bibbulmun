package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class ViewReportActivity extends AppCompatActivity {
    private ListView listViewFiles;
    private List<String> fileList;
    private Button uploadFilesBtn, viewUploadedFiles;
    private TextView tvNoFiles;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent;
                intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this,onBackPressedCallback);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_report);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        listViewFiles = findViewById(R.id.listViewFiles);
        uploadFilesBtn = findViewById(R.id.upload_files_btn);
        viewUploadedFiles = findViewById(R.id.view_uploaded_files_btn);
        tvNoFiles = findViewById(R.id.tvNoFiles);
        fileList = getFileList();
        ArrayAdapter<String> fileAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileList);
        listViewFiles.setAdapter(fileAdapter);
        updateUIBasedOnFiles();
        viewUploadedFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ViewUploadedReportActivity.class);
                startActivity(intent);
                finish();
            }
        });
        uploadFilesBtn.setOnClickListener(view -> uploadAllFiles());
        listViewFiles.setOnItemClickListener((parent, view, position, id) -> {
            openFile(fileList.get(position));
        });
        listViewFiles.setOnItemLongClickListener((parent, view, position, id) -> {
            String fileName = fileList.get(position);
            showDeleteConfirmationDialog(fileName);
            return true;
        });
    }
    private void updateUIBasedOnFiles() {
        if (fileList.isEmpty()) {
            tvNoFiles.setVisibility(View.VISIBLE);
            listViewFiles.setVisibility(View.GONE);
        } else {
            tvNoFiles.setVisibility(View.GONE);
            listViewFiles.setVisibility(View.VISIBLE);
        }
    }
    private List<String> getFileList() {
        File directory = getExternalFilesDir(null);
        File[] files = directory.listFiles();
        List<String> fileList = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".xlsx")) {
                    fileList.add(file.getName());
                }
            }
        }
        return fileList;
    }

    private void openFile(String fileName) {
        File file = new File(getExternalFilesDir(null), fileName);
        Uri uri = FileProvider.getUriForFile(this, "com.example.myapplication.fileprovider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No application found to open Excel files", Toast.LENGTH_LONG).show();
        }
    }
    private void showDeleteConfirmationDialog(String fileName) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete this report?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> deleteChosenFile(fileName))
                .setNegativeButton(android.R.string.no, null)
                .show();
    }


    //New stuff
    private void uploadFileToFirebase(String fileName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            File file = new File(getExternalFilesDir(null), fileName);
            Uri fileUri = Uri.fromFile(file);
            StorageReference storageRef = FirebaseStorage.getInstance().getReference();
            StorageReference userFileRef = storageRef.child("Reports/" + user.getUid() + "/" + fileName);

            userFileRef.putFile(fileUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // File uploaded successfully, now delete the local file
                        if (file.delete()) {
                            Log.d("Upload", "File uploaded and deleted locally: " + fileName);
                            Toast.makeText(this, "File uploaded and deleted locally: " + fileName, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e("Delete", "Failed to delete local file: " + fileName);
                            Toast.makeText(this, "Failed to delete local file: " + fileName, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Upload", "Failed to upload file: " + e.getMessage());
                        Toast.makeText(this, "Upload failed for " + fileName, Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "User not logged in, upload canceled", Toast.LENGTH_SHORT).show();
        }
    }

    // Call this method when you want to upload all files
    private void uploadAllFiles() {
        File directory = getExternalFilesDir(null);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".xlsx")) {
                    uploadFileToFirebase(file.getName());
                }
            }
        }
    }
    private void deleteChosenFile(String fileName) {
        File file = new File(getExternalFilesDir(null), fileName);
        if (file.delete()) {
            fileList.remove(fileName); // Update your list
            ((ArrayAdapter<String>) listViewFiles.getAdapter()).notifyDataSetChanged();
            updateUIBasedOnFiles();
            Toast.makeText(this, "File deleted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show();
        }
    }
}