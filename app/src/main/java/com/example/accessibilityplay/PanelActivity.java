package com.example.accessibilityplay;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;

import java.util.Locale;
import java.util.Vector;

public class PanelActivity extends AppCompatActivity  {

    public static Vector<String> packageToSetTime = new Vector<>();

    private static final String TAG = "Main Panel";
    private static long CHECKING_PERIOD = 1000; // 1 s
    private TextView declaredTimeDisplay;

    private View.OnClickListener appChoices, stop;
    private AlertDialog alertDialog;
    private boolean isLaunched = false;
    private Chip chipTapDelay, chipProlong, chipDouble, chipOffset;
    private Chip chipSwipeDelay, chipRatio, chipReverse, chipMultiple;
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


//        String[] strengthOptions = {"Slight", "Medium", "Salient"};
//        strengthPicker.setDisplayedValues(strengthOptions);
//        strengthPicker.setMinValue(0);
//        strengthPicker.setMaxValue(2);
//        strengthPicker.setValue(1);
//        strengthPicker.setOnValueChangedListener(this);

        Button startApp = findViewById(R.id.startApp);
        startApp.setText("Choose Apps");
        appChoices = new View.OnClickListener() {
            private Vector<String> app = new Vector<>(), packages = new Vector<>();
            @Override
            public void onClick(View v) {
                if (CoreService.packageNameMap.size() == 0) {
                    // tell user to enable accessibility settings first
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
                }
                else {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PanelActivity.this);
                    alertBuilder.setTitle("Choose disruptive apps:");
                    String[] displayApps = CoreService.packageNameMap.values().toArray(new String[0]);
                    alertBuilder.setMultiChoiceItems(displayApps, null, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            if (isChecked) {
                                app.add(CoreService.packageNameMap.valueAt(which));
                                packages.add(CoreService.packageNameMap.keyAt(which));
                                packageToSetTime.add(CoreService.packageNameMap.keyAt(which));
                            } else {
                                app.remove(CoreService.packageNameMap.valueAt(which));
                                packages.remove(CoreService.packageNameMap.keyAt(which));
                                packageToSetTime.remove(CoreService.packageNameMap.keyAt(which));
                            }
                            Log.d(TAG, "onClick: " + which + " is clicked");
                        }
                    });
                    alertBuilder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
//                            Toast.makeText(PanelActivity.this, "App choices confirmed " + which, Toast.LENGTH_SHORT).show();
                            if (app.size() == 0) {
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
                            allowTapToSeeApps();
                            CoreService.appChosen = app;
                            CoreService.packageChosen = packages;
                            String currentInterventions = CoreService.coreService.getCurrentInterventions();
                            CoreService.coreService.broadcastField("Current Intervention", currentInterventions, 2, true);
                            alertDialog.dismiss();
                            startActivity(new Intent(PanelActivity.this, TimeSetting.class));
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
                CoreService.coreService.resetInterventions();
                CoreService.coreService.closeOverlayWindow();
                CoreService.appChosen.removeAllElements();
                CoreService.packageChosen.removeAllElements();
                while (!CoreService.appUsedTime.isEmpty()) {
                    CoreService.appUsedTime.removeAt(0);
                }
                while (!CoreService.appTargetTime.isEmpty()) {
                    CoreService.appTargetTime.removeAt(0);
                }
                startApp.setText("Choose Apps");
                startApp.setOnClickListener(appChoices);
            }
        };
        startApp.setOnClickListener(appChoices);
        createNotificationChannel();

        // set chips

        TextView noteInformation = findViewById(R.id.noteInformation);
        chipTapDelay = findViewById(R.id.chipTapDelay);
        chipProlong = findViewById(R.id.chipProlong);
        chipDouble = findViewById(R.id.chipDouble);
        chipOffset = findViewById(R.id.chipOffset);
        chipSwipeDelay = findViewById(R.id.chipSwipeDelay);
        chipRatio = findViewById(R.id.chipRatio);
        chipReverse = findViewById(R.id.chipReverse);
        chipMultiple = findViewById(R.id.chipMultiple);

        chipTapDelay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    CoreService.tapDelayMax = 800;
                    noteInformation.setText("By selecting this intervention, you need to wait 1s for every tap to take effect");
                    String content = String.format(Locale.ENGLISH, "INTERVENTION_SELECT;%d;TAP_DELAY\n", System.currentTimeMillis());
                    CoreService.coreService.writeToFile(CoreService.dataFileUri, content);
                } else {
                    CoreService.tapDelayMax = 0;
                    String content = String.format(Locale.ENGLISH, "INTERVENTION_UNSELECT;%d;TAP_DELAY\n", System.currentTimeMillis());
                    CoreService.coreService.writeToFile(CoreService.dataFileUri, content);
                }
                CoreService.usingDefaultIntervention = !( chipTapDelay.isChecked() | chipProlong.isChecked() | chipDouble.isChecked() | chipOffset.isChecked() |
                        chipSwipeDelay.isChecked() | chipRatio.isChecked() | chipReverse.isChecked() | chipMultiple.isChecked());
                String currentIntervention = CoreService.coreService.getCurrentInterventions();
                CoreService.coreService.broadcastField("Current Interventions", currentIntervention, 2, true);
            }
        });

        chipProlong.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    CoreService.prolongMax = 200;
                    noteInformation.setText("By selecting this intervention, you need to hold your finger on the screen for 200ms");
                    String content = String.format(Locale.ENGLISH, "INTERVENTION_SELECT;%d;TAP_PROLONG\n", System.currentTimeMillis());
                    CoreService.coreService.writeToFile(CoreService.dataFileUri, content);
                } else {
                    CoreService.prolongMax = 0;
                    String content = String.format(Locale.ENGLISH, "INTERVENTION_UNSELECT;%d;TAP_PROLONG\n", System.currentTimeMillis());
                    CoreService.coreService.writeToFile(CoreService.dataFileUri, content);
                }
                CoreService.usingDefaultIntervention = !( chipTapDelay.isChecked() | chipProlong.isChecked() | chipDouble.isChecked() | chipOffset.isChecked() |
                        chipSwipeDelay.isChecked() | chipRatio.isChecked() | chipReverse.isChecked() | chipMultiple.isChecked());
                String currentIntervention = CoreService.coreService.getCurrentInterventions();
                CoreService.coreService.broadcastField("Current Interventions", currentIntervention, 2, true);
            }
        });

        chipDouble.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    CoreService.isDoubleTapToSingleTap = true;
                    noteInformation.setText("By selecting this intervention, you need to perform double tap in order to single tap");
                    String content = String.format(Locale.ENGLISH, "INTERVENTION_SELECT;%d;TAP_DOUBLE\n", System.currentTimeMillis());
                    CoreService.coreService.writeToFile(CoreService.dataFileUri, content);
                } else {
                    CoreService.isDoubleTapToSingleTap = false;
                    String content = String.format(Locale.ENGLISH, "INTERVENTION_UNSELECT;%d;TAP_DOUBLE\n", System.currentTimeMillis());
                    CoreService.coreService.writeToFile(CoreService.dataFileUri, content);
                }
                CoreService.usingDefaultIntervention = !( chipTapDelay.isChecked() | chipProlong.isChecked() | chipDouble.isChecked() | chipOffset.isChecked() |
                        chipSwipeDelay.isChecked() | chipRatio.isChecked() | chipReverse.isChecked() | chipMultiple.isChecked());
                String currentIntervention = CoreService.coreService.getCurrentInterventions();
                CoreService.coreService.broadcastField("Current Interventions", currentIntervention, 2, true);
            }
        });

        chipOffset.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    CoreService.yOffset = -200;
                    noteInformation.setText("By selecting this intervention, you need to tap 200dp below the desired position");
                    String content = String.format(Locale.ENGLISH, "INTERVENTION_SELECT;%d;TAP_OFFSET\n", System.currentTimeMillis());
                    CoreService.coreService.writeToFile(CoreService.dataFileUri, content);
                } else {
                    CoreService.yOffset = 0;
                    String content = String.format(Locale.ENGLISH, "INTERVENTION_UNSELECT;%d;TAP_OFFSET\n", System.currentTimeMillis());
                    CoreService.coreService.writeToFile(CoreService.dataFileUri, content);
                }
                CoreService.xOffset = 0;
                CoreService.usingDefaultIntervention = !( chipTapDelay.isChecked() | chipProlong.isChecked() | chipDouble.isChecked() | chipOffset.isChecked() |
                        chipSwipeDelay.isChecked() | chipRatio.isChecked() | chipReverse.isChecked() | chipMultiple.isChecked());
                String currentIntervention = CoreService.coreService.getCurrentInterventions();
                CoreService.coreService.broadcastField("Current Interventions", currentIntervention, 2, true);
            }
        });

        chipSwipeDelay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    CoreService.swipeDelayMax = 800;
                    noteInformation.setText("By selecting this intervention, you need to wait 800ms for every swipe to take effect");
                    String content = String.format(Locale.ENGLISH, "INTERVENTION_SELECT;%d;SWIPE_DELAY\n", System.currentTimeMillis());
                    CoreService.coreService.writeToFile(CoreService.dataFileUri, content);
                } else {
                    CoreService.swipeDelayMax = 0;
                    String content = String.format(Locale.ENGLISH, "INTERVENTION_UNSELECT;%d;SWIPE_DELAY\n", System.currentTimeMillis());
                    CoreService.coreService.writeToFile(CoreService.dataFileUri, content);
                }
                CoreService.usingDefaultIntervention = !( chipTapDelay.isChecked() | chipProlong.isChecked() | chipDouble.isChecked() | chipOffset.isChecked() |
                        chipSwipeDelay.isChecked() | chipRatio.isChecked() | chipReverse.isChecked() | chipMultiple.isChecked());
                String currentIntervention = CoreService.coreService.getCurrentInterventions();
                CoreService.coreService.broadcastField("Current Interventions", currentIntervention, 2, true);
            }
        });

        chipRatio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    CoreService.scrollRatioMax = 4;
                    noteInformation.setText("By selecting this intervention, you need wait for every swipe to be replayed 4 times slower");
                    String content = String.format(Locale.ENGLISH, "INTERVENTION_SELECT;%d;SWIPE_RATIO\n", System.currentTimeMillis());
                    CoreService.coreService.writeToFile(CoreService.dataFileUri, content);
                } else {
                    CoreService.scrollRatioMax = 1;
                    String content = String.format(Locale.ENGLISH, "INTERVENTION_UNSELECT;%d;SWIPE_RATIO\n", System.currentTimeMillis());
                    CoreService.coreService.writeToFile(CoreService.dataFileUri, content);
                }
                CoreService.usingDefaultIntervention = !( chipTapDelay.isChecked() | chipProlong.isChecked() | chipDouble.isChecked() | chipOffset.isChecked() |
                        chipSwipeDelay.isChecked() | chipRatio.isChecked() | chipReverse.isChecked() | chipMultiple.isChecked());
                String currentIntervention = CoreService.coreService.getCurrentInterventions();
                CoreService.coreService.broadcastField("Current Interventions", currentIntervention, 2, true);
            }
        });

        chipReverse.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    CoreService.reverseDirection = true;
                    noteInformation.setText("By selecting this intervention, you need to swipe in the reversed direction");
                    String content = String.format(Locale.ENGLISH, "INTERVENTION_SELECT;%d;SWIPE_REVERSE\n", System.currentTimeMillis());
                    CoreService.coreService.writeToFile(CoreService.dataFileUri, content);
                } else {
                    CoreService.reverseDirection = false;
                    String content = String.format(Locale.ENGLISH, "INTERVENTION_UNSELECT;%d;SWIPE_REVERSE\n", System.currentTimeMillis());
                    CoreService.coreService.writeToFile(CoreService.dataFileUri, content);
                }
                CoreService.usingDefaultIntervention = !( chipTapDelay.isChecked() | chipProlong.isChecked() | chipDouble.isChecked() | chipOffset.isChecked() |
                        chipSwipeDelay.isChecked() | chipRatio.isChecked() | chipReverse.isChecked() | chipMultiple.isChecked());
                String currentIntervention = CoreService.coreService.getCurrentInterventions();
                CoreService.coreService.broadcastField("Current Interventions", currentIntervention, 2, true);
            }
        });

        chipMultiple.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    CoreService.swipeFingers = 2;
                    noteInformation.setText("By selecting this intervention, you need to use 2 fingers to swipe");
                    String content = String.format(Locale.ENGLISH, "INTERVENTION_SELECT;%d;SWIPE_MULTIPLE\n", System.currentTimeMillis());
                    CoreService.coreService.writeToFile(CoreService.dataFileUri, content);
                } else {
                    CoreService.swipeFingers = 1;
                    String content = String.format(Locale.ENGLISH, "INTERVENTION_UNSELECT;%d;SWIPE_MULTIPLE\n", System.currentTimeMillis());
                    CoreService.coreService.writeToFile(CoreService.dataFileUri, content);                }
                CoreService.usingDefaultIntervention = !( chipTapDelay.isChecked() | chipProlong.isChecked() | chipDouble.isChecked() | chipOffset.isChecked() |
                        chipSwipeDelay.isChecked() | chipRatio.isChecked() | chipReverse.isChecked() | chipMultiple.isChecked());
                String currentIntervention = CoreService.coreService.getCurrentInterventions();
                CoreService.coreService.broadcastField("Current Interventions", currentIntervention, 2, true);
            }
        });


    }



    private void allowTapToSeeApps() {
        declaredTimeDisplay.setText("Tap to see the apps you have chosen.");
        declaredTimeDisplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PanelActivity.this);
                alertBuilder.setTitle("Apps to get away");
                Vector<String> appAndTime = new Vector<>();
                CoreService.packageChosen.forEach(s -> {
                    long time = CoreService.appUsedTime.get(s);
                    long hour = time / 3600000;
                    time -= hour * 3600000;
                    long minute = time / 60000;
                    time -= minute * 60000;
                    long second = time / 1000;
                    appAndTime.add(String.format(Locale.ENGLISH, "%s: %d hours, %d minutes, %d seconds", CoreService.packageNameMap.get(s), hour, minute, second));
                });
                alertBuilder.setItems(appAndTime.toArray(new String[0]), new DialogInterface.OnClickListener() {
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
        CharSequence name = "Lab Study";
        String description = "Lab study channel";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel("lab_study", name, importance);
        channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        NotificationChannel field = new NotificationChannel("field_study", "Field Study", importance);
        field.setDescription("Field study channel");
        notificationManager.createNotificationChannel(field);

        NotificationChannel survey = new NotificationChannel("in_study_survey", "In-Study Survey", importance);
        field.setDescription("In-study survey channel");
        notificationManager.createNotificationChannel(survey);
    }
}