package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.firebase.database.DataSnapshot;


public class SettingsActivity extends AppCompatActivity {

    private EditText editTextUpdateEmail, editTextUpdateName, editTextUpdatePhone,editTextUpdateSection, editTextUpdateSkillSet;
    private String email, name, phone,section,skillSet;
    private FirebaseAuth auth;
    private ProgressBar progressBar;
    private Button saveSettingsButton, logoutButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        progressBar = findViewById(R.id.save_progressBar);
        logoutButton = findViewById(R.id.logout_btn);
        editTextUpdateName = findViewById(R.id.edit_text_update_name);
        editTextUpdateEmail = findViewById(R.id.edit_text_update_email);
        editTextUpdatePhone = findViewById(R.id.edit_text_update_phone);
        editTextUpdateSection = findViewById(R.id.edit_text_update_section);
        editTextUpdateSkillSet = findViewById(R.id.edit_text_update_skillset);
        saveSettingsButton = findViewById(R.id.save_settings_btn);

        auth =FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        showProfile(user);

        saveSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings(user);
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });


    }

    private void saveSettings(FirebaseUser user) {
        String userID = user.getUid();
        if(TextUtils.isEmpty(name)){
            Toast.makeText(SettingsActivity.this,"Please enter your name",Toast.LENGTH_SHORT).show();
            editTextUpdateName.setError("Name is required");
            editTextUpdateName.requestFocus();
        } else if (TextUtils.isEmpty(email)) {
            Toast.makeText(SettingsActivity.this,"Please enter your email",Toast.LENGTH_SHORT).show();
            editTextUpdateEmail.setError("Email is required");
            editTextUpdateEmail.requestFocus();
        } else if (TextUtils.isEmpty(phone)) {
            Toast.makeText(SettingsActivity.this,"Please enter your phone number",Toast.LENGTH_SHORT).show();
            editTextUpdatePhone.setError("Phone is required");
            editTextUpdatePhone.requestFocus();
        } else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(SettingsActivity.this,"Please re-enter your email",Toast.LENGTH_SHORT).show();
            editTextUpdateEmail.setError("Invalid email");
            editTextUpdateEmail.requestFocus();
        } else{
            name = editTextUpdateName.getText().toString();
            phone = editTextUpdatePhone.getText().toString();
            email = editTextUpdateEmail.getText().toString();
            section = editTextUpdateSection.getText().toString();
            skillSet = editTextUpdateSkillSet.getText().toString();

            DatabaseReference reference = FirebaseDatabase.getInstance("https://bibbulmun-track-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("users");
            reference.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    UserData userData= snapshot.getValue(UserData.class);
                    userData.setName(name);
                    userData.setPhone(phone);
                    userData.setSection(section);
                    userData.setSkillSet(skillSet);
                    reference.child(userID).setValue(userData);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    private void showProfile(FirebaseUser firebaseUser) {
        String userID;
        userID = firebaseUser.getUid();
        DatabaseReference reference = FirebaseDatabase.getInstance("https://bibbulmun-track-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("users");
        progressBar.setVisibility(View.VISIBLE);

        reference.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserData userData= snapshot.getValue(UserData.class);
                if(userData != null) {
                    name = userData.getName();
                    editTextUpdateName.setText(name);
                    phone = userData.getPhone();
                    editTextUpdatePhone.setText(phone);
                    email = firebaseUser.getEmail();
                    editTextUpdateEmail.setText(email);
                    skillSet = userData.getSkillSet();
                    editTextUpdateSkillSet.setText(skillSet);
                    section = userData.getSection();
                    editTextUpdateSection.setText(section);
                }else {
                    Toast.makeText(SettingsActivity.this,"Error", Toast.LENGTH_SHORT).show();
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SettingsActivity.this,"Cancelled", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}