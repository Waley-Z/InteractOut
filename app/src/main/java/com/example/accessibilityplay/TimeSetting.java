package com.example.accessibilityplay;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.Locale;

public class TimeSetting extends AppCompatActivity implements NumberPicker.OnValueChangeListener, NumberPicker.OnScrollListener, NumberPicker.Formatter{
    private String currentPackageName, TAG = "TimeSetting.java";
    private int currentIdx = 0;
    private long timeLimit = 0;
    private NumberPicker hourPicker, minutePicker, secondPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_setting);
        currentPackageName = PanelActivity.packageToSetTime.get(0);
        Button timeSetBtn = findViewById(R.id.timeSetBtn);
        TextView instruction = findViewById(R.id.currentApp);
        instruction.setText(CoreService.packageNameMap.get(currentPackageName));

        hourPicker = (NumberPicker) findViewById(R.id.hourPicker);
        minutePicker = (NumberPicker) findViewById(R.id.minutePicker);
        secondPicker = (NumberPicker) findViewById(R.id.secondPicker);
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        secondPicker.setMinValue(0);
        secondPicker.setMaxValue(59);
        hourPicker.setOnScrollListener(this);
        minutePicker.setOnScrollListener(this);
        secondPicker.setOnScrollListener(this);
        hourPicker.setOnValueChangedListener(this);
        minutePicker.setOnValueChangedListener(this);
        secondPicker.setOnValueChangedListener(this);
        View.OnClickListener lastOne = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CoreService.appTargetTime.put(currentPackageName, timeLimit);
                CoreService.appGrantedTime.put(currentPackageName, 0L);
                PanelActivity.packageToSetTime.removeAllElements();
                String content = String.format(Locale.ENGLISH, "APP_TIME_LIMIT;%d;%s;%s\n", System.currentTimeMillis(), currentPackageName, timeLimit);
                CoreService.coreService.writeToFile(CoreService.dataFileUri, content);
                TimeSetting.this.onBackPressed();
            }
        };

        View.OnClickListener notLastOne = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentIdx == PanelActivity.packageToSetTime.size() - 2) {
                    // the second last one now
                    timeSetBtn.setOnClickListener(lastOne);
                }
                CoreService.appTargetTime.put(currentPackageName, timeLimit);
                CoreService.appGrantedTime.put(currentPackageName, 0L);
                currentIdx++;
                currentPackageName = PanelActivity.packageToSetTime.get((currentIdx));
                instruction.setText(CoreService.packageNameMap.get(currentPackageName));
                String content = String.format(Locale.ENGLISH, "APP_TIME_LIMIT;%d;%s;%s\n", System.currentTimeMillis(), currentPackageName, timeLimit);
                CoreService.coreService.writeToFile(CoreService.dataFileUri, content);
            }
        };

        if (PanelActivity.packageToSetTime.size() < 2) {
            timeSetBtn.setOnClickListener(lastOne);
        } else {
            timeSetBtn.setOnClickListener(notLastOne);
        }
    }

    @Override
    public String format(int value) {
        return null;
    }

    @Override
    public void onScrollStateChange(NumberPicker view, int scrollState) {
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            int hour = hourPicker.getValue();
            int minute = minutePicker.getValue();
            int second = secondPicker.getValue();
            timeLimit = (hour * 3600L + minute * 60L + second) * 1000;
    }
}