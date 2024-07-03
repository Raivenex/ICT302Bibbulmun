package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminViewReports extends AppCompatActivity {
    private ListView listViewFiles;
    private List<String> fileList = new ArrayList<>();
    private ArrayAdapter<String> fileAdapter;
    private Map<String, String> userMap = new HashMap<>();
    private Map<String, String> fileNameToPathMap = new HashMap<>();
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
                intent = new Intent(getApplicationContext(), AdminMainActivity.class);
                startActivity(intent);
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this,onBackPressedCallback);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_view_reports);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        listViewFiles = findViewById(R.id.listViewFiles);
        Spinner userSpinner = findViewById(R.id.spinnerSelectUser);
        fileAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileList);
        listViewFiles.setAdapter(fileAdapter);
        fetchUsers(userSpinner);
        userSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedUserName = (String) parent.getItemAtPosition(position);
                if (selectedUserName.equals("All")) {
                    retrieveFilesFromAllUsers();
                } else {
                    String selectedUserId = userMap.get(selectedUserName);
                    if (selectedUserId != null) {
                        retrieveFiles(selectedUserId);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // This method is also required, even if it does nothing.
            }
        });
        listViewFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String fileName = fileList.get(position);
                String path = fileNameToPathMap.get(fileName);
                if (path != null) {
                    downloadFile(path);
                    openFile(fileName);
                } else {
                    Toast.makeText(AdminViewReports.this, "File path not found", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void retrieveFiles(String userId) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference userFilesRef = storageRef.child("Reports/" + userId);
        userFilesRef.listAll()
                .addOnSuccessListener(listResult -> {
                    fileList.clear();
                    for (StorageReference item : listResult.getItems()) {
                        // Filter for .xlsx files only
                        if (item.getName().endsWith(".xlsx")) {
                            fileList.add(item.getName());
                            fileNameToPathMap.put(item.getName(),"Reports/" + userId + "/" + item.getName());
                        }
                    }
                    fileAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminViewReports.this, "Failed to retrieve files: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void fetchUsers(Spinner userSpinner) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance("https://bibbulmun-track-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> userNames = new ArrayList<>();
                userNames.add("All"); // Add 'All' as the first option in the spinner
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String userName = snapshot.child("name").getValue(String.class);
                    String userId = snapshot.getKey();
                    userNames.add(userName);
                    userMap.put(userName, userId);
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(AdminViewReports.this,
                        android.R.layout.simple_spinner_dropdown_item, userNames);
                userSpinner.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminViewReports.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void retrieveFilesFromAllUsers() {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        fileList.clear(); // Clear current file list
        for (String userId : userMap.values()) {
            StorageReference userFilesRef = storageRef.child("Reports/" + userId);
            userFilesRef.listAll()
                    .addOnSuccessListener(listResult -> {
                        for (StorageReference item : listResult.getItems()) {
                            if (item.getName().endsWith(".xlsx")) {
                                fileList.add(item.getName());
                                fileNameToPathMap.put(item.getName(),"Reports/" + userId + "/" + item.getName());
                            }
                        }
                        fileAdapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AdminViewReports.this, "Failed to retrieve files for all users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
    private void downloadFile(String fullPath) {
        StorageReference fileRef = FirebaseStorage.getInstance().getReference().child(fullPath);

        // Define where you want to save the file
        File localFile = new File(getExternalFilesDir(null), fullPath.substring(fullPath.lastIndexOf('/') + 1));

        fileRef.getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(AdminViewReports.this, "Download successful: " + localFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                }).addOnFailureListener(exception -> {
                    Toast.makeText(AdminViewReports.this, "Download failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void openFile(String fileName) {
        File file = new File(getExternalFilesDir(null), fileName);  // 1. Locate the file
        Uri uri = FileProvider.getUriForFile(this, "com.example.myapplication.fileprovider", file);  // 2. Create a URI

        Intent intent = new Intent(Intent.ACTION_VIEW);  // 3. Setup Intent
        intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");  // Explicit MIME type
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);  // Grant temporary read permission

        try {
            startActivity(intent);  // 4. Attempt to open the file
        } catch (Exception e) {
            Toast.makeText(this, "No application found to open Excel files", Toast.LENGTH_LONG).show();  // Handle exceptions
        }
    }
}

