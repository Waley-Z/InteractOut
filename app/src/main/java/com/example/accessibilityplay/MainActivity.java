package com.example.accessibilityplay;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    final static String TAG = "MainActivity";
    private int count = 0;
    private static final float[] scrollRatioValues = {5, 4, 3, 2, 1, 1f/2f, 1f/3f, 1f/4f, 1f/5f};

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyAccessibilityService.myAccessibilityService.disableSelf();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ContentResolver contentResolver = getContentResolver();
        Log.d(TAG, "onCreate: accessibility status is " + Settings.Secure.getString(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED));
        String status = Settings.Secure.getString(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED);
        if (status.equals("0")) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
        // disable button
        Button btn = findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyAccessibilityService.myAccessibilityService.disableSelf();
            }
        });
        // tap to +1
        Button btn2 = findViewById(R.id.button2);
        TextView textView = findViewById(R.id.textView);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                count++;
                textView.setText("" + count);
            }
        });
        // reverse swipe direction
        Switch revertDirectionSwitch = (Switch) findViewById(R.id.swipeDirectionSwtch);
        revertDirectionSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            MyAccessibilityService.revertDirection = b;
        });
        // tap delay
        SeekBar tapDelay = (SeekBar) findViewById(R.id.tapDelayBar);
        TextView tapDelayDisplay = findViewById(R.id.tapDelayDisplay);
        int MAXPROGRESS = 2000;
        tapDelay.setMax(MAXPROGRESS);
        tapDelay.setProgress(0);
        tapDelayDisplay.setText(String.format(Locale.ENGLISH,"%d ms", tapDelay.getProgress()));
        tapDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tapDelayDisplay.setText(String.format(Locale.ENGLISH,"%d ms", progress));
                MyAccessibilityService.tapDelay = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        // disabling window
        Switch disableArea = findViewById(R.id.disableAreaSwitch);
        disableArea.setOnCheckedChangeListener((c, b) -> {
            if (b) {
                MyAccessibilityService.myAccessibilityService.window.activateDisablingWindow();
            } else {
                MyAccessibilityService.myAccessibilityService.window.deactivateDisablingWindow();
            }
        });

        // scroll ratio
        TextView scrollRatioDisplay = findViewById(R.id.scrollRatioDisplay);
        SeekBar scrollRatioBar = (SeekBar) findViewById(R.id.scrollRatioBar);
        scrollRatioBar.setProgress(4);
        scrollRatioDisplay.setText(String.format(Locale.ENGLISH, "x%.2f", scrollRatioValues[4]));
        scrollRatioBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                MyAccessibilityService.scrollRatio = scrollRatioValues[progress];
                scrollRatioDisplay.setText(String.format(Locale.ENGLISH, "x%.2f", scrollRatioValues[8 - progress]));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // swipe fingers
        TextView swipeFingersDisplay = findViewById(R.id.swipeFingerDisplay);
        SeekBar swipeFingersBar = findViewById(R.id.swipeFingersBar);
        swipeFingersDisplay.setText(String.format(Locale.ENGLISH, "%d", swipeFingersBar.getProgress() + 1));
        swipeFingersBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                MyAccessibilityService.swipeFingers = progress + 1;
                swipeFingersDisplay.setText(String.format(Locale.ENGLISH, "%d", progress + 1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // two prolongs
        TextView prolongDisplay = findViewById(R.id.prolongDisplay);
        TextView tapThresholdDisplay = findViewById(R.id.tapThresholdDisplay);
        SeekBar tapThresholdBar = findViewById(R.id.tapThresholdBar);
        TextView longPressProlongDisplay = findViewById(R.id.longPressProlongDisplay);
        SeekBar longPressProlongBar = findViewById(R.id.longPressProlongBar);

        prolongDisplay.setText(
                String.format(
                        Locale.ENGLISH, "Tap threshold: %d ms; long press threshold: %d ms",
                        tapThresholdBar.getProgress(),
                        longPressProlongBar.getProgress() + tapThresholdBar.getProgress() + ViewConfiguration.getLongPressTimeout()
                )
        );

        tapThresholdDisplay.setText(String.format(Locale.ENGLISH, "%d ms", tapThresholdBar.getProgress()));
        tapThresholdBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                GestureDetector.TAP_THRESHOLD = progress;
                tapThresholdDisplay.setText(String.format(Locale.ENGLISH, "%d ms", progress));
                prolongDisplay.setText(
                        String.format(
                                Locale.ENGLISH, "Tap threshold: %d ms; long press threshold: %d ms",
                                progress,
                                longPressProlongBar.getProgress() + progress + ViewConfiguration.getLongPressTimeout()
                        )
                );
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        longPressProlongDisplay.setText(String.format(Locale.ENGLISH, "%d ms", longPressProlongBar.getProgress()));
        longPressProlongBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                GestureDetector.LONG_PRESS_PROLONG = progress;
                longPressProlongDisplay.setText(String.format(Locale.ENGLISH, "%d ms", progress));
                prolongDisplay.setText(
                        String.format(
                                Locale.ENGLISH, "Tap threshold: %d ms; long press threshold: %d ms",
                                tapThresholdBar.getProgress(),
                                tapThresholdBar.getProgress() + progress + ViewConfiguration.getLongPressTimeout()
                        )
                );
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
}