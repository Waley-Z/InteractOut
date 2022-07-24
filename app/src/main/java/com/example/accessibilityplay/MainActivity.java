package com.example.accessibilityplay;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";
    private int count = 0;
    private static final float[] scrollRatioValues = {5, 4, 3, 2, 1, 1f/2f, 1f/3f, 1f/4f, 1f/5f};



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Display display = getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getRealSize(point);
        Log.d(TAG, "onCreate: \n" + point.x + ' ' + point.y);
        MyAccessibilityService.screenWidth = point.x;
        MyAccessibilityService.screenHeight = point.y;
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


        // Offset
        TextView xOffsetDisplay = findViewById(R.id.xOffsetDisplay);
        TextView yOffsetDisplay = findViewById(R.id.yOffsetDisplay);
        SeekBar xOffsetBar = findViewById(R.id.xOffsetBar);
        SeekBar yOffsetBar = findViewById(R.id.yOffsetBar);
        int ABS_X_OFFSET = xOffsetBar.getMax() / 2;
        int ABS_Y_OFFSET = yOffsetBar.getMax() / 2;
        xOffsetBar.setProgress(ABS_X_OFFSET);
        yOffsetBar.setProgress(ABS_Y_OFFSET);
        xOffsetDisplay.setText(String.format(Locale.ENGLISH, "%d", xOffsetBar.getProgress() - ABS_X_OFFSET));
        yOffsetDisplay.setText(String.format(Locale.ENGLISH, "%d", yOffsetBar.getProgress() - ABS_Y_OFFSET));
        xOffsetBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                MyAccessibilityService.xOffset = progress - ABS_X_OFFSET;
                xOffsetDisplay.setText(String.format(Locale.ENGLISH, "%d", progress - ABS_X_OFFSET));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        yOffsetBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                MyAccessibilityService.yOffset = progress - ABS_Y_OFFSET;
                yOffsetDisplay.setText(String.format(Locale.ENGLISH, "%d", progress - ABS_Y_OFFSET));
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