package com.example.accessibilityplay;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

public class Tutorial extends AppCompatActivity {
    private static String TAG = "Tutorial.java";
    private int val = 0;
    private int currentIntervention = 0, currentLevel = 2;
    private boolean isTap = false;
    private TextView tapInstructions, swipeInstructions;
    private Button tapTutorialProceedBtn, swipeTutorialProceedBtn;
    private SeekBar swipeProgressBar, tapProgressBar;
    private RadioGroup tapLevelSelector, swipeLevelSelector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        CoreService.isInTutorial = true;
        CoreService.inTutorialMainPage = true;
        ConstraintLayout mainLayout = findViewById(R.id.mainLayout);
        ConstraintLayout tapExperienceLayout = findViewById(R.id.tapExperienceLayout);
        ConstraintLayout swipeExperienceLayout = findViewById(R.id.swipeExperienceLayout);
        swipeExperienceLayout.setVisibility(View.INVISIBLE);
        Button tapTutorial = findViewById(R.id.tapTutorialBtn);
        Button swipeTutorial = findViewById(R.id.swipeTutorialBtn);
        Button tapToAddOneBtn = findViewById(R.id.tapToAddOneBtn);
        tapTutorialProceedBtn = findViewById(R.id.tapTutorialProceedBtn);
        swipeTutorialProceedBtn = findViewById(R.id.swipeTutorialProceedBtn);
        TextView tapToAddOneDisplay = findViewById(R.id.tapToAddOneDisplay);
        tapInstructions = findViewById(R.id.tapInstructions);
        swipeInstructions = findViewById(R.id.swipeInstructions);
        tapProgressBar = findViewById(R.id.tapProgressBar);
        swipeProgressBar = findViewById(R.id.swipeProgressBar);
        tapLevelSelector = findViewById(R.id.tapLevelSelector);
        swipeLevelSelector = findViewById(R.id.swipeLevelSelector);
        tapToAddOneDisplay.setText(String.format(Locale.ENGLISH, "%d", val));
        mainLayout.setVisibility(View.VISIBLE);
        tapExperienceLayout.setVisibility(View.INVISIBLE);
        swipeExperienceLayout.setVisibility(View.INVISIBLE);

        tapTutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tapExperienceLayout.setVisibility(View.VISIBLE);
                swipeExperienceLayout.setVisibility(View.INVISIBLE);
                mainLayout.setVisibility(View.INVISIBLE);
                CoreService.inTutorialMainPage = false;
                startOverlay(true);
                tapToAddOneBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        val++;
                        tapToAddOneDisplay.setText(String.format(Locale.ENGLISH, "%d", val));
                    }
                });
                tapToAddOneBtn.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        val += 2;
                        tapToAddOneDisplay.setText(String.format(Locale.ENGLISH, "%d", val));
                        return true;
                    }
                });
                tapTutorialProceedBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currentIntervention ++;
                        proceed(true, currentIntervention);
                        tapProgressBar.setProgress(currentIntervention);
                    }
                });
                tapProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            currentIntervention = progress;
                            proceed(true, progress);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
            }
        });

        swipeTutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swipeExperienceLayout.setVisibility(View.VISIBLE);
                mainLayout.setVisibility(View.INVISIBLE);
                tapExperienceLayout.setVisibility(View.INVISIBLE);
                CoreService.inTutorialMainPage = false;
                startOverlay(false);
                swipeTutorialProceedBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currentIntervention ++;
                        proceed(false, currentIntervention);
                        swipeProgressBar.setProgress(currentIntervention);
                    }
                });
                swipeProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            Log.d(TAG, "onProgressChanged: " + progress);
                        if (fromUser) {
                            currentIntervention = progress;
                            proceed(false, progress);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
            }
        });
    }

    private void startOverlay(boolean isTap) {
        CoreService.coreService.launchOverlayWindow();
        if (isTap) {
            tapLevelSelector.setVisibility(View.INVISIBLE);
            tapInstructions.setText("Now you have interventions started. Tap \"+1\" button to see the effect. Tap \"proceed\" button to continue.");
        } else {
            swipeLevelSelector.setVisibility(View.INVISIBLE);
            swipeInstructions.setText("Now you have interventions started. Scroll up and down to see the effect. Tap \"proceed\" button to continue.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private void closeOverlay() {
         if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
    }

    private void reset() {
        CoreService.tapDelay = 0; // 500 ms intrinsic delay
        GestureDetector.TAP_THRESHOLD = 0;
        GestureDetector.LONG_PRESS_PROLONG = 0;
        CoreService.yOffset = 0;
        CoreService.isDoubleTapToSingleTap = false;
        CoreService.swipeDelay = 0;
        CoreService.swipeFingers = 1;
        CoreService.scrollRatio = 1;
        CoreService.reverseDirection = false;
    }

    private void tapLevelChange(int currentIntervention) {
        RadioButton level2 = findViewById(R.id.tapLevel2);
        if (currentIntervention == 0 || currentIntervention == 5) {
            // initial stage
            tapLevelSelector.setVisibility(View.INVISIBLE);
        } else if (currentIntervention == 3) {
            // double tap to single tap does not have 2 levels.
            tapLevelSelector.setVisibility(View.VISIBLE);
            level2.setVisibility(View.GONE);
        }
        else {
            tapLevelSelector.setVisibility(View.VISIBLE);
            level2.setVisibility(View.VISIBLE);
        }
        tapLevelSelector.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.tapLevel1) {
                    tapLevel1(currentIntervention);
                } else if (checkedId == R.id.tapLevel2) {
                    tapLevel2(currentIntervention);
                } else {
                    noIntervention();
                }
            }
        });
    }

    private void noIntervention() {
        closeOverlay();
        currentLevel = 0;
        tapInstructions.setText("Now you have no interventions.");
        swipeInstructions.setText("Now you have no interventions.");
    }

    private void tapLevel1(int currentIntervention) {
        if (currentLevel == 0) {
            if (!CoreService.isOverlayOn) CoreService.coreService.launchOverlayWindow();
        }
        currentLevel = 1;
        reset();
        switch (currentIntervention) {
            case 1:
                CoreService.tapDelay = 300;
                tapInstructions.setText("Now you have tap delay with 500ms. Observe the time the number takes to change.");
                return;
            case 2:
                GestureDetector.TAP_THRESHOLD = 100;
                tapInstructions.setText("Now you need to place your finger on the screen for 100ms to trigger a tap.");
                return;
            case 3:
                CoreService.isDoubleTapToSingleTap = true;
                tapInstructions.setText("Now you need to double tap to trigger a single tap, and double tap is not supported.");
                return;
            case 4:
                CoreService.yOffset = -100;
                tapInstructions.setText("Now the place of your tap is shifted 100dp upwards. Tap on somewhere below the \"+1\" button to get a sense of the distance.");
        }
    }

    private void tapLevel2(int currentIntervention) {
        if (currentLevel == 0) {
            if (!CoreService.isOverlayOn) CoreService.coreService.launchOverlayWindow();
        }
        currentLevel = 2;
        reset();
        switch (currentIntervention) {
            case 1:
                CoreService.tapDelay = 800;
                tapInstructions.setText("Now you have tap delay with 1s. Observe the time the number takes to change.");
                return;
            case 2:
                GestureDetector.TAP_THRESHOLD = 200;
                tapInstructions.setText("Now you need to place your finger on the screen for 200ms to trigger a tap.");
                return;
            case 4:
                CoreService.yOffset = -200;
                tapInstructions.setText("Now the place of your tap is shifted 200dp upwards. Tap on somewhere below the \"+1\" button to get a sense of the distance.");
        }
    }

    private void swipeLevelChange(int currentIntervention) {
        RadioButton level2 = findViewById(R.id.swipeLevel2);
        if (currentIntervention == 0 || currentIntervention == 5) {
            // initial stage
            swipeLevelSelector.setVisibility(View.INVISIBLE);
        } else if (currentIntervention == 4) {
            // reverse swipe direction does not have 2 levels.
            swipeLevelSelector.setVisibility(View.VISIBLE);
            level2.setVisibility(View.GONE);
        }
        else {
            swipeLevelSelector.setVisibility(View.VISIBLE);
            level2.setVisibility(View.VISIBLE);
        }
        swipeLevelSelector.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.swipeLevel1) {
                    swipeLevel1(currentIntervention);
                } else if (checkedId == R.id.swipeLevel2) {
                    swipeLevel2(currentIntervention);
                } else {
                    noIntervention();
                }
            }
        });
    }

    private void swipeLevel1(int currentIntervention) {
        if (currentLevel == 0) {
            if (!CoreService.isOverlayOn) CoreService.coreService.launchOverlayWindow();
        }
        currentLevel = 1;
        reset();
        switch (currentIntervention) {
            case 1:
                // TODO: to make sure the exact delay of this number.
                CoreService.swipeDelay = 300;
                swipeInstructions.setText("Now you have delay of 300 ms. Observe the time after a scroll takes effective.");
                return;
            case 2:
                CoreService.scrollRatio = 2;
                swipeInstructions.setText("Now your scroll is 2 times slower than your actual speed. Observe the time for a scroll to replay.");
                return;
            case 3:
                CoreService.swipeFingers = 2;
                swipeInstructions.setText("Now you need 2 fingers to scroll.");
                return;
            case 4:
                CoreService.reverseDirection = true;
                swipeInstructions.setText("Now your scroll direction is reversed.");
        }
    }

    private void swipeLevel2(int currentIntervention) {
        if (currentLevel == 0) {
            if (!CoreService.isOverlayOn) CoreService.coreService.launchOverlayWindow();
        }
        currentLevel = 2;
        reset();
        switch (currentIntervention) {
            case 1:
                CoreService.swipeDelay = 800;
                swipeInstructions.setText("Now you have delay of 800 ms. Observe the time after a scroll takes effective.");
                return;
            case 2:
                CoreService.scrollRatio = 4;
                swipeInstructions.setText("Now your scroll is 4 times slower than your actual speed. Observe the time for a scroll to replay.");
                return;
            case 3:
                CoreService.swipeFingers = 3;
                swipeInstructions.setText("Now you need 3 fingers to scroll.");
        }
    }

    private void proceed(boolean isTap, int currentIntervention) {
        Log.d(TAG, "proceed: " + currentIntervention);
        reset();
        if (currentIntervention != 5) {
            if (!CoreService.isOverlayOn) {
                CoreService.coreService.launchOverlayWindow();
            }
        }
        if (isTap) {
            tapLevelChange(currentIntervention);
            tapLevelSelector.check(R.id.tapLevel2);
            switch (currentIntervention) {
                case 0:
                    tapInstructions.setText("Now you have interventions started. Tap \"+1\" button to see the effect. Tap \"proceed\" button to continue.");
                    return;
                case 1:
                    // delay: 1s
                    CoreService.tapDelay = 800; // 200 ms intrinsic delay
                    tapInstructions.setText("Now you have tap delay with 1s. Observe the time the number takes to change.");
                    return;
                case 2:
                    // prolong: 200ms
                    GestureDetector.TAP_THRESHOLD = 200;
                    tapInstructions.setText("Now you need to place your finger on the screen for 200ms to trigger a tap.");
                    return;
                case 3:
                    // multi-finger: 2
                    CoreService.isDoubleTapToSingleTap = true;
                    tapInstructions.setText("Now you need to double tap to trigger a single tap, and double tap is not supported.");
                    tapLevelSelector.check(R.id.tapLevel1);
                    return;
                case 4:
                    // offset: y - 100
                    CoreService.yOffset = -200;
                    tapInstructions.setText("Now the place of your tap is shifted 200dp upwards. Tap on somewhere below the \"+1\" button to get a sense of the distance.");
                    return;
                case 5:
//                    // Long press
//                    tapInstructions.setText("Now if you want long press, you have to first press anywhere on the screen" +
//                            " for about 500ms until a popped-up notification shows. Then you are temporarily unfettered to allow long press. After you finish, the interventions will be enabled again. You can long click" +
//                            " the \"+1\" button to see the effect.");
//                    return;
//                case 6:
                    endOfTutorial(true);
            }
        } else {
            swipeLevelChange(currentIntervention);
            swipeLevelSelector.check(R.id.swipeLevel2);
            switch (currentIntervention) {
                case 0:
                    swipeInstructions.setText("Now you have interventions started. Scroll up and down to see the effect. Tap \"proceed\" button to continue.");
                    return;
                case 1: // delay: 1s
                    CoreService.swipeDelay = 800;
                    swipeInstructions.setText("Now you have delay of 800 ms. Observe the time after a scroll takes effective.");
                    return;
                case 2: // ratio: 1/5
                    CoreService.scrollRatio = 4;
                    swipeInstructions.setText("Now your scroll is 4 times slower than your actual speed. Observe the time for a scroll to replay.");
                    return;
                case 3: // multi-finger: 2
                    CoreService.swipeFingers = 3;
                    swipeInstructions.setText("Now you need 3 fingers to scroll.");
                    return;
                case 4: // direction: yes
                    swipeLevelSelector.check(R.id.swipeLevel1);
                    CoreService.reverseDirection = true;
                    swipeInstructions.setText("Now your scroll direction is reversed.");
                    return;
                case 5:
                    endOfTutorial(false);
            }
        }
    }

    private void endOfTutorial(boolean isTap) {
        if (isTap) {
            tapInstructions.setText("End of tutorial, tap \"proceed\" to exit");
            tapTutorialProceedBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    exit();
                }
            });
        } else {
            swipeInstructions.setText("End of tutorial, tap \"proceed\" to exit");
            swipeTutorialProceedBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    exit();
                }
            });
        }
        CoreService.coreService.closeOverlayWindow();
    }

    private void exit() {
        Log.d(TAG, "exit: " + CoreService.isInTutorial);
        CoreService.isInTutorial = false;
        super.onBackPressed();
    }
}