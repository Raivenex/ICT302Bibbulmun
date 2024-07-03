package com.example.myapplication;

import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
public class ViewUploadedReportActivity extends AppCompatActivity {
    private ListView listViewFiles;
    private List<String> fileList = new ArrayList<>();
    private ArrayAdapter<String> fileAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent;
                intent = new Intent(getApplicationContext(), ViewReportActivity.class);
                startActivity(intent);
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this,onBackPressedCallback);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_uploaded_report);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        listViewFiles = findViewById(R.id.listViewUserFiles);
        fileAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileList);
        listViewFiles.setAdapter(fileAdapter);

        if (user != null) {
            retrieveUserFiles(user.getUid());
        }

        listViewFiles.setOnItemClickListener((parent, view, position, id) -> {
            String fileName = fileList.get(position);
            downloadAndOpenFile("Reports/" + user.getUid() + "/" + fileName);
        });
    }
    private void retrieveUserFiles(String userId) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference userFilesRef = storageRef.child("Reports/" + userId);
        userFilesRef.listAll()
                .addOnSuccessListener(listResult -> {
                    fileList.clear();
                    for (StorageReference item : listResult.getItems()) {
                        if (item.getName().endsWith(".xlsx")) {
                            fileList.add(item.getName());
                        }
                    }
                    fileAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ViewUploadedReportActivity.this, "Failed to retrieve files: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void downloadAndOpenFile(String fullPath) {
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(fullPath);

        ref.getDownloadUrl().addOnSuccessListener(uri -> {
            DownloadManager.Request request = new DownloadManager.Request(uri)
                    .setTitle("Download File") // Title of the Download Notification
                    .setDescription("Downloading") // Description of the Download Notification
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED) // Visibility of the download Notification
                    .setAllowedOverMetered(true) // Set if download is allowed on Mobile network
                    .setAllowedOverRoaming(true); // Set if download is allowed on roaming network

            // Save the file in the public Downloads folder
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, uri.getLastPathSegment());

            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                long downloadID = downloadManager.enqueue(request); // Enqueue a new download
                Toast.makeText(getApplicationContext(), "Download Started...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Download manager is not available", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getApplicationContext(), "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void openFile(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No application found to open Excel files", Toast.LENGTH_LONG).show();
        }
    }
}