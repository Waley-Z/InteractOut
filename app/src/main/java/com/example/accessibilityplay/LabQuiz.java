package com.example.accessibilityplay;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;
import android.util.ArrayMap;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.Locale;
import java.util.Map;

public class LabQuiz extends AppCompatActivity {
    private String[] answers = new String[]{"", "", "", "", "", "0"};
    private ConstraintLayout question6Layout;
    private TextView question6Text;
    private RadioButton question6Level1, question6Level2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_quiz);
        question6Layout = findViewById(R.id.question6Layout);
        question6Level1 = findViewById(R.id.question6Level1);
        question6Level2 = findViewById(R.id.question6Level2);
        question6Text = findViewById(R.id.question6Text);
        long surveyStartTime = System.currentTimeMillis();
        quizInit();
        Button submitBtn = findViewById(R.id.submitBtn);
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (answers[0].equals("") || answers[1].equals("") || answers[2].equals("") || answers[3].equals("") || answers[4].equals("")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(LabQuiz.this);
                    builder.setTitle("Warning!");
                    builder.setItems(new String[]{"Please complete the survey before proceeding."}, null);
                    builder.create().show();
                } else {
                    // Indicator,current stage, timestamp, ans 1, ans 2, ans 3, ans 4, ans 5, ans 6.
                    LabMode.answerString = String.format(Locale.ENGLISH, "STAGE_END;%d\nANSWERS;%d;%d;%s;%s;%s;%s;%s;%s\n", surveyStartTime, LabMode.currentStage, System.currentTimeMillis(), answers[0], answers[1], answers[2], answers[3], answers[4], answers[5]);
                    LabMode.currentStage += 1;
                    LabMode.isSurveyFinished = true;
                    LabQuiz.this.onBackPressed();
                }
            }
        });
    }


    private void quizInit() {
        // question 1
        RadioGroup q1 = findViewById(R.id.question1Choices);
        Map<Integer, String> choiceMap = new ArrayMap<>();
        choiceMap.put(R.id.stronglyDisagree, "1");
        choiceMap.put(R.id.stronglyDisagree2, "1");
        choiceMap.put(R.id.stronglyDisagree3, "1");
        choiceMap.put(R.id.stronglyDisagree4, "1");
        choiceMap.put(R.id.stronglyDisagree5, "1");
        choiceMap.put(R.id.question6Level1, "1");
        choiceMap.put(R.id.disagree, "2");
        choiceMap.put(R.id.disagree2, "2");
        choiceMap.put(R.id.disagree3, "2");
        choiceMap.put(R.id.disagree4, "2");
        choiceMap.put(R.id.disagree5, "2");
        choiceMap.put(R.id.question6Level2, "2");
        choiceMap.put(R.id.somewhatDisagree, "3");
        choiceMap.put(R.id.somewhatDisagree2, "3");
        choiceMap.put(R.id.somewhatDisagree3, "3");
        choiceMap.put(R.id.somewhatDisagree4, "3");
        choiceMap.put(R.id.somewhatDisagree5, "3");
        choiceMap.put(R.id.neutral, "4");
        choiceMap.put(R.id.neutral2, "4");
        choiceMap.put(R.id.neutral3, "4");
        choiceMap.put(R.id.neutral4, "4");
        choiceMap.put(R.id.neutral5, "4");
        choiceMap.put(R.id.somewhatAgree, "5");
        choiceMap.put(R.id.somewhatAgree2, "5");
        choiceMap.put(R.id.somewhatAgree3, "5");
        choiceMap.put(R.id.somewhatAgree4, "5");
        choiceMap.put(R.id.somewhatAgree5, "5");
        choiceMap.put(R.id.agree, "6");
        choiceMap.put(R.id.agree2, "6");
        choiceMap.put(R.id.agree3, "6");
        choiceMap.put(R.id.agree4, "6");
        choiceMap.put(R.id.agree5, "6");
        choiceMap.put(R.id.stronglyAgree, "7");
        choiceMap.put(R.id.stronglyAgree2, "7");
        choiceMap.put(R.id.stronglyAgree3, "7");
        choiceMap.put(R.id.stronglyAgree4, "7");
        choiceMap.put(R.id.stronglyAgree5, "7");
        q1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                answers[0] = choiceMap.get(checkedId);
            }
        });

        // question 2
        RadioGroup q2 = findViewById(R.id.question2Choices);
        q2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                answers[1] = choiceMap.get(checkedId);
            }
        });

        // question 3
        RadioGroup q3 = findViewById(R.id.question3Choices);
        q3.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                answers[2] = choiceMap.get(checkedId);
            }
        });

        // question 4
        RadioGroup q4 = findViewById(R.id.question4Choices);
        q4.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                answers[3] = choiceMap.get(checkedId);
            }
        });

        // question 5
        RadioGroup q5 = findViewById(R.id.question5Choices);
        q5.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                answers[4] = choiceMap.get(checkedId);
            }
        });

        // question 6
        RadioGroup q6 = findViewById(R.id.question6Choices);
        q6.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                answers[5] = choiceMap.get(checkedId);
            }
        });

        // decide and set question 6
        decideQuestion6(LabMode.pID, LabMode.currentStage);
    }

    private void decideQuestion6(int pID, int currentStage) {
        pID = pID % 10;
        if (pID == 0) {
            switch (currentStage) {
                case 2:
                    setQuestion6(true, "tap delay", 2, "Level 1: delay 500 ms", "Level 2: delay 1000 ms");
                    return;
                case 4:
                    setQuestion6(true, "tap offset", 2, "Level 1: offset 100 dp", "Level 2: offset 200 dp");
                    return;
                case 7:
                    setQuestion6(true, "tap prolong", 2, "Level 1: threshold 100 ms", "Level 2: threshold 200 ms");
                    return;
                case 9:
                    setQuestion6(true, "swipe delay", 2, "Level 1: delay 300 ms", "Level 2: delay 800 ms");
                    return;
                case 11:
                    setQuestion6(true, "swipe ratio", 2, "Level 1: 2 times slower", "Level 2: 4 times slower");
                    return;
                case 15:
                    setQuestion6(true, "swipe multiple fingers", 2, "Level 1: 2 fingers", "Level 2: 3 fingers");
                    return;
                default:
                    setQuestion6(false, null, 0, null, null);
            }
        } else if (pID == 1) {
            switch (currentStage) {
                case 15:
                    setQuestion6(true, "tap delay", 2, "Level 1: delay 500 ms", "Level 2: delay 1000 ms");
                    return;
                case 9:
                    setQuestion6(true, "tap offset", 1, "Level 1: offset 100 dp", "Level 2: offset 200 dp");
                    return;
                case 11:
                    setQuestion6(true, "tap prolong", 1, "Level 1: threshold 100 ms", "Level 2: threshold 200 ms");
                    return;
                case 4:
                    setQuestion6(true, "swipe delay", 1, "Level 1: delay 300 ms", "Level 2: delay 800 ms");
                    return;
                case 7:
                    setQuestion6(true, "swipe ratio", 2, "Level 1: 2 times slower", "Level 2: 4 times slower");
                    return;
                case 2:
                    setQuestion6(true, "swipe multiple fingers", 1, "Level 1: 2 fingers", "Level 2: 3 fingers");
                    return;
                default:
                    setQuestion6(false, null, 0, null, null);
            }
        } else if (pID == 2) {
            switch (currentStage) {
                case 4:
                    setQuestion6(true, "tap delay", 2, "Level 1: delay 500 ms", "Level 2: delay 1000 ms");
                    return;
                case 6:
                    setQuestion6(true, "tap offset", 1, "Level 1: offset 100 dp", "Level 2: offset 200 dp");
                    return;
                case 2:
                    setQuestion6(true, "tap prolong", 2, "Level 1: threshold 100 ms", "Level 2: threshold 200 ms");
                    return;
                case 15:
                    setQuestion6(true, "swipe delay", 1, "Level 1: delay 300 ms", "Level 2: delay 800 ms");
                    return;
                case 12:
                    setQuestion6(true, "swipe ratio", 2, "Level 1: 2 times slower", "Level 2: 4 times slower");
                    return;
                case 10:
                    setQuestion6(true, "swipe multiple fingers", 1, "Level 1: 2 fingers", "Level 2: 3 fingers");
                    return;
                default:
                    setQuestion6(false, null, 0, null, null);
            }
        } else if (pID == 3) {
            switch (currentStage) {
                case 9:
                    setQuestion6(true, "tap delay", 1, "Level 1: delay 500 ms", "Level 2: delay 1000 ms");
                    return;
                case 13:
                    setQuestion6(true, "tap offset", 2, "Level 1: offset 100 dp", "Level 2: offset 200 dp");
                    return;
                case 15:
                    setQuestion6(true, "tap prolong", 1, "Level 1: threshold 100 ms", "Level 2: threshold 200 ms");
                    return;
                case 3:
                    setQuestion6(true, "swipe delay", 1, "Level 1: delay 300 ms", "Level 2: delay 800 ms");
                    return;
                case 1:
                    setQuestion6(true, "swipe ratio", 2, "Level 1: 2 times slower", "Level 2: 4 times slower");
                    return;
                case 7:
                    setQuestion6(true, "swipe multiple fingers", 1, "Level 1: 2 fingers", "Level 2: 3 fingers");
                    return;
                default:
                    setQuestion6(false, null, 0, null, null);
            }
        } else if (pID == 4) {
            switch (currentStage) {
                case 6:
                    setQuestion6(true, "tap delay", 2, "Level 1: delay 500 ms", "Level 2: delay 1000 ms");
                    return;
                case 1:
                    setQuestion6(true, "tap offset", 2, "Level 1: offset 100 dp", "Level 2: offset 200 dp");
                    return;
                case 4:
                    setQuestion6(true, "tap prolong", 1, "Level 1: threshold 100 ms", "Level 2: threshold 200 ms");
                    return;
                case 10:
                    setQuestion6(true, "swipe delay", 2, "Level 1: delay 300 ms", "Level 2: delay 800 ms");
                    return;
                case 14:
                    setQuestion6(true, "swipe ratio", 2, "Level 1: 2 times slower", "Level 2: 4 times slower");
                    return;
                case 12:
                    setQuestion6(true, "swipe multiple fingers", 1, "Level 1: 2 fingers", "Level 2: 3 fingers");
                    return;
                default:
                    setQuestion6(false, null, 0, null, null);
            }
        } else if (pID == 5) {
            switch (currentStage) {
                case 14:
                    setQuestion6(true, "tap delay", 1, "Level 1: delay 500 ms", "Level 2: delay 1000 ms");
                    return;
                case 12:
                    setQuestion6(true, "tap offset", 1, "Level 1: offset 100 dp", "Level 2: offset 200 dp");
                    return;
                case 9:
                    setQuestion6(true, "tap prolong", 1, "Level 1: threshold 100 ms", "Level 2: threshold 200 ms");
                    return;
                case 7:
                    setQuestion6(true, "swipe delay", 1, "Level 1: delay 300 ms", "Level 2: delay 800 ms");
                    return;
                case 5:
                    setQuestion6(true, "swipe ratio", 1, "Level 1: 2 times slower", "Level 2: 4 times slower");
                    return;
                case 1:
                    setQuestion6(true, "swipe multiple fingers", 2, "Level 1: 2 fingers", "Level 2: 3 fingers");
                    return;
                default:
                    setQuestion6(false, null, 0, null, null);
            }
        } else if (pID == 6) {
            switch (currentStage) {
                case 1:
                    setQuestion6(true, "tap delay", 1, "Level 1: delay 500 ms", "Level 2: delay 1000 ms");
                    return;
                case 7:
                    setQuestion6(true, "tap offset", 2, "Level 1: offset 100 dp", "Level 2: offset 200 dp");
                    return;
                case 5:
                    setQuestion6(true, "tap prolong", 2, "Level 1: threshold 100 ms", "Level 2: threshold 200 ms");
                    return;
                case 12:
                    setQuestion6(true, "swipe delay", 2, "Level 1: delay 300 ms", "Level 2: delay 800 ms");
                    return;
                case 9:
                    setQuestion6(true, "swipe ratio", 1, "Level 1: 2 times slower", "Level 2: 4 times slower");
                    return;
                case 14:
                    setQuestion6(true, "swipe multiple fingers", 2, "Level 1: 2 fingers", "Level 2: 3 fingers");
                    return;
                default:
                    setQuestion6(false, null, 0, null, null);
            }
        } else if (pID == 7) {
            switch (currentStage) {
                case 12:
                    setQuestion6(true, "tap delay", 1, "Level 1: delay 500 ms", "Level 2: delay 1000 ms");
                    return;
                case 10:
                    setQuestion6(true, "tap offset", 1, "Level 1: offset 100 dp", "Level 2: offset 200 dp");
                    return;
                case 14:
                    setQuestion6(true, "tap prolong", 2, "Level 1: threshold 100 ms", "Level 2: threshold 200 ms");
                    return;
                case 1:
                    setQuestion6(true, "swipe delay", 1, "Level 1: delay 300 ms", "Level 2: delay 800 ms");
                    return;
                case 4:
                    setQuestion6(true, "swipe ratio", 1, "Level 1: 2 times slower", "Level 2: 4 times slower");
                    return;
                case 6:
                    setQuestion6(true, "swipe multiple fingers", 1, "Level 1: 2 fingers", "Level 2: 3 fingers");
                    return;
                default:
                    setQuestion6(false, null, 0, null, null);
            }
        } else if (pID == 8) {
            switch (currentStage) {
                case 7:
                    setQuestion6(true, "tap delay", 2, "Level 1: delay 500 ms", "Level 2: delay 1000 ms");
                    return;
                case 3:
                    setQuestion6(true, "tap offset", 2, "Level 1: offset 100 dp", "Level 2: offset 200 dp");
                    return;
                case 1:
                    setQuestion6(true, "tap prolong", 2, "Level 1: threshold 100 ms", "Level 2: threshold 200 ms");
                    return;
                case 13:
                    setQuestion6(true, "swipe delay", 2, "Level 1: delay 300 ms", "Level 2: delay 800 ms");
                    return;
                case 15:
                    setQuestion6(true, "swipe ratio", 2, "Level 1: 2 times slower", "Level 2: 4 times slower");
                    return;
                case 9:
                    setQuestion6(true, "swipe multiple fingers", 2, "Level 1: 2 fingers", "Level 2: 3 fingers");
                    return;
                default:
                    setQuestion6(false, null, 0, null, null);
            }
        } else if (pID == 9) {
            switch (currentStage) {
                case 10:
                    setQuestion6(true, "tap delay", 1, "Level 1: delay 500 ms", "Level 2: delay 1000 ms");
                    return;
                case 15:
                    setQuestion6(true, "tap offset", 1, "Level 1: offset 100 dp", "Level 2: offset 200 dp");
                    return;
                case 12:
                    setQuestion6(true, "tap prolong", 1, "Level 1: threshold 100 ms", "Level 2: threshold 200 ms");
                    return;
                case 6:
                    setQuestion6(true, "swipe delay", 2, "Level 1: delay 300 ms", "Level 2: delay 800 ms");
                    return;
                case 2:
                    setQuestion6(true, "swipe ratio", 1, "Level 1: 2 times slower", "Level 2: 4 times slower");
                    return;
                case 4:
                    setQuestion6(true, "swipe multiple fingers", 1, "Level 1: 2 fingers", "Level 2: 3 fingers");
                    return;
                default:
                    setQuestion6(false, null, 0, null, null);
            }
        }
    }

    private void setQuestion6(boolean isVisible, String interventionName, int level, String level1Description, String level2Description) {
        if (isVisible) {
            question6Layout.setVisibility(View.VISIBLE);
            question6Text.setText(getResources().getString(R.string.question_6, interventionName));
            question6Level1.setText("Level A");
            question6Level2.setText("Level B");
        } else {
            question6Layout.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LabMode.answerString = "";
    }
}