package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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

//TODO make an upload page to see all the files you have uploaded

public class ViewReportActivity extends AppCompatActivity {
    private ListView listViewFiles;
    private List<String> fileList;
    private FloatingActionButton fabDeleteAll;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_report);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        listViewFiles = findViewById(R.id.listViewFiles);

        fileList = getFileList();
        ArrayAdapter<String> fileAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileList);
        listViewFiles.setAdapter(fileAdapter);
        fabDeleteAll = findViewById(R.id.delete_All_btn);
        fabDeleteAll.setOnClickListener(view -> uploadAllFiles());
        listViewFiles.setOnItemClickListener((parent, view, position, id) -> {
            openFile(fileList.get(position));
        });
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
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete all reports?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> deleteAllReports())
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void deleteAllReports() {
        // Get the directory where the files are saved
        File directory = getExternalFilesDir(null);
        if (directory != null && directory.isDirectory()) {
            // List all files in the directory
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        boolean deleted = file.delete();
                        if (deleted) {
                            Log.d("DeleteReports", "Deleted file: " + file.getName());
                        } else {
                            Log.d("DeleteReports", "Failed to delete file: " + file.getName());
                        }
                    }
                }
            }
        }
        // Update the UI if necessary, assuming you have a list and adapter to update
        if (listViewFiles != null && listViewFiles.getAdapter() instanceof ArrayAdapter) {
            ((ArrayAdapter<String>) listViewFiles.getAdapter()).clear();
            ((ArrayAdapter<String>) listViewFiles.getAdapter()).notifyDataSetChanged();
        }
        Toast.makeText(this, "All reports deleted", Toast.LENGTH_SHORT).show();
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
}