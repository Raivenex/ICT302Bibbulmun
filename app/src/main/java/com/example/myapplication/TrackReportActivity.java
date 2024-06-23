package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.location.LocationManager;
import android.Manifest;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.graphics.Bitmap;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class TrackReportActivity extends AppCompatActivity {
    private Spinner spinnerIssueType, spinnerSeverity, spinnerFrequency;
    private EditText trackLocation, trackActionTaken, trackComments, trackDateTime;
    private CheckBox trackAssistanceRequired;
    private Button btnAddReport;
    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView trackImageUpload;
    private Button btnGetLocation;
    private LocationManager locationManager;
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
        setContentView(R.layout.activity_track_report);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        spinnerIssueType = findViewById(R.id.spinnerIssueType);
        spinnerSeverity = findViewById(R.id.spinnerSeverity);
        spinnerFrequency = findViewById(R.id.spinnerFrequency);
        trackLocation = findViewById(R.id.etLocation);
        trackActionTaken = findViewById(R.id.etActionTaken);
        trackComments = findViewById(R.id.etComments);
        trackAssistanceRequired = findViewById(R.id.cbAssistanceRequired);
        btnAddReport = findViewById(R.id.btnAddReport);
        trackImageUpload = findViewById(R.id.imageUpload);
        trackImageUpload.setOnClickListener(v -> openFileChooser());
        //New shit to test
        trackLocation = findViewById(R.id.etLocation);
        btnGetLocation = findViewById(R.id.btnGetLocation);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        trackDateTime = findViewById(R.id.etDateTime);
        populateSpinners();

        // Setup Item Selected Listener for Issue Type Spinner
        spinnerIssueType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Only show additional fields if a valid issue type is selected (not the prompt)
                if (position != 0) { // Check if the selected item is not the prompt
                    // Show the additional fields
                    setFieldVisibility(View.VISIBLE);

                    // Set the current date and time
                    trackDateTime.setText(getCurrentDateTime());
                } else {
                    // Hide the additional fields
                    setFieldVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                setFieldVisibility(View.GONE);
            }
        });

        // Button to add the report
        btnAddReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportDataToExcelTrack();
                clearFields();
            }
        });

        btnGetLocation.setOnClickListener(v -> {
            // Check for location permission and request if not granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                getLocation();
            }
        });
    }
    private void populateSpinners() {
        // Populate Issue Type Spinner
        ArrayAdapter<CharSequence> adapterIssueType = ArrayAdapter.createFromResource(this,
                R.array.issue_types, android.R.layout.simple_spinner_item);
        adapterIssueType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIssueType.setAdapter(adapterIssueType);

        // Populate Severity Spinner
        ArrayAdapter<CharSequence> adapterSeverity = ArrayAdapter.createFromResource(this,
                R.array.severity, android.R.layout.simple_spinner_item);
        adapterSeverity.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSeverity.setAdapter(adapterSeverity);

        spinnerSeverity.setSelection(0, false);

        // Populate Frequency Spinner
        ArrayAdapter<CharSequence> adapterFrequency = ArrayAdapter.createFromResource(this,
                R.array.frequency, android.R.layout.simple_spinner_item);
        adapterFrequency.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrequency.setAdapter(adapterFrequency);

        spinnerFrequency.setSelection(0, false);

    }

    protected void onPause() {
        super.onPause();
        saveFormData();
    }

    protected void onResume() {
        super.onResume();
        restoreFormData();
    }

    private void setFieldVisibility(int visibility) {
        trackLocation.setVisibility(visibility);
        spinnerSeverity.setVisibility(visibility);
        spinnerFrequency.setVisibility(visibility);
        trackActionTaken.setVisibility(visibility);
        trackAssistanceRequired.setVisibility(visibility);
        trackComments.setVisibility(visibility);
        btnAddReport.setVisibility(visibility);
        trackImageUpload.setVisibility(visibility);
        btnGetLocation.setVisibility(visibility);
        trackDateTime.setVisibility(visibility);
    }

    private void clearFields() {
        trackLocation.setText("");
        trackActionTaken.setText("");
        trackComments.setText("");
        trackAssistanceRequired.setChecked(false);
        spinnerIssueType.setSelection(0);
        spinnerSeverity.setSelection(0);
        spinnerFrequency.setSelection(0);
    }
    //New shit
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            trackImageUpload.setImageURI(imageUri);
            trackImageUpload.setTag(imageUri.toString());
        }
    }

    private void getLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                trackLocation.setText("Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude());
            } else {
                trackLocation.setText("Unable to find location. Make sure location is enabled on the device.");
            }
        }
    }

    private void saveFormData() {
        SharedPreferences prefs = getSharedPreferences("FormData", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("Location", trackLocation.getText().toString());
        editor.putInt("IssueTypePosition", spinnerIssueType.getSelectedItemPosition());
        editor.putInt("SeverityPosition", spinnerSeverity.getSelectedItemPosition());
        editor.putInt("FrequencyPosition", spinnerFrequency.getSelectedItemPosition());
        editor.putString("ActionsTaken", trackActionTaken.getText().toString());
        editor.putBoolean("AssistanceRequired", trackAssistanceRequired.isChecked());
        editor.putString("Comments", trackComments.getText().toString());
        editor.putString("DateAndTime", trackDateTime.getText().toString());
        editor.apply(); // use apply instead of commit for background thread saving
    }
    private void restoreFormData() {
        SharedPreferences prefs = getSharedPreferences("TrackFormData", MODE_PRIVATE);

        trackLocation.setText(prefs.getString("Location", ""));
        spinnerIssueType.setSelection(prefs.getInt("IssueTypePosition", 0));
        spinnerSeverity.setSelection(prefs.getInt("SeverityPosition", 0));
        spinnerFrequency.setSelection(prefs.getInt("FrequencyPosition", 0));
        trackActionTaken.setText(prefs.getString("ActionsTaken", ""));
        trackAssistanceRequired.setChecked(prefs.getBoolean("AssistanceRequired", false));
        trackComments.setText(prefs.getString("Comments", ""));
        trackDateTime.setText(prefs.getString("DateAndTime", ""));

    }
    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    public void exportDataToExcelTrack() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Report Data");

        // Create headers
        Row headerRow = sheet.createRow(0);
        String[] columns = {"Issue Type", "Severity", "Frequency", "Location", "Date and Time", "Actions Taken", "Assistance Required", "Comments","Image"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
        }

        // Adding form data
        Row dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellValue(spinnerIssueType.getSelectedItem().toString());
        dataRow.createCell(1).setCellValue(spinnerSeverity.getSelectedItem().toString());
        dataRow.createCell(2).setCellValue(spinnerFrequency.getSelectedItem().toString());
        dataRow.createCell(3).setCellValue(trackLocation.getText().toString());
        dataRow.createCell(4).setCellValue(trackDateTime.getText().toString());
        dataRow.createCell(5).setCellValue(trackActionTaken.getText().toString());
        dataRow.createCell(6).setCellValue(trackAssistanceRequired.isChecked() ? "Yes" : "No");
        dataRow.createCell(7).setCellValue(trackComments.getText().toString());

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "ReportData_" + timeStamp + ".xlsx";

        // Adding the image to the Excel file
        if (trackImageUpload.getTag() != null) {
            try {
                Uri imageUri = Uri.parse(trackImageUpload.getTag().toString());
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] bytes = stream.toByteArray();
                addImageToSheet(workbook, sheet, bytes);
                stream.close();
            } catch (Exception e) {
                Log.e("ExcelExport", "Error adding image to Excel.", e);
            }
        }
        try {
            File file = new File(getExternalFilesDir(null), fileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();
            Toast.makeText(TrackReportActivity.this, "Report saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e("ExcelExport", "Failed to save the Excel file.", e);
            Toast.makeText(TrackReportActivity.this, "Failed to save report", Toast.LENGTH_SHORT).show();
        }
    }

    private void addImageToSheet(Workbook workbook, Sheet sheet, byte[] bytes) {
        int pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG); // Add the picture to the workbook
        CreationHelper helper = workbook.getCreationHelper();
        Drawing drawing = sheet.createDrawingPatriarch(); // Create a drawing patriarch to define the anchor points

        // This anchor tells where the picture will be positioned in the sheet
        ClientAnchor anchor = helper.createClientAnchor();
        anchor.setCol1(8);
        anchor.setRow1(1);
        anchor.setCol2(10);
        anchor.setRow2(6);
        // Create a picture at the location defined by the anchor points
        Picture pict = drawing.createPicture(anchor, pictureIdx);
    }
}