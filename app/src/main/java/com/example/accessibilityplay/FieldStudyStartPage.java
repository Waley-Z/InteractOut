package com.example.accessibilityplay;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

public class FieldStudyStartPage extends AppCompatActivity {
    private AlertDialog alertDialog;
    private String TAG = "FieldStudyStartPage";
    private LinearLayout appLinearLayout;

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
        appLinearLayout = findViewById(R.id.appOptionLinearLayout);

        appLinearLayout.setGravity(Gravity.CENTER);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (CoreService.packageNameMap.size() == 0) {
            promptAccessibilitySettingNote();
        } else if (!CoreService.itemAdded) {
            appLinearLayout.removeAllViews();
            for (int i = 0; i < CoreService.itemIdArrays.size(); i++) {
                addItem(i);
            }
            CoreService.itemAdded = true;
        }
    }

    private void addItem(int idx) {
        RelativeLayout.LayoutParams btnParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        btnParams.addRule(RelativeLayout.CENTER_VERTICAL);
        btnParams.rightMargin = 100;
        RelativeLayout.LayoutParams txtParams = new RelativeLayout.LayoutParams(400, ViewGroup.LayoutParams.WRAP_CONTENT);
        txtParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        txtParams.addRule(RelativeLayout.CENTER_VERTICAL);
        txtParams.leftMargin = 100;
        TextView txt = new TextView(this);
        txt.setTextSize(18);
        txt.setText(CoreService.appChosen.get(idx));
        txt.setId(CoreService.itemIdArrays.get(idx).textId);
        RelativeLayout relativeLayout = new RelativeLayout(appLinearLayout.getContext());
        LinearLayout btnLayout = new LinearLayout(relativeLayout.getContext());
        LinearLayout.LayoutParams btnParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParam.setMargins(0, 10, 0, 10);
        btnLayout.setOrientation(LinearLayout.VERTICAL);

        TextView timePermitted = new TextView(this);
        timePermitted.setId(CoreService.itemIdArrays.get(idx).timePermitted);
        timePermitted.setText("Permission Granted");
        timePermitted.setTextSize(18);

        Button btn1 = new Button(this);
        btn1.setLayoutParams(btnParam);
        btn1.setText("Pause for 15 min");
        btn1.setId(CoreService.itemIdArrays.get(idx).pause15);
        btn1.setBackgroundResource(R.drawable.shape_all_round);
        btn1.setTextColor(Color.parseColor("#007aff"));
        btn1.setPadding(20, 0, 20, 0);

        Button btn2 = new Button(this);
        btn2.setPadding(20, 0, 20, 0);
        btn2.setText("Pause for Today");
        btn2.setId(CoreService.itemIdArrays.get(idx).pauseToday);
        btn2.setLayoutParams(btnParam);
        btn2.setBackgroundResource(R.drawable.shape_all_round);

        btnLayout.addView(timePermitted);
        btnLayout.addView(btn1);
        btnLayout.addView(btn2);
        if (CoreService.appGrantedTime.get(CoreService.packageChosen.get(idx)) == 0) {
            Log.d(TAG, "addItem: " + CoreService.packageChosen.get(idx));
            timePermitted.setVisibility(View.GONE);
        } else {
            btn1.setVisibility(View.GONE);
            btn2.setVisibility(View.GONE);
        }
        relativeLayout.addView(txt, txtParams);
        relativeLayout.addView(btnLayout, btnParams);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 20, 0, 20);
        relativeLayout.setLayoutParams(layoutParams);
        appLinearLayout.addView(relativeLayout);
        btn1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String content = String.format(Locale.ENGLISH, "15_EXTRA_MINUTES:%s", CoreService.appChosen.get(idx));
                CoreService.coreService.writeToFile(content);
                btn1.setVisibility(View.GONE);
                btn2.setVisibility(View.GONE);
                timePermitted.setVisibility(View.VISIBLE);
                CoreService.appGrantedTime.put(CoreService.packageChosen.get(idx), 15*60000L);
                CoreService.itemIdArrays.get(idx).countDownTimer = new CountDownTimer(15*60000, 1000) {
                    private Button btn1 = appLinearLayout.findViewById(CoreService.itemIdArrays.get(idx).pause15);
                    private Button btn2 = appLinearLayout.findViewById(CoreService.itemIdArrays.get(idx).pauseToday);
                    private TextView timePermitted = appLinearLayout.findViewById(CoreService.itemIdArrays.get(idx).timePermitted);
                    @Override
                    public void onTick(long millisUntilFinished) {
                        btn1 = appLinearLayout.findViewById(CoreService.itemIdArrays.get(idx).pause15);
                        btn2 = appLinearLayout.findViewById(CoreService.itemIdArrays.get(idx).pauseToday);
                        timePermitted = appLinearLayout.findViewById(CoreService.itemIdArrays.get(idx).timePermitted);
//                        long time = millisUntilFinished;
//                        long hour = time / 3600000;
//                        time -= hour * 3600000;
//                        long minute = time / 60000;
//                        time -= minute * 60000;
//                        long second = time / 1000;
//                        timePermitted.setText(String.format(Locale.ENGLISH, "%d:%d:%d", hour, minute, second));
                        Log.d(TAG, "onTick: " + CoreService.appChosen.get(idx) + ' ' + millisUntilFinished / 1000 + 's');
                    }

                    @Override
                    public void onFinish() {
                        btn1.setVisibility(View.VISIBLE);
                        btn2.setVisibility(View.VISIBLE);
                        timePermitted.setVisibility(View.GONE);
                        CoreService.appGrantedTime.put(CoreService.packageChosen.get(idx), 0L);
                    }
                }.start();
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            private Button btn1 = appLinearLayout.findViewById(CoreService.itemIdArrays.get(idx).pause15);
            private Button btn2 = appLinearLayout.findViewById(CoreService.itemIdArrays.get(idx).pauseToday);
            private TextView timePermitted = appLinearLayout.findViewById(CoreService.itemIdArrays.get(idx).timePermitted);
            @Override
            public void onClick(View v) {
                String content = String.format(Locale.ENGLISH, "PAUSE_TODAY:%s", CoreService.appChosen.get(idx));
                CoreService.coreService.writeToFile(content);
                btn1.setVisibility(View.GONE);
                btn2.setVisibility(View.GONE);
                timePermitted.setText("Paused for Today");
                timePermitted.setVisibility(View.VISIBLE);
                CoreService.appGrantedTime.put(CoreService.packageChosen.get(idx), 86400000L);
                CoreService.itemIdArrays.get(idx).countDownTimer = new CountDownTimer(86400000L, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        Log.d(TAG, "onTick: " + CoreService.appChosen.get(idx) + ' ' + millisUntilFinished / 1000 + 's');
                    }

                    @Override
                    public void onFinish() {
                        btn1.setVisibility(View.VISIBLE);
                        btn2.setVisibility(View.VISIBLE);
                        timePermitted.setVisibility(View.GONE);
                        CoreService.appGrantedTime.put(CoreService.packageChosen.get(idx), 0L);
                    }
                }.start();
            }
        });
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CoreService.itemAdded = false;
    }
}