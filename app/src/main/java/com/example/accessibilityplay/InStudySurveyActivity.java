package com.example.accessibilityplay;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class InStudySurveyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_study_survey);
        CoreService.coreService.broadcastInStudySurvey("In Study Survey", "Please take the survey today");
        super.onBackPressed();
    }
}