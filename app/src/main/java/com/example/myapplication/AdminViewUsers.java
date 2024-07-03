package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminViewUsers extends AppCompatActivity {
    private EditText editTextName, editTextPhone, editTextSection, editTextSkillSet, editTextHours;
    private Spinner spinnerSelectUser;
    private Button buttonSave;
    private Map<String, String> userMap = new HashMap<>();
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
        setContentView(R.layout.activity_admin_view_users);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        spinnerSelectUser = findViewById(R.id.spinnerSelectUser);
        editTextName = findViewById(R.id.edit_text_name);
        editTextPhone = findViewById(R.id.edit_text_phone);
        editTextSection = findViewById(R.id.edit_text_section);
        editTextSkillSet = findViewById(R.id.edit_text_skillset);
        editTextHours = findViewById(R.id.edit_text_hours);
        buttonSave = findViewById(R.id.buttonSave);
        buttonSave.setOnClickListener(v -> saveUserDetails());

        setupSpinner();
    }
    private void setupSpinner() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance("https://bibbulmun-track-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("users");
        List<String> userNames = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, userNames);
        spinnerSelectUser.setAdapter(adapter);

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userNames.clear();
                userMap.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String userId = snapshot.getKey();
                    String userName = snapshot.child("name").getValue(String.class);
                    userNames.add(userName);
                    userMap.put(userName, userId);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AdminViewUsers.this, "Failed to load users.", Toast.LENGTH_SHORT).show();
            }
        });

        spinnerSelectUser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedUserName = parent.getItemAtPosition(position).toString();
                String ID = userMap.get(selectedUserName);
                loadUserDetails(ID);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadUserDetails(String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance("https://bibbulmun-track-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("users").child(userId);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserData userData = dataSnapshot.getValue(UserData.class);
                if (userData != null) {
                    editTextName.setText(userData.getName());
                    editTextPhone.setText(userData.getPhone());
                    editTextSection.setText(userData.getSection());
                    editTextSkillSet.setText(userData.getSkillSet());
                    editTextHours.setText(String.valueOf(userData.getHours()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AdminViewUsers.this, "Failed to retrieve user details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserDetails() {
        if (!validateInput()) {
            Toast.makeText(this, "Please correct input errors", Toast.LENGTH_SHORT).show();
            return; // Stop further execution if validation fails
        }

        String selectedUserName = spinnerSelectUser.getSelectedItem().toString();
        String userId = userMap.get(selectedUserName);
        DatabaseReference userRef = FirebaseDatabase.getInstance("https://bibbulmun-track-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("users").child(userId);

        HashMap<String, Object> updates = new HashMap<>();
        updates.put("name", editTextName.getText().toString());
        updates.put("phone", editTextPhone.getText().toString());
        updates.put("section", editTextSection.getText().toString());
        updates.put("skillSet", editTextSkillSet.getText().toString());
        updates.put("hours", Long.parseLong(editTextHours.getText().toString()));

        userRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(AdminViewUsers.this, "User details updated successfully.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(AdminViewUsers.this, "Failed to update user details.", Toast.LENGTH_SHORT).show());
    }
    //Validate

    private boolean validateInput() {
        String name = editTextName.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String section = editTextSection.getText().toString().trim();
        String skillSet = editTextSkillSet.getText().toString().trim();
        String hours = editTextHours.getText().toString().trim();

        // Validate Name (only alphabets and spaces)
        if (!name.matches("[a-zA-Z\\s]+")) {
            editTextName.setError("Invalid name; only letters and spaces allowed.");
            return false;
        }

        // Validate Phone (only numbers and common symbols)
        if (!phone.matches("[0-9+\\-\\s()]+")) {
            editTextPhone.setError("Invalid phone number; only numbers and symbols (+,-,(),spaces) allowed.");
            return false;
        }

        // Validate Section (only alphabets and spaces)
        if (!section.matches("[a-zA-Z\\s]+")) {
            editTextSection.setError("Invalid section; only letters and spaces allowed.");
            return false;
        }

        // Validate SkillSet (comma-separated values, each trimmed and checked if alphabetical)
        String[] skills = skillSet.split(",");
        for (String skill : skills) {
            if (!skill.trim().matches("[a-zA-Z\\s]+")) {
                editTextSkillSet.setError("Skills should be comma-separated and alphabetic only.");
                return false;
            }
        }

        // Validate Hours (numeric only)
        if (!hours.matches("\\d+")) {
            editTextHours.setError("Invalid hours; only numeric values allowed.");
            return false;
        }
        return true;
    }

}