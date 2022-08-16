package com.example.accessibilityplay;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.Vector;

public class PanelActivity extends AppCompatActivity implements NumberPicker.OnValueChangeListener, NumberPicker.OnScrollListener, NumberPicker.Formatter {

    private static final String TAG = "Main Panel";
    private static long CHECKING_PERIOD = 1000; // 1 s
    private TextView declaredTimeDisplay;
    private NumberPicker hourPicker, minutePicker, secondPicker, strengthPicker;
    private View.OnClickListener launch, appChoices, stop;
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
            if (CoreService.appChosen.size() == 0) {
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
            startApp.setText("Stop");
            startApp.setOnClickListener(stop);
            launchOverlayDelayed();
        };
        appChoices = new View.OnClickListener() {
            private Vector<String> app = new Vector<>(), packages = new Vector<>();
            @Override
            public void onClick(View v) {
                if (CoreService.packageNameMap.size() == 0) {
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
                    boolean[] arr = new boolean[CoreService.appListDisplay.size()];
                    for (int i = 0; i < arr.length; i++) {
                        arr[i] = CoreService.appListDisplay.get(i);
                    }
                    alertBuilder.setMultiChoiceItems(CoreService.packageNameMap.values().toArray(new String[0]), arr, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            if (isChecked) {
                                app.add(CoreService.packageNameMap.valueAt(which));
                                packages.add(CoreService.packageNameMap.keyAt(which));
                                CoreService.appListDisplay.set(which, true);
                            } else {
                                app.remove(CoreService.packageNameMap.valueAt(which));
                                packages.remove(CoreService.packageNameMap.keyAt(which));
                                CoreService.appListDisplay.set(which, false);
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
                            allowTapToSeeApps();
                            CoreService.appChosen = app;
                            CoreService.packageChosen = packages;
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
        stop = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLaunched = false;
                CoreService.coreService.closeOverlayWindow();
                CoreService.appChosen.removeAllElements();
                CoreService.packageChosen.removeAllElements();
                startApp.setText("Choose Apps");
                startApp.setOnClickListener(appChoices);
            }
        };
        startApp.setOnClickListener(appChoices);
        createNotificationChannel();
    }

    private void allowTapToSeeApps() {
        declaredTimeDisplay.setText("Tap to see the apps you have chosen.");
        declaredTimeDisplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PanelActivity.this);
                alertBuilder.setTitle("Apps to get away");
                alertBuilder.setItems(CoreService.appChosen.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.dismiss();
                    }
                });
                alertDialog = alertBuilder.create();
                alertDialog.show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (isLaunched)  {
            CoreService.coreService.disableSelf();
//            repeatedCheckHandler.removeCallbacks(repeatedCheck);
            isLaunched = false;
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

        Toast.makeText(this, "Start!", Toast.LENGTH_SHORT).show();
        isLaunched = true;
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        CharSequence name = "Channel Name";
        String description = "This my channel";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel("lalala", name, importance);
        channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
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
            CoreService.strength = newVal;
        } else {
            int hour = hourPicker.getValue();
            int minute = minutePicker.getValue();
            int second = secondPicker.getValue();
            CoreService.timeBeforeOverlaid = (hour * 3600L + minute * 60L + second) * 1000;
            String hours = (hour == 1) ? "hour" : "hours";
            String minutes = (minute == 1) ? "minute" : "minutes";
            String seconds = (second == 1) ? "second" : "seconds";
            declaredTimeDisplay.setText(String.format(Locale.ENGLISH, "You want to interact with the nice real world after\n\t%d %s, %d %s and %d %s.", hour, hours, minute, minutes, second, seconds));
        }
    }
}