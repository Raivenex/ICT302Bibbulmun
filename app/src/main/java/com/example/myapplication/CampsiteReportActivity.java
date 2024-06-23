package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class CampsiteReportActivity extends AppCompatActivity {

    private Spinner spinnerCampsite, spinnerDistrict, spinnerCampsiteIssue;
    private Button locateButton, addReportButton;
    private ImageView imageUpload;
    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText etLocation, etActionTaken, etComments, etDateTime;
    private CheckBox cbAssistanceRequired;
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
        setContentView(R.layout.activity_campsite_report);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        spinnerCampsite = findViewById(R.id.spinnerCampsite);
        spinnerDistrict = findViewById(R.id.spinnerDistrict);
        spinnerCampsiteIssue = findViewById(R.id.spinnerCampsiteIssue);
        locateButton = findViewById(R.id.LocateButton);
        addReportButton = findViewById(R.id.btnAddReport);
        imageUpload = findViewById(R.id.imageUpload);
        imageUpload.setOnClickListener(v -> openFileChooser());
        etLocation = findViewById(R.id.crLocation);
        etActionTaken = findViewById(R.id.crActionTaken);
        etComments = findViewById(R.id.crComments);
        cbAssistanceRequired = findViewById(R.id.crcbAssistanceRequired);
        etDateTime = findViewById(R.id.crDateTime);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        populateSpinners();

        spinnerCampsite.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Only show additional fields if a valid issue type is selected (not the prompt)
                if (position != 0) { // Check if the selected item is not the prompt
                    // Show the additional fields
                    setFieldVisibility(View.VISIBLE);

                    // Set the current date and time
                    etDateTime.setText(getCurrentDateTime());
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
        locateButton.setOnClickListener(v -> {
            // Check for location permission and request if not granted
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                getLocation();
            }
        });

        addReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportDataToExcel();
                clearFields();
            }
        });

    }

    protected void onPause() {
        super.onPause();
        saveFormData();
    }

    protected void onResume() {
        super.onResume();
        restoreFormData();
    }

    private void saveFormData() {
        SharedPreferences prefs = getSharedPreferences("CampsiteFormData", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("Location", etLocation.getText().toString());
        editor.putInt("CampsiteSpinnerPosition", spinnerCampsite.getSelectedItemPosition());
        editor.putInt("CampsiteIssuePosition", spinnerCampsiteIssue.getSelectedItemPosition());
        editor.putInt("DistrictPosition", spinnerDistrict.getSelectedItemPosition());
        editor.putString("ActionsTaken", etActionTaken.getText().toString());
        editor.putBoolean("AssistanceRequired", cbAssistanceRequired.isChecked());
        editor.putString("Comments", etComments.getText().toString());
        editor.putString("DateAndTime", etDateTime.getText().toString());
        editor.apply(); // use apply instead of commit for background thread saving
    }
    private void restoreFormData() {
        SharedPreferences prefs = getSharedPreferences("FormData", MODE_PRIVATE);

        etLocation.setText(prefs.getString("Location", ""));
        spinnerCampsite.setSelection(prefs.getInt("CampsiteSpinnerPosition", 0));
        spinnerCampsiteIssue.setSelection(prefs.getInt("CampsiteIssuePosition", spinnerCampsiteIssue.getSelectedItemPosition()));
        spinnerDistrict.setSelection(prefs.getInt("DistrictPosition",spinnerDistrict.getSelectedItemPosition()));
        etActionTaken.setText(prefs.getString("ActionsTaken", ""));
        cbAssistanceRequired.setChecked(prefs.getBoolean("AssistanceRequired", false));
        etComments.setText(prefs.getString("Comments", ""));
        etDateTime.setText(prefs.getString("DateAndTime", ""));

    }

    private void populateSpinners(){
        //populate campsite issues
        ArrayAdapter<CharSequence> adapterCampsiteIssueType = ArrayAdapter.createFromResource(this,
                R.array.campsite_issues, android.R.layout.simple_spinner_item);
        adapterCampsiteIssueType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCampsiteIssue.setAdapter(adapterCampsiteIssueType);

        //populate campsite locations
        ArrayAdapter<CharSequence> adapterCampsites= ArrayAdapter.createFromResource(this,
                R.array.campsites, android.R.layout.simple_spinner_item);
        adapterCampsites.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCampsite.setAdapter(adapterCampsites);

        //populate district locations
        ArrayAdapter<CharSequence> adapterDistricts= ArrayAdapter.createFromResource(this,
                R.array.districts, android.R.layout.simple_spinner_item);
        adapterDistricts.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDistrict.setAdapter(adapterDistricts);
    }

    private void setFieldVisibility(int visibility){
        etLocation.setVisibility(visibility);
        etActionTaken.setVisibility(visibility);
        cbAssistanceRequired.setVisibility(visibility);
        etComments.setVisibility(visibility);
        addReportButton.setVisibility(visibility);
        imageUpload.setVisibility(visibility);
        locateButton.setVisibility(visibility);
        etDateTime.setVisibility(visibility);
        spinnerCampsiteIssue.setVisibility(visibility);
        spinnerDistrict.setVisibility(visibility);
    }

    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void getLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                etLocation.setText("Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude());
            } else {
                etLocation.setText("Unable to find location. Make sure location is enabled on the device.");
            }
        }
    }
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            imageUpload.setImageURI(imageUri);
            imageUpload.setTag(imageUri.toString());
        }
    }
    public void exportDataToExcel() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Campsite Report Data");

        // Create headers
        Row headerRow = sheet.createRow(0);
        String[] columns = {"Campsite", "District", "Issue", "Location", "Date and Time", "Actions Taken", "Assistance Required", "Comments","Image"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
        }

        // Adding form data
        Row dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellValue(spinnerCampsite.getSelectedItem().toString());
        dataRow.createCell(1).setCellValue(spinnerDistrict.getSelectedItem().toString());
        dataRow.createCell(2).setCellValue(spinnerCampsiteIssue.getSelectedItem().toString());
        dataRow.createCell(3).setCellValue(etLocation.getText().toString());
        dataRow.createCell(4).setCellValue(etDateTime.getText().toString());
        dataRow.createCell(5).setCellValue(etActionTaken.getText().toString());
        dataRow.createCell(6).setCellValue(cbAssistanceRequired.isChecked() ? "Yes" : "No");
        dataRow.createCell(7).setCellValue(etComments.getText().toString());

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
        String fileName = "CampsiteReport_" + timeStamp + ".xlsx";

        // Adding the image to the Excel file
        if (imageUpload.getTag() != null) {
            try {
                Uri imageUri = Uri.parse(imageUpload.getTag().toString());
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
        // Write the output to a file
        try {
            File file = new File(getExternalFilesDir(null), fileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();
            Toast.makeText(CampsiteReportActivity.this, "Report saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e("ExcelExport", "Failed to save the Excel file.", e);
            Toast.makeText(CampsiteReportActivity.this, "Failed to save report", Toast.LENGTH_SHORT).show();
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
    private void clearFields() {
        // Clear EditText fields
        etLocation.setText("");
        etActionTaken.setText("");
        etComments.setText("");
        etDateTime.setText("");

        // Reset CheckBox
        cbAssistanceRequired.setChecked(false);

        // Reset Spinners to the first item (assuming the first item is a prompt like "Select One")
        spinnerCampsite.setSelection(0);
        spinnerDistrict.setSelection(0);
        spinnerCampsiteIssue.setSelection(0);

        // If you are using an ImageView for displaying an image, clear it as well
        imageUpload.setImageDrawable(null); // This will remove the image from the ImageView
        imageUpload.setTag(null); // Clear any tag if you've set one when choosing an image

        // Optionally, if you're displaying a toast message upon clearing fields:
        Toast.makeText(this, "All fields have been cleared", Toast.LENGTH_SHORT).show();
    }
}