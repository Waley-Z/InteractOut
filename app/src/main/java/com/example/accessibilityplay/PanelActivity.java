package com.example.accessibilityplay;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class PanelActivity extends AppCompatActivity implements NumberPicker.OnValueChangeListener, NumberPicker.OnScrollListener, NumberPicker.Formatter {

    private static final String TAG = "Main Panel";
    private static long CHECKING_PERIOD = 1000; // 1 s
    private TextView declaredTimeDisplay;
    private NumberPicker hourPicker, minutePicker, secondPicker, strengthPicker;
    private View.OnClickListener launch, appChoices;
    private AlertDialog alertDialog;
    private CountDownTimer countDownTimer;
    private boolean isLaunched = false;
//    private Handler repeatedCheckHandler = new Handler();
//    private Runnable repeatedCheck = new Runnable() {
//        @Override
//        public void run() {
//
//            repeatedCheckHandler.postDelayed(repeatedCheck, CHECKING_PERIOD);
//        }
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panel);

        ContentResolver contentResolver = getContentResolver();
        Log.d(TAG, "onCreate: accessibility status is " + Settings.Secure.getString(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED));
        String status = Settings.Secure.getString(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED);
        if (status.equals("0")) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }

        declaredTimeDisplay = findViewById(R.id.declaredTimeDisplay);
        hourPicker = (NumberPicker) findViewById(R.id.hourPicker);
        minutePicker = (NumberPicker) findViewById(R.id.minutePicker);
        secondPicker = (NumberPicker) findViewById(R.id.secondPicker);
        strengthPicker = (NumberPicker) findViewById(R.id.strengthPicker);
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

        String[] strengthOptions = {"Slight", "Medium", "Salient"};
        strengthPicker.setDisplayedValues(strengthOptions);
        strengthPicker.setMinValue(0);
        strengthPicker.setMaxValue(2);
        strengthPicker.setValue(1);
        strengthPicker.setOnValueChangedListener(this);

        Button startApp = findViewById(R.id.startApp);
        startApp.setText("Choose Apps");
        launch = v -> {
            if (MyAccessibilityService.appChosen.size() == 0) {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PanelActivity.this);
                alertBuilder.setTitle("Warning");
                alertBuilder.setItems(new String[]{"Please choose at least one app to continue!"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.dismiss();
                    }
                });
                alertDialog = alertBuilder.create();
                alertDialog.show();
                startApp.setText("Choose Apps");
                startApp.setOnClickListener(appChoices);
                return;
            } else if (isLaunched) {
                Toast.makeText(PanelActivity.this, "Already launched!", Toast.LENGTH_SHORT).show();
            }
            launchOverlayDelayed();
        };
        appChoices = new View.OnClickListener() {
            private Vector<String> app = new Vector<>(), packages = new Vector<>();
            @Override
            public void onClick(View v) {
                if (MyAccessibilityService.packageNameMap.size() == 0) {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PanelActivity.this);
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
                } else {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PanelActivity.this);
                    alertBuilder.setTitle("Choose disruptive apps:");
                    // TODO: specify checkedItems
                    alertBuilder.setMultiChoiceItems(MyAccessibilityService.packageNameMap.values().toArray(new String[0]), null, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            if (isChecked) {
                                app.add(MyAccessibilityService.packageNameMap.valueAt(which));
                                packages.add(MyAccessibilityService.packageNameMap.keyAt(which));
                            } else {
                                app.remove(MyAccessibilityService.packageNameMap.valueAt(which));
                                packages.remove(MyAccessibilityService.packageNameMap.keyAt(which));
                            }
                            Log.d(TAG, "onClick: " + which + " is clicked");
                        }
                    });
                    alertBuilder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
//                            Toast.makeText(PanelActivity.this, "App choices confirmed " + which, Toast.LENGTH_SHORT).show();
                            startApp.setText("Launch");
                            startApp.setOnClickListener(launch);
                            MyAccessibilityService.appChosen = app;
                            MyAccessibilityService.packageChosen = packages;
                            alertDialog.dismiss();
                        }
                    });
                    alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            alertDialog.cancel();
                        }
                    });
                    alertDialog = alertBuilder.create();
                    alertDialog.show();
                }
            }
        };
        startApp.setOnClickListener(appChoices);
    }

    private void countdownText() {
        declaredTimeDisplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PanelActivity.this);
                alertBuilder.setTitle("Apps to get away");
                alertBuilder.setItems(MyAccessibilityService.appChosen.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.dismiss();
                    }
                });
                alertDialog = alertBuilder.create();
                alertDialog.show();
            }
        });
//        countDownTimer = new CountDownTimer(MyAccessibilityService.timeBeforeOverlaid * 1000L, 1000) {
//            @Override
//            public void onTick(long millisUntilFinished) {
//                millisUntilFinished /= 1000;
//                long hour = millisUntilFinished / 3600;
//                long minute = (millisUntilFinished - hour * 3600) / 60;
//                long second = millisUntilFinished- hour * 3600 - minute * 60;
//                String hours = (hour == 1) ? "hour" : "hours";
//                String minutes = (minute == 1) ? "minute" : "minutes";
//                String seconds = (second == 1) ? "second" : "seconds";
//                declaredTimeDisplay.setText(String.format(Locale.ENGLISH, "%d %s, %d %s and %d %s left. Tap to see the apps you have chosen.", hour, hours, minute, minutes, second, seconds));
//            }
//
//            @Override
//            public void onFinish() {
//                declaredTimeDisplay.setText("Overlay launched.");
//            }
//        };
//        countDownTimer.start();
        Toast.makeText(this, "timer activated", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (isLaunched)  {
            MyAccessibilityService.myAccessibilityService.disableSelf();
//            repeatedCheckHandler.removeCallbacks(repeatedCheck);
        }
        super.onDestroy();
    }

    public void settingButtonOnClick(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void labModeButtonOnClick(View v) {
        Intent intent = new Intent(this, LabMode.class);
        startActivity(intent);
    }

    private void launchOverlayDelayed() {
//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                MyAccessibilityService.myAccessibilityService.launchOverlayWindow();
//            }
//        }, MyAccessibilityService.timeBeforeOverlaid * 1000L);
//        repeatedCheck.run();
//        countdownText();
        Toast.makeText(this, "Start!", Toast.LENGTH_SHORT).show();
        isLaunched = true;
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
        if (picker == strengthPicker) {
            MyAccessibilityService.strength = newVal;
        } else {
            int hour = hourPicker.getValue();
            int minute = minutePicker.getValue();
            int second = secondPicker.getValue();
            MyAccessibilityService.timeBeforeOverlaid = (hour * 3600L + minute * 60L + second) * 1000;
            String hours = (hour == 1) ? "hour" : "hours";
            String minutes = (minute == 1) ? "minute" : "minutes";
            String seconds = (second == 1) ? "second" : "seconds";
            declaredTimeDisplay.setText(String.format(Locale.ENGLISH, "You want to interact with the nice real world after\n\t%d %s, %d %s and %d %s.", hour, hours, minute, minutes, second, seconds));
        }
    }
}