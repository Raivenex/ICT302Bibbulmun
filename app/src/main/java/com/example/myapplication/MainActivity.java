package com.example.myapplication;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private Button buttonLogout;
    private FloatingActionButton addButton, reportArchiveButton, createReportButton, settingButton;
    private TextView textViewName, textViewHoursVolunteered, textViewMySections, textViewListofSkills;
    private String Name,hoursVolunteered, mySection, mySkillSet;
    private boolean clicked = true;
    private Animation rotateOpen;
    private Animation rotateClose;
    private Animation fromBottom;
    private Animation toBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textViewName = findViewById(R.id.DisplayName);
        textViewHoursVolunteered = findViewById(R.id.hoursWorkedTextView);
        textViewMySections = findViewById(R.id.mySection);
        textViewListofSkills = findViewById(R.id.listOfSkills);
        auth = FirebaseAuth.getInstance();
        addButton = findViewById(R.id.add_btn);
        settingButton = findViewById(R.id.settings_btn);
        reportArchiveButton = findViewById(R.id.reportArchive_btn);
        createReportButton = findViewById(R.id.createReport_btn);
        FirebaseUser User = auth.getCurrentUser();
        if(User == null){
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        }
        else{
            loadUserDetails(User);
        }

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setVisibility(clicked);
                if (clicked) {
                    addButton.startAnimation(getRotateOpen());
                    settingButton.startAnimation(getFromBottom());
                    reportArchiveButton.startAnimation(getFromBottom());
                    createReportButton.startAnimation(getFromBottom());

                    createReportButton.setClickable(false);
                    settingButton.setClickable(true);
                    reportArchiveButton.setClickable(false);

                } else {
                    addButton.startAnimation(getRotateClose());
                    settingButton.startAnimation(getToBottom());
                    reportArchiveButton.startAnimation(getToBottom());
                    createReportButton.startAnimation(getToBottom());
                    settingButton.setClickable(false);
                }
                //setAnimation(clicked);
                clicked = !clicked;
            }
        });

        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
                finish();;
            }
        });

    }


    private void loadUserDetails(FirebaseUser firebaseUser) {
        String userID = firebaseUser.getUid();

        DatabaseReference reference = FirebaseDatabase.getInstance("https://bibbulmun-track-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("users");
        reference.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserData userData = snapshot.getValue(UserData.class);
                if(userData != null){
                    Name = userData.getName();
                    hoursVolunteered = String.valueOf(userData.getHours());
                    mySection = userData.getSection();
                    textViewName.setText("Welcome, "+ Name);
                    textViewName.setTextColor(getResources().getColor(R.color.white));
                    textViewName.setTextSize(37);
                    textViewHoursVolunteered.setText("Hours Volunteered: " + hoursVolunteered);
                    textViewHoursVolunteered.setTextSize(15);
                    textViewHoursVolunteered.setTextColor(getResources().getColor(R.color.black));
                    textViewMySections.setText("My section: " + mySection);
                    textViewMySections.setTextColor(getResources().getColor(R.color.black));
                    textViewMySections.setTextSize(15);
                    mySkillSet = userData.getSkillSet();
                    if (mySkillSet != null && !mySkillSet.isEmpty()) {
                        String[] skills = mySkillSet.split(",");
                        StringBuilder skillsFormatted = new StringBuilder();
                        for (String skill : skills) {
                            skillsFormatted.append("- ").append(skill.trim()).append("\n");
                        }
                        textViewListofSkills.setText(skillsFormatted.toString());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this,"Error",Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void setVisibility(boolean clicked) {
        if (clicked) {
            createReportButton.setVisibility(View.VISIBLE);
            settingButton.setVisibility(View.VISIBLE);
            reportArchiveButton.setVisibility(View.VISIBLE);
            //clickable

        } else {
            createReportButton.setVisibility(View.INVISIBLE);
            settingButton.setVisibility(View.INVISIBLE);
            reportArchiveButton.setVisibility(View.INVISIBLE);
            //clickable
            createReportButton.setClickable(true);
            settingButton.setClickable(true);
            reportArchiveButton.setClickable(true);
        }
    }
    private Animation getRotateOpen() {
        if (rotateOpen == null) {
            rotateOpen = AnimationUtils.loadAnimation(this, R.anim.rotate_open_anim);
        }
        return rotateOpen;
    }

    private Animation getRotateClose() {
        if (rotateClose == null) {
            rotateClose = AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim);
        }
        return rotateClose;
    }

    private Animation getFromBottom() {
        if (fromBottom == null) {
            fromBottom = AnimationUtils.loadAnimation(this, R.anim.from_bottom_anim);
        }
        return fromBottom;
    }

    private Animation getToBottom() {
        if (toBottom == null) {
            toBottom = AnimationUtils.loadAnimation(this, R.anim.to_bottom_anim);
        }
        return toBottom;
    }
}