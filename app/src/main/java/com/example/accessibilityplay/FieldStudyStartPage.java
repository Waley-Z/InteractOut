package com.example.accessibilityplay;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;

import com.google.android.material.chip.Chip;

public class FieldStudyStartPage extends AppCompatActivity {
    private AlertDialog alertDialog;
    private String TAG = "FieldStudyStartPage";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_study_start_page);
        Button userPageBtn = findViewById(R.id.goToUserPageBtn);
        userPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToUserPage();
            }
        });
        Button surveyLink = findViewById(R.id.surveyLink);
        surveyLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSurvey();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (CoreService.packageNameMap.size() == 0) {
            promptAccessibilitySettingNote();
        } else {

        }
    }

    private void goToUserPage() {
        Intent intent = new Intent(this, PanelActivity.class);
        startActivity(intent);
    }

    private void startSurvey() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://forms.gle/4hh6iihVkFTMj66U9"));
        startActivity(intent);
    }

    private void promptAccessibilitySettingNote() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("Warning");
        alertBuilder.setItems(new String[]{"Please enable accessibility settings!"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });
        alertBuilder.setPositiveButton("Go to settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                alertDialog.dismiss();
            }
        });
        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });
        alertDialog = alertBuilder.create();
        alertDialog.show();
    }
}