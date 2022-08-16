package com.example.accessibilityplay;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
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
        CoreService.screenWidth = point.x;
        CoreService.screenHeight = point.y;
        // disable button
        Button btn = findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CoreService.coreService.disableSelf();
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
        revertDirectionSwitch.setChecked(CoreService.reverseDirection);
        revertDirectionSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            CoreService.reverseDirection = b;
        });

        // tap delay
        SeekBar tapDelay = (SeekBar) findViewById(R.id.tapDelayBar);
        TextView tapDelayDisplay = findViewById(R.id.tapDelayDisplay);
        int MAXPROGRESS = 2000;
        tapDelay.setMax(MAXPROGRESS);
        tapDelay.setProgress(CoreService.tapDelay);
        tapDelayDisplay.setText(String.format(Locale.ENGLISH,"%d ms", tapDelay.getProgress()));
        tapDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tapDelayDisplay.setText(String.format(Locale.ENGLISH,"%d ms", progress));
                CoreService.tapDelay = progress;
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
                CoreService.isDisablingWindowAllowed = b;
        });

        // scroll ratio
        TextView scrollRatioDisplay = findViewById(R.id.scrollRatioDisplay);
        SeekBar scrollRatioBar = (SeekBar) findViewById(R.id.scrollRatioBar);
        int progress = (CoreService.scrollRatio == 1f / 5f) ? 0 :
                (CoreService.scrollRatio == 1f / 4f) ? 1 :
                (CoreService.scrollRatio == 1f / 3f) ? 2 :
                (CoreService.scrollRatio == 1f / 2f) ? 3 :
                (CoreService.scrollRatio == 1) ? 4 :
                (CoreService.scrollRatio == 2) ? 5 :
                (CoreService.scrollRatio == 3) ? 6 :
                (CoreService.scrollRatio == 5) ? 7 : 8;
        scrollRatioBar.setProgress(progress);
        scrollRatioDisplay.setText(String.format(Locale.ENGLISH, "x%.2f", scrollRatioValues[4]));
        scrollRatioBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                CoreService.scrollRatio = scrollRatioValues[progress];
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
        swipeFingersBar.setProgress(CoreService.swipeFingers - 1);
        swipeFingersDisplay.setText(String.format(Locale.ENGLISH, "%d", swipeFingersBar.getProgress() + 1));
        swipeFingersBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                CoreService.swipeFingers = progress + 1;
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
        tapThresholdBar.setProgress(GestureDetector.TAP_THRESHOLD);
        longPressProlongBar.setProgress(GestureDetector.LONG_PRESS_PROLONG);

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
        xOffsetBar.setProgress(CoreService.xOffset + ABS_X_OFFSET);
        yOffsetBar.setProgress(CoreService.yOffset + ABS_Y_OFFSET);
        xOffsetDisplay.setText(String.format(Locale.ENGLISH, "%d", xOffsetBar.getProgress() - ABS_X_OFFSET));
        yOffsetDisplay.setText(String.format(Locale.ENGLISH, "%d", yOffsetBar.getProgress() - ABS_Y_OFFSET));
        xOffsetBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                CoreService.xOffset = progress - ABS_X_OFFSET;
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
                CoreService.yOffset = progress - ABS_Y_OFFSET;
                yOffsetDisplay.setText(String.format(Locale.ENGLISH, "%d", progress - ABS_Y_OFFSET));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // fingers to tap
        SeekBar tapFingersBar = findViewById(R.id.tapFingersBar);
        TextView tapFingersDisplay = findViewById(R.id.tapFingersDisplay);
        tapFingersBar.setProgress(CoreService.minimumFingerToTap - 1);
        tapFingersDisplay.setText(String.format(Locale.ENGLISH, "%d", tapFingersBar.getProgress()+1));
        tapFingersBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tapFingersDisplay.setText(String.format(Locale.ENGLISH, "%d", progress+1));
                CoreService.minimumFingerToTap = progress + 1;
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