package com.example.accessibilityplay;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.progressindicator.CircularProgressIndicator;

public class LabMode extends AppCompatActivity {
    private static int STAGE_TIME = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_mode);
        TextView tutorial = findViewById(R.id.tutorialText);
        NumberPicker appPicker = findViewById(R.id.appPicker);
        NumberPicker numberPicker = findViewById(R.id.numberPicker);
        Button startBtn = findViewById(R.id.startBtn);
        CircularProgressIndicator timeRemain = findViewById(R.id.timeIndicator);
        timeRemain.setVisibility(View.INVISIBLE);
        timeRemain.setIndeterminate(false);
        timeRemain.setMax(STAGE_TIME);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeRemain.setVisibility(View.VISIBLE);
                tutorial.setVisibility(View.INVISIBLE);
                appPicker.setVisibility(View.INVISIBLE);
                numberPicker.setVisibility(View.INVISIBLE);
                CountDownTimer countDownTimer = new CountDownTimer(10000, 10) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        timeRemain.setProgress((int) millisUntilFinished);
                    }

                    @Override
                    public void onFinish() {
                        timeRemain.setVisibility(View.INVISIBLE);
                        tutorial.setVisibility(View.VISIBLE);
                        appPicker.setVisibility(View.VISIBLE);
                        numberPicker.setVisibility(View.VISIBLE);
                        timeRemain.setProgress(0);
                        Toast.makeText(LabMode.this, "A stage finished", Toast.LENGTH_SHORT).show();
                    }
                }.start();
            }
        });

    }
}