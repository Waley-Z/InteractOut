package com.example.accessibilityplay;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class LabMode extends AppCompatActivity implements NumberPicker.OnValueChangeListener {
    public static String answerString = "";
    public static int currentStage = 0;
    public static int pID = 0;
    public static int STAGE_TIME = 180000; // 3 min
    public static boolean isSurveyFinished = true;

    private static int TOTAL_STAGES = 16;
    private boolean isFirstStage = true;
    private Button startBtn, tutorialBtn, gobackBtn;
    private NumberPicker numberPicker, stagePicker;
    private TextView stageDisplay;
    private View.OnClickListener doneEarly, startAndContinue;
    private CountDownTimer countDownTimer;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_mode);
        CoreService.isInLabMode = true;
        TextView tutorial = findViewById(R.id.tutorialText);
        stageDisplay = findViewById(R.id.currentStageNumber);
        numberPicker = findViewById(R.id.numberPicker);
        numberPicker.setMaxValue(40);
        numberPicker.setOnValueChangedListener(this);
        stagePicker = findViewById(R.id.stagePicker);
        stagePicker.setMaxValue(15);
        stagePicker.setOnValueChangedListener(this);
        CoreService.packageChosen.removeAllElements();
        CoreService.appChosen.removeAllElements();
        startBtn = findViewById(R.id.startBtn);
        tutorialBtn = findViewById(R.id.tutorialBtn);
        gobackBtn = findViewById(R.id.gobackBtn);
        CircularProgressIndicator timeRemain = findViewById(R.id.timeIndicator);
        timeRemain.setVisibility(View.INVISIBLE);
        timeRemain.setIndeterminate(false);
        timeRemain.setMax(STAGE_TIME);
        gobackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mIntent;
                if ((pID % 2 != 0 && currentStage < 8) || (pID % 2 == 0 && currentStage >= 8)) {
                    mIntent = getPackageManager().getLaunchIntentForPackage("com.twitter.android");
                } else {
                    mIntent = getPackageManager().getLaunchIntentForPackage("com.teamlava.bubble");
                }
                String content = String.format(Locale.ENGLISH, "STAGE_CONTINUE,%d\n", System.currentTimeMillis());
                writeToFile(CoreService.participantFilename, content, MODE_APPEND);
                setIntervention(pID, currentStage);
                startActivity(mIntent);
            }
        });
        startAndContinue = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFirstStage) {
                    CoreService.prolongNoteShowTime = 15;
                    isFirstStage = false;
                    CoreService.participantFilename = String.format(Locale.ENGLISH, "P%s-%s-lab_study.txt", pID, System.currentTimeMillis());
                }
                if (isSurveyFinished) {
                    isSurveyFinished = false;
                    CoreService.isInTutorial = false;
                    setIntervention(pID, currentStage);
                    tutorial.setVisibility(View.INVISIBLE);
                    tutorialBtn.setVisibility(View.INVISIBLE);
                    numberPicker.setVisibility(View.INVISIBLE);
                    stagePicker.setVisibility(View.INVISIBLE);
                    gobackBtn.setVisibility(View.VISIBLE);
                    stageDisplay.setText(String.format(Locale.ENGLISH, "%d/%d stage(s) completed", currentStage, TOTAL_STAGES));
                    startBtn.setText("I'm done");
                    startBtn.setOnClickListener(doneEarly);
                    timeRemain.setVisibility(View.VISIBLE);
                    countDownTimer = new CountDownTimer(STAGE_TIME, 100) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            timeRemain.setProgress((int) millisUntilFinished);
                        }

                        @Override
                        public void onFinish() {
                            startBtn.setText("Continue");
                            startBtn.setOnClickListener(startAndContinue);
                            timeRemain.setVisibility(View.INVISIBLE);
                            gobackBtn.setVisibility(View.INVISIBLE);
                            timeRemain.setProgress(0);
                            CoreService.coreService.broadcast("Time is up", "Please tap to take a survey.", true);
                        }
                    };
                    countDownTimer.start();
                    Intent mIntent;
                    if ((pID % 2 != 0 && currentStage < 8) || (pID % 2 == 0 && currentStage >= 8)) {
                        mIntent = getPackageManager().getLaunchIntentForPackage("com.twitter.android");
                    } else {
                        mIntent = getPackageManager().getLaunchIntentForPackage("com.teamlava.bubble");
                    }
                    String content = String.format(Locale.ENGLISH, "STAGE_START,%d\n", System.currentTimeMillis());
                    writeToFile(CoreService.participantFilename, content, MODE_APPEND);
                    startActivity(mIntent);
                } else {
                    Toast.makeText(LabMode.this, "Please tap the banner in notification channel to finish the survey before you continue.", Toast.LENGTH_SHORT).show();
                    String content = String.format(Locale.ENGLISH, "INTENT_START_BEFORE_SURVEY,%d\n", System.currentTimeMillis());
                    writeToFile(CoreService.participantFilename, content, MODE_APPEND);
                }
            }
        };

        doneEarly = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBtn.setText("Continue");
                startBtn.setOnClickListener(startAndContinue);
                timeRemain.setVisibility(View.INVISIBLE);
                timeRemain.setProgress(0);
                countDownTimer.cancel();
//                String content = String.format(Locale.ENGLISH, "CLICK_DONE,%d\n", System.currentTimeMillis());
//                writeToFile(filename, content, MODE_APPEND);
                startActivity(new Intent(LabMode.this, LabQuiz.class));
            }
        };
        startBtn.setOnClickListener(startAndContinue);
    }

    public void setTutorialButton(View v) {
        startActivity(new Intent(this, Tutorial.class));
    }

    public void writeToFile(String filename, String content, int mode) {
        Log.d("File content", content);
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = openFileOutput(filename, mode);
            fileOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void readFile(String filename) throws IOException {
        try (FileInputStream fileInputStream = openFileInput(filename); Reader reader = new InputStreamReader(fileInputStream); BufferedReader bufferedReader = new BufferedReader(reader)) {
            StringBuilder result = new StringBuilder();
            String temp;
            while ((temp = bufferedReader.readLine()) != null) {
                result.append(temp);
            }
            Log.d(TAG, "readFile: \n" + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFile(String filename) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto","luttul@umich.edu", null));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Test subject");
        intent.putExtra(Intent.EXTRA_TEXT, "This is test text.");
        startActivity(Intent.createChooser(intent, "Choose an Email client :"));

    }

    private void setIntervention(int pID, int currentStage) {
        resetIntervention();
        if (!CoreService.isOverlayOn) CoreService.coreService.launchOverlayWindow();
        switch (pID % 10) {
            case 0:
                switch (currentStage) {
                    case 0:
                        // none
                        if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
                        toastMsg("No intervention");
                        return;
                    case 1:
                        // delay 500ms
                        tapDelay(false);
                        return;
                    case 2:
                        // delay 800ms
                        tapDelay(true);
                        return;
                    case 3:
                        // offset shift up 100dp
                        tapOffset(false);
                        return;
                    case 4:
                        // offset shift left 100dp
                        tapOffset(true);
                        return;
                    case 5:
                        // multiple: double tap to single tap
                        tapMultiple();
                        return;
                    case 6:
                        // prolong tap for 100ms
                        tapProlong(false);
                        return;
                    case 7:
                        // prolong tap for 200ms
                        tapProlong(true);
                        return;
                    case 8:
                        swipeDelay(false);
                        return;
                    case 9:
                        swipeDelay(true);
                        return;
                    case 10:
                        swipeRatio(true);
                        return;
                    case 11:
                        swipeRatio(false);
                        return;
                    case 12:
                        swipeDirection();
                        return;
                    case 13:
                        if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
                        toastMsg("No intervention");
                        return;
                    case 14:
                        swipeFingers(false);
                        return;
                    case 15:
                        swipeFingers(true);
                        return;
                }
            case 1: // pID = 1
                switch (currentStage) {
                    case 0:
                        swipeDirection();
                        return;
                    case 1:
                        swipeFingers(true);
                        return;
                    case 2:
                        swipeFingers(false);
                        return;
                    case 3:
                        swipeDelay(true);
                        return;
                    case 4:
                        swipeDelay(false);
                        return;
                    case 5:
                        if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
                        toastMsg("No intervention");
                        return;
                    case 6:
                        swipeRatio(false);
                        return;
                    case 7:
                        swipeRatio(true);
                        return;
                    case 8:
                        tapOffset(true);
                        return;
                    case 9:
                        tapOffset(false);
                        return;
                    case 10:
                        tapProlong(true);
                        return;
                    case 11:
                        tapProlong(false);
                        return;
                    case 12:
                        if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
                        toastMsg("No intervention");
                        return;
                    case 13:
                        tapMultiple();
                        return;
                    case 14:
                        tapDelay(false);
                        return;
                    case 15:
                        tapDelay(true);
                        return;
                }
            case 2: // pID = 2
                switch (currentStage) {
                    case 0:
                        tapMultiple();
                        return;
                    case 1:
                        tapProlong(false);
                        return;
                    case 2:
                        tapProlong(true);
                        return;
                    case 3:
                        tapDelay(false);
                        return;
                    case 4:
                        tapDelay(true);
                        return;
                    case 5:
                        tapOffset(true);
                        return;
                    case 6:
                        tapOffset(false);
                        return;
                    case 7:
                        if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
                        toastMsg("No intervention");
                        return;
                    case 8:
                        if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
                        toastMsg("No intervention");
                        return;
                    case 9:
                        swipeFingers(true);
                        return;
                    case 10:
                        swipeFingers(false);
                        return;
                    case 11:
                        swipeRatio(true);
                        return;
                    case 12:
                        swipeRatio(false);
                        return;
                    case 13:
                        swipeDirection();
                        return;
                    case 14:
                        swipeDelay(true);
                        return;
                    case 15:
                        swipeDelay(false);
                        return;
                }
            case 3: // pID = 3
                switch (currentStage) {
                    case 0:
                        swipeRatio(true);
                        return;
                    case 1:
                        swipeRatio(false);
                        return;
                    case 2:
                        swipeDelay(true);
                        return;
                    case 3:
                        swipeDelay(false);
                        return;
                    case 4:
                        if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
                        toastMsg("No intervention");
                        return;
                    case 5:
                        swipeDirection();
                        return;
                    case 6:
                        swipeFingers(true);
                        return;
                    case 7:
                        swipeFingers(false);
                        return;
                    case 8:
                        tapDelay(true);
                        return;
                    case 9:
                        tapDelay(false);
                        return;
                    case 10:
                        if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
                        toastMsg("No intervention");
                        return;
                    case 11:
                        tapMultiple();
                        return;
                    case 12:
                        tapOffset(false);
                        return;
                    case 13:
                        tapOffset(true);
                        return;
                    case 14:
                        tapProlong(true);
                        return;
                    case 15:
                        tapProlong(false);
                        return;
                }
            case 4: // pID = 4
                switch (currentStage) {
                    case 0:
                        tapOffset(false);
                        return;
                    case 1:
                        tapOffset(true);
                        return;
                    case 2:
                        if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
                        toastMsg("No intervention");
                        return;
                    case 3:
                        tapProlong(true);
                        return;
                    case 4:
                        tapProlong(false);
                        return;
                    case 5:
                        tapDelay(false);
                        return;
                    case 6:
                        tapDelay(true);
                        return;
                    case 7:
                        tapMultiple();
                        return;
                    case 8:
                        swipeDirection();
                        return;
                    case 9:
                        swipeDelay(false);
                        return;
                    case 10:
                        swipeDelay(true);
                        return;
                    case 11:
                        swipeFingers(true);
                        return;
                    case 12:
                        swipeFingers(false);
                        return;
                    case 13:
                        swipeRatio(false);
                        return;
                    case 14:
                        swipeRatio(true);
                        return;
                    case 15:
                        if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
                        toastMsg("No intervention");
                        return;
                }
            case 5: // pID = 5
                switch (currentStage) {
                    case 0:
                        swipeFingers(false);
                        return;
                    case 1:
                        swipeFingers(true);
                        return;
                    case 2:
                        if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
                        toastMsg("No intervention");
                        return;
                    case 3:
                        swipeDirection();
                        return;
                    case 4:
                        swipeRatio(true);
                        return;
                    case 5:
                        swipeRatio(false);
                        return;
                    case 6:
                        swipeDelay(true);
                        return;
                    case 7:
                        swipeDelay(false);
                        return;
                    case 8:
                        tapProlong(true);
                        return;
                    case 9:
                        tapProlong(false);
                        return;
                    case 10:
                        tapMultiple();
                        return;
                    case 11:
                        tapOffset(true);
                        return;
                    case 12:
                        tapOffset(false);
                        return;
                    case 13:
                        tapDelay(true);
                        return;
                    case 14:
                        tapDelay(false);
                        return;
                    case 15:
                        if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
                        toastMsg("No intervention");
                        return;
                }
            case 6: // pID = 6
                switch (currentStage) {
                    case 0:
                        tapDelay(true);
                        return;
                    case 1:
                        tapDelay(false);
                        return;
                    case 2:
                        tapMultiple();
                        return;
                    case 3:
                        if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
                        toastMsg("No intervention");
                        return;
                    case 4:
                        tapProlong(false);
                        return;
                    case 5:
                        tapProlong(true);
                        return;
                    case 6:
                        tapOffset(false);
                        return;
                    case 7:
                        tapOffset(true);
                        return;
                    case 8:
                        swipeRatio(true);
                        return;
                    case 9:
                        swipeRatio(false);
                        return;
                    case 10:
                        if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
                        toastMsg("No intervention");
                        return;
                    case 11:
                        swipeDelay(false);
                        return;
                    case 12:
                        swipeDelay(true);
                        return;
                    case 13:
                        swipeFingers(false);
                        return;
                    case 14:
                        swipeFingers(true);
                        return;
                    case 15:
                        swipeDirection();
                        return;
                }
            case 7: // pID = 7
                switch (currentStage) {
                    case 0:
                        swipeDelay(true);
                        return;
                    case 1:
                        swipeDelay(false);
                        return;
                    case 2:
                        swipeDirection();
                        return;
                    case 3:
                        swipeRatio(true);
                        return;
                    case 4:
                        swipeRatio(false);
                        return;
                    case 5:
                        swipeFingers(true);
                        return;
                    case 6:
                        swipeFingers(false);
                        return;
                    case 7:
                        if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
                        toastMsg("No intervention");
                        return;
                    case 8:
                        if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
                        toastMsg("No intervention");
                        return;
                    case 9:
                        tapOffset(true);
                        return;
                    case 10:
                        tapOffset(false);
                        return;
                    case 11:
                        tapDelay(true);
                        return;
                    case 12:
                        tapDelay(false);
                        return;
                    case 13:
                        tapProlong(false);
                        return;
                    case 14:
                        tapProlong(true);
                        return;
                    case 15:
                        tapMultiple();
                        return;
                }
            case 8: // pID = 8
                switch (currentStage) {
                    case 0:
                        tapProlong(false);
                        return;
                    case 1:
                        tapProlong(true);
                        return;
                    case 2:
                        tapOffset(false);
                        return;
                    case 3:
                        tapOffset(true);
                        return;
                    case 4:
                        tapMultiple();
                        return;
                    case 5:
                        if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
                        toastMsg("No intervention");
                        return;
                    case 6:
                        tapDelay(false);
                        return;
                    case 7:
                        tapDelay(true);
                        return;
                    case 8:
                        swipeFingers(false);
                        return;
                    case 9:
                        swipeFingers(true);
                        return;
                    case 10:
                        swipeDirection();
                        return;
                    case 11:
                        if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
                        return;
                    case 12:
                        swipeDelay(false);
                        return;
                    case 13:
                        swipeDelay(true);
                        return;
                    case 14:
                        swipeRatio(false);
                        return;
                    case 15:
                        swipeRatio(true);
                        return;
                }
            case 9: // pID = 9
                switch (currentStage) {
                    case 0:
                        if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
                        toastMsg("No intervention");
                        return;
                    case 1:
                        swipeRatio(true);
                        return;
                    case 2:
                        swipeRatio(false);
                        return;
                    case 3:
                        swipeFingers(true);
                        return;
                    case 4:
                        swipeFingers(false);
                        return;
                    case 5:
                        swipeDelay(false);
                        return;
                    case 6:
                        swipeDelay(true);
                        return;
                    case 7:
                        swipeDirection();
                        return;
                    case 8:
                        tapMultiple();
                        return;
                    case 9:
                        tapDelay(true);
                        return;
                    case 10:
                        tapDelay(false);
                        return;
                    case 11:
                        tapProlong(true);
                        return;
                    case 12:
                        tapProlong(false);
                        return;
                    case 13:
                        if (CoreService.isOverlayOn) CoreService.coreService.closeOverlayWindow();
                        toastMsg("No intervention");
                        return;
                    case 14:
                        tapOffset(true);
                        return;
                    case 15:
                        tapOffset(false);
                        return;
                }
        }
    }

    private void resetIntervention() {
        CoreService.tapDelay = 0;
        CoreService.xOffset = 0;
        CoreService.yOffset = 0;
        CoreService.isDoubleTapToSingleTap = false;
        GestureDetector.TAP_THRESHOLD = 0;
        GestureDetector.LONG_PRESS_PROLONG = 0;
        CoreService.swipeDelay = 0;
        CoreService.swipeFingers = 1;
        CoreService.scrollRatio = 1;
        CoreService.reverseDirection = false;
    }

    private void tapDelay(boolean isStrong) {
        if (isStrong) {
            // 1000 ms delay
            CoreService.tapDelay = 800;
            toastMsg("Delay 1000ms");
        } else {
            // 500 ms delay
            CoreService.tapDelay = 300;
            toastMsg("Delay 500ms");
        }
    }

    private void tapOffset(boolean isStrong) {
        if (isStrong) {
            CoreService.yOffset = -200;
            toastMsg("Tap activation position shifts up 200dp");
        } else {
            CoreService.yOffset = -100;
            toastMsg("Tap activation position shifts up 100dp");
        }
    }

    private void tapMultiple() {
        CoreService.isDoubleTapToSingleTap = true;
        toastMsg("You need to double tap to trigger a single tap");
    }

    private void tapProlong(boolean isStrong) {
        if (isStrong) {
            GestureDetector.TAP_THRESHOLD = 200;
            toastMsg("Tap threshold 200ms");
        } else {
            GestureDetector.TAP_THRESHOLD = 100;
            toastMsg("Tap threshold 100ms");
        }
    }

    private void swipeRatio(boolean isStrong) {
        if (isStrong) {
            CoreService.scrollRatio = 4;
            toastMsg("Scroll speed x0.25");
        } else {
            CoreService.scrollRatio = 2;
            toastMsg("Scroll speed x0.5");
        }
    }

    private void swipeDelay(boolean isStrong) {
        if (isStrong) {
            CoreService.swipeDelay = 800;
            toastMsg("Swipe delay 800 ms");
        } else {
            CoreService.swipeDelay = 300;
            toastMsg("Swipe delay 300 ms");
        }
    }

    private void swipeFingers(boolean isStrong) {
        if (isStrong) {
            CoreService.swipeFingers = 3;
            toastMsg("3 fingers to swipe");
        } else {
            CoreService.swipeFingers = 2;
            toastMsg("2 fingers to swipe");
        }
    }

    private void swipeDirection() {
        CoreService.reverseDirection = true;
        toastMsg("Swipe direction reversed");
    }

    private void toastMsg(String msg) {
//        Toast.makeText(CoreService.coreService, msg, Toast.LENGTH_LONG).show();
        CoreService.coreService.broadcast("Current Intervention", msg, false);
    }


    @Override
    protected void onResume() {
        if (currentStage == TOTAL_STAGES) {
            stageDisplay.setText("All stages completed!");
            startBtn.setVisibility(View.INVISIBLE);
            writeToFile(CoreService.participantFilename, answerString, MODE_APPEND);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Congratulations!");
            builder.setItems(new String[]{"You have completed the experiment."}, null);
            builder.create().show();
        } else if (currentStage != 0 && currentStage != 8) {
            writeToFile(CoreService.participantFilename, answerString, MODE_APPEND);
            stageDisplay.setText(String.format(Locale.ENGLISH, "%d/%d stage(s) completed", currentStage, TOTAL_STAGES));
        } else if (currentStage == 8) {
            writeToFile(CoreService.participantFilename, answerString, MODE_APPEND);
            stageDisplay.setText(String.format(Locale.ENGLISH, "%d/%d stage(s) completed,\nyou can have a 10 min break :)", currentStage, TOTAL_STAGES));
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        CoreService.isInLabMode = false;
        currentStage = 0;
        answerString = "";
        super.onDestroy();
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        if (picker == numberPicker) {
            pID = newVal;
        } else {
            currentStage = newVal;
        }
    }
}