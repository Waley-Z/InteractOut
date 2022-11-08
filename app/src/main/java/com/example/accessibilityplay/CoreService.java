package com.example.accessibilityplay;


import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;

public class CoreService extends AccessibilityService {
    public static CoreService coreService;
    public Window window;
    public static boolean reverseDirection = false,
            isInLabMode = false,
            isDisablingWindowAllowed = false,
            isLongpressListenerOn = false,
            isOverlayOn = false,
            inTutorialMainPage = true,
            isInTutorial = false,
            isDoubleTapToSingleTap = false,
            usingDefaultIntervention = true,
            isDefaultInterventionLaunched = false,
            customizeIntervention = false,
            itemAdded = false;
    public static int targetTimeFieldStudy = 60000; // 1 hour
    public static int tapDelay = 0, tapDelayMax = 0;
    public static int swipeDelay = 0, swipeDelayMax = 0;
    public static int minimumFingerToTap = 1;
    // 50 to 100 ms time is good for prolong
    public static int swipeFingers = 1;
    public static int xOffset = 0;
    public static int yOffset = 0;
    public static int prolongMax = 0;
    public static float scrollRatio = 1, scrollRatioMax = 1, scrollRatioExp = 0;
    public static int prolongNoteShowTime = 100;
    public static int screenWidth = 1080, screenHeight = 2400;
    public static String currentForegroundPackage = "", currrentClassName = "";
    public static String participantFilename = "test user";
    public static Vector<String> appChosen = new Vector<>(), packageChosen = new Vector<>(), systemPackages = new Vector<>();
    public static ArrayMap<String, Long> appUsedTime = new ArrayMap<>(), appTargetTime = new ArrayMap<>(), appGrantedTime = new ArrayMap<>();
    public static ArrayMap<String, Integer> tapDelayValues = new ArrayMap<>(), swipeDelayValues = new ArrayMap<>(), tapProlongValues = new ArrayMap<>();
    public static ArrayMap<String, Float> swipeRatioValues = new ArrayMap<>();
    public static ArrayMap<String, String> packageNameMap = new ArrayMap<>(), appInterventionCode = new ArrayMap<>();
    public static Vector<ItemIds> itemIdArrays = new Vector<>();
    public FirebaseFirestore db;

    private final static String TAG = "MyAccessibilityService.java";
    private Calendar beginOfToday;
    private CountDownTimer countDownTimer;
    private boolean isCountdownLaunched = false;
    private final long SATURATION_NUM = 1; // after 10 interactions, call increaseIntensity()
    private int currentStep = 0;
    private PendingIntent inStudySurveyPopUpIntent;
    private Handler checkTimeHandler;
    private Runnable checkTimeRunnable;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {

        Log.d(TAG, "onAccessibilityEvent: \n" + accessibilityEvent);
        if (accessibilityEvent.getPackageName() == null
                || (accessibilityEvent.getPackageName().equals(getPackageName())
                && (accessibilityEvent.getClassName().equals("android.view.ViewGroup")
                || accessibilityEvent.getClassName().equals("android.app.AlertDialog"))))
        {
            Log.d(TAG, "onAccessibilityEvent: Returned");
            Log.d(TAG, "onAccessibilityEvent: isOverlay = " + isOverlayOn);
            return;
        }
        currentForegroundPackage = (String) accessibilityEvent.getPackageName();
        currrentClassName = (String) accessibilityEvent.getClassName();
        if (currentForegroundPackage.equals("com.google.android.inputmethod.latin")) {
            return;
        }
        if (isInLabMode && accessibilityEvent.getClassName().equals(getPackageName() + ".Tutorial")) {
            if (!isOverlayOn && !inTutorialMainPage) {
                    launchOverlayWindow();
            }
            return;
        }
        if (isInLabMode &&
                (currentForegroundPackage.equals("com.twitter.android")
                        || currentForegroundPackage.equals("com.teamlava.bubble"))) {
            // in lab mode the intervention of these two app is control on purpose
            return;
        }
        if (systemPackages.contains(currentForegroundPackage)) {
            if (!currrentClassName.equals("com.android.systemui.volume.VolumeDialogImpl$CustomDialog")
                    && isCountdownLaunched) {
                countDownTimer.cancel();
                isCountdownLaunched = false;
                Toast.makeText(this, "countdown turned off", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        refreshUseTime();
        Log.d(TAG, "onAccessibilityEvent: \n" + appUsedTime + '\n' + appTargetTime);
        boolean isTargetApp = appUsedTime.containsKey(currentForegroundPackage);
        if (isTargetApp) {
            String content = String.format(Locale.ENGLISH, "USAGE;%d;%s;%s;%s\n", System.currentTimeMillis(), appUsedTime, appTargetTime, appGrantedTime);
            writeToFile(content);
            if (!isOverlayOn) {
                long time = 0;
                for (int i = 0; i < appUsedTime.size(); i++) {
                    time += appUsedTime.valueAt(i);
                }
//                long targetTime = appTargetTime.get(currentForegroundPackage) + appGrantedTime.get(currentForegroundPackage);
                if (time > targetTimeFieldStudy) {
                    if (appGrantedTime.get(currentForegroundPackage) == 0L) {
                        lauchInterventionWithCode(currentForegroundPackage);
                    }
//                    if (usingDefaultIntervention) {
//                        launchDefaultIntervention(currentForegroundPackage);
//                    } else {
//                        launchOverlayWindow();
//                    }
                } else if (!isCountdownLaunched && targetTimeFieldStudy > time) {
                    long timeRemain = targetTimeFieldStudy - time;
                    countDownTimer = new CountDownTimer(timeRemain, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            Log.d(TAG, "onTick: " + millisUntilFinished / 1000 + "s remaining");
                        }

                        @Override
                        public void onFinish() {
//                            if (usingDefaultIntervention) {
//                                launchDefaultIntervention(currentForegroundPackage);
//                            } else {
//                                launchOverlayWindow();
//                            }
                            lauchInterventionWithCode(currentForegroundPackage);
                            isCountdownLaunched = false;
                        }
                    }.start();
                    isCountdownLaunched = true;
                    Toast.makeText(this, "countdown turned on", Toast.LENGTH_SHORT).show();
                }
            } else {
                closeOverlayWindow();
                if (appGrantedTime.get(currentForegroundPackage) == 0L) {
                    lauchInterventionWithCode(currentForegroundPackage);
                }
            }
        } else {
            if (isOverlayOn || isLongpressListenerOn) {
                Log.d(TAG, "onAccessibilityEvent: " + isOverlayOn + ' ' + isLongpressListenerOn);
                closeOverlayWindow();
//                currentForegroundPackage = "";
            }
            if (isCountdownLaunched) {
                countDownTimer.cancel();
                isCountdownLaunched = false;
                Toast.makeText(this, "countdown turned off", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void lauchInterventionWithCode(String currentForegroundPackage) {
        String interventionCode = appInterventionCode.get(currentForegroundPackage);
        if (interventionCode.charAt(0) == '1') {
            // default
            launchDefaultIntervention(currentForegroundPackage);
        } else {
            // interactout interventions
            setInterventions(interventionCode.charAt(1), interventionCode.charAt(2));
            launchOverlayWindow();
        }
    }

    private void dailyClean() {
        // clean extra permitted time and reset intervention values
        String content = String.format(Locale.ENGLISH, "NEW_DAY;%d\n", System.currentTimeMillis());
        writeToFile(content);
        resetInterventions();
        syncAppMap();
        for (int i = 0; i < appUsedTime.size(); i++) {
            appUsedTime.setValueAt(i, 0L);
        }
        customizeIntervention = false;
        for (int i = 0; i < appGrantedTime.size(); i++) {
            appGrantedTime.setValueAt(i, 0L);
        }
    }

    public void refreshUseTime() {
        Log.d(TAG, "refreshUseTime: \n" + beginOfToday.get(Calendar.YEAR) + ' ' + beginOfToday.get(Calendar.MONTH) + " " + beginOfToday.get(Calendar.DATE));
        Map<String, UsageStats> stats = getUsageStats(beginOfToday.getTimeInMillis(), System.currentTimeMillis());
        CoreService.packageChosen.forEach((item) -> {
            long usedTime;
            try {
                usedTime = stats.get(item).getTotalTimeInForeground();
            } catch (NullPointerException e) {
                usedTime = 0;
            }
            if (appUsedTime.containsKey(item)) {
                appUsedTime.setValueAt(appUsedTime.indexOfKey(item), usedTime);
            } else {
                appUsedTime.put(item, usedTime);
            }
        });
    }

    public CoreService() {
        super();
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Interrupted");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind: unbind");
        closeOverlayWindow();
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.cancel(inStudySurveyPopUpIntent);
        checkTimeHandler.removeCallbacks(checkTimeRunnable);
        writeToFile("Accessibility stopped");
        return super.onUnbind(intent);
    }

    private void clearAllArrays() {
        packageChosen.removeAllElements();
        appChosen.removeAllElements();
        itemIdArrays.forEach(i -> {
            if (i.countDownTimer != null) {
                i.countDownTimer.onFinish();
                i.countDownTimer.cancel();
            }
        });
        itemIdArrays.removeAllElements();
        appUsedTime.forEach((k, v) -> {
            appUsedTime.remove(k);
        });
        appInterventionCode.forEach((k, v) -> {
            appInterventionCode.remove(k);
        });
    }

    public void syncAppMap() {
        db.collection(participantFilename).document("Settings").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                clearAllArrays();
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    Log.d("firebase", String.valueOf(task.getResult().getData()));
                    task.getResult().getData().forEach((k, v) -> {
                        packageChosen.add(k);
                        appChosen.add(packageNameMap.get(k));
                        appUsedTime.put(k, 0L);
                        appGrantedTime.put(k, 0L);
                        appInterventionCode.put(k, v.toString());
                        itemIdArrays.add(new ItemIds(View.generateViewId(), View.generateViewId(), View.generateViewId(), View.generateViewId()));
                    });
                }
            }
        });
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        window = new Window(this);
        db = FirebaseFirestore.getInstance();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        info.notificationTimeout = 10;
        this.setServiceInfo(info);
        Log.d(TAG, "onServiceConnected: \n" + screenWidth + ' ' + screenHeight);
        coreService = this;
        checkTimeHandler = new Handler();
        checkTimeRunnable = new Runnable() {
            @Override
            public void run() {
                int date = (beginOfToday == null) ? 0 : beginOfToday.get(Calendar.DATE);
                beginOfToday = Calendar.getInstance();
                if (beginOfToday.get(Calendar.MINUTE) == 15) {
                    // for testing
                    writeToFile("15分");
                }
                if (date != 0 && date != beginOfToday.get(Calendar.DATE)) {
                    dailyClean();
                }
                if (isOverlayOn) {
                    stepIncrease();
                }
                beginOfToday.set(Calendar.HOUR, 0);
                beginOfToday.set(Calendar.MINUTE, 0);
                beginOfToday.set(Calendar.SECOND, 0);
                checkTimeHandler.postDelayed(checkTimeRunnable, 60000);
            }
        };
        checkTimeRunnable.run();

        // get all installed apps
        getAllInstalledApps();
        syncAppMap();
        Log.d(TAG, "onServiceConnected: Service connected");
        setInStudySurveyRepeat();
    }

    private void setInStudySurveyRepeat() {
        Intent intent = new Intent(coreService, InStudySurveyActivity.class);
        inStudySurveyPopUpIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 20);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, inStudySurveyPopUpIntent);
    }

    public void resetInterventions() {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.cancel(2);
        notificationManagerCompat.cancel(3);
        CoreService.tapDelay = 0;
        CoreService.isDoubleTapToSingleTap = false;
        CoreService.xOffset = 0;
        CoreService.yOffset = 0;
        GestureDetector.TAP_THRESHOLD = 0;
        GestureDetector.LONG_PRESS_PROLONG = 1000;
        CoreService.swipeDelay = 0;
        CoreService.scrollRatio = 1;
        scrollRatioExp = 0;
        CoreService.swipeFingers = 1;
        CoreService.reverseDirection = false;
    }

    private void resetInterventionLimit() {
        tapDelayMax = 0;
        prolongMax = 0;
        swipeDelayMax = 0;
        scrollRatioMax = 1;
    }

    private void setInterventions(char tapIntervention, char swipeIntervention) {
        resetInterventionLimit();
        resetInterventions();
        switch (tapIntervention) {
            case 'a':
                tapDelayMax = 800;
                try {
                    tapDelay = tapDelayValues.get(currentForegroundPackage);
                } catch (NullPointerException e) {
                    tapDelay = 0;
                }
                break;
            case 'b':
                prolongMax = 200;
                try {
                    GestureDetector.TAP_THRESHOLD = tapProlongValues.get(currentForegroundPackage);
                } catch (NullPointerException e) {
                    GestureDetector.TAP_THRESHOLD = 0;
                }
                break;
            case 'c':
                isDoubleTapToSingleTap = true;
                break;
            case 'd':
                yOffset = -200;
                break;
        }

        switch (swipeIntervention) {
            case 'a':
                swipeDelayMax = 800;
                try {
                    swipeDelay = swipeDelayValues.get(currentForegroundPackage);
                } catch (NullPointerException e) {
                    swipeDelay = 0;
                }
                break;
            case 'b':
                scrollRatioMax = 4;
                try {
                    scrollRatio = swipeRatioValues.get(currentForegroundPackage);
                } catch (NullPointerException e) {
                    scrollRatio = 0;
                }
                break;
            case 'c':
                reverseDirection = true;
                break;
            case 'd':
                swipeFingers = 2;
                break;
        }
        String currentIntervention = getCurrentInterventions();
        broadcastField("Current Interventions", currentIntervention, 2, true);
    }

    public void writeToFile(String txt) {
        Map<String, Object> data = new ArrayMap<>();
        Calendar cal = Calendar.getInstance();
        data.put(cal.get(Calendar.YEAR) + "/" + (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DATE) + " " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND), txt);
        db.collection(participantFilename).document("Behaviors").set(data, SetOptions.merge());
    }

    private void getAllInstalledApps() {
        List<PackageInfo> packageInfos = getPackageManager().getInstalledPackages(PackageManager.GET_PERMISSIONS);
        for (int i = 0; i < packageInfos.size(); i++) {
            PackageInfo packageInfo = packageInfos.get(i);
//            Log.d(TAG, packageInfo.packageName + " " + ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) + " " + getPackageManager().getApplicationLabel(packageInfo.applicationInfo));
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                packageNameMap.put(packageInfo.packageName, packageInfo.applicationInfo.loadLabel(getPackageManager()).toString());
            } else {
                systemPackages.add(packageInfo.packageName);
            }
        }
        packageNameMap.put("com.google.android.youtube", "YouTube");
        systemPackages.remove("com.google.android.youtube");
    }

    public Map<String, UsageStats> getUsageStats(long startTime, long endTime) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        return usageStatsManager.queryAndAggregateUsageStats(
                startTime, endTime);
    }

    public void launchOverlayWindow() {
        if (!isOverlayOn) {
//            increaseIntensity();
            Log.d(TAG, "launchOverlayWindow: " + isInTutorial);
            window.open(this);
            isOverlayOn = true;
            isLongpressListenerOn = false;
            Toast.makeText(this, "Overlay window launched", Toast.LENGTH_SHORT).show();
        }
        if (isDisablingWindowAllowed) {
            window.activateDisablingWindow();
        }
    }

    private void increaseIntensity() {
        if (customizeIntervention) {
            return;
        }
        Log.d(TAG, "increaseIntensity: " + tapDelayMax);
        if (tapDelay + 10 <= tapDelayMax) {
            tapDelay += 10;
        }
        if (swipeDelay + 10 <= swipeDelayMax) {
            swipeDelay += 10;
        }
        if (GestureDetector.TAP_THRESHOLD + 10 <= prolongMax) {
            GestureDetector.TAP_THRESHOLD += 10;
        }
        if (scrollRatioExp + 0.1 <= 1) {
            scrollRatio = (float) Math.pow(scrollRatioMax, scrollRatioExp);
            scrollRatioExp += 0.1;
        }
        if (packageChosen.contains(currentForegroundPackage)) {
            tapDelayValues.put(currentForegroundPackage, tapDelay);
            swipeDelayValues.put(currentForegroundPackage, swipeDelay);
            tapProlongValues.put(currentForegroundPackage, GestureDetector.TAP_THRESHOLD);
            swipeRatioValues.put(currentForegroundPackage, scrollRatio);
        }
        Log.d(TAG, "increaseIntensity: tapDelayValues" + tapDelayValues);
        broadcastField("Current Intervention", getCurrentInterventions(), 2, true);
    }

    public String getCurrentInterventions() {
//        if (CoreService.usingDefaultIntervention) {
//            return "Lockout Window";
//        }
        String res = "";
        if (tapDelayMax != 0) res += String.format(Locale.ENGLISH, "Tap delay: %dms\n", CoreService.tapDelay + 200);
        if (prolongMax != 0) res += String.format(Locale.ENGLISH, "Tap prolong: %dms\n", GestureDetector.TAP_THRESHOLD);
        if (isDoubleTapToSingleTap) res += "Double tap to single tap\n";
        if (xOffset != 0 || yOffset != 0) res += String.format(Locale.ENGLISH, "Tap x offset: %ddp; y offset: %ddp\n", CoreService.xOffset, CoreService.yOffset);
        if (swipeDelayMax != 0) res += String.format(Locale.ENGLISH, "Swipe delay: %dms\n", CoreService.swipeDelay);
        if (scrollRatioMax != 1) res += String.format(Locale.ENGLISH, "Swipe ratio: x%.2f\n", CoreService.scrollRatio);
        if (reverseDirection) res += "Swipe direction reversed\n";
        if (swipeFingers != 1) res += String.format(Locale.ENGLISH, "%d fingers to swipe\n", CoreService.swipeFingers);
        return (!res.equals("")) ? res.substring(0, res.length() - 1) : "Lockout Window";
    }

    private void launchDefaultIntervention(String currentForegroundPackage) {
        if (isDefaultInterventionLaunched) {
            return;
        }
        isDefaultInterventionLaunched = true;
        Intent intent = new Intent(this, AppBlockPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("package_name", currentForegroundPackage);
        startActivity(intent);
    }

    public View launchIgnoreLimitMenu() {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        float height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics());
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(1080, (int) height,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSPARENT);
        params.gravity = Gravity.BOTTOM;
        params.dimAmount = (float) 0.3;
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View menu = layoutInflater.inflate(R.layout.ignore_limit_select_menu, null);
        menu.setBackgroundColor(Color.parseColor("#00123456"));
        windowManager.addView(menu, params);
        return menu;
    }

    public void closeIgnoreLimitMenu(View menu, String packageToGo) {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.removeView(menu);
        if (packageToGo != null) {
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageToGo);
            startActivity(intent);
        }
    }


    private void stepIncrease() {
        currentStep++;
        if (currentStep == SATURATION_NUM) {
            increaseIntensity();
            currentStep = 0;
        }
    }

    public void closeOverlayWindow() {
        Log.d(TAG, "closeOverlayWindow: isOverlayOn " + isOverlayOn);
        if (isOverlayOn) {
            window.close();
            isOverlayOn = false;
            isLongpressListenerOn = false;
            Toast.makeText(this, "Overlay window close", Toast.LENGTH_SHORT).show();
        }
        if (isDisablingWindowAllowed) {
            window.deactivateDisablingWindow();
        }
    }

    public void performSingleTap(float x, float y, long delay, long duration) {
        x = x + xOffset;
        y = y + yOffset;
        stepIncrease();
//        Log.d(TAG, "performSingleTap: \n" + x + ' ' + y);
        if (x < 0 || y < 0) {
            // TODO landscape mode upper bound set.
            Toast.makeText(this, "Offset tap out of bound", Toast.LENGTH_SHORT).show();
//            String content = String.format(Locale.ENGLISH, "SINGLE_TAP_OUT_OF_BOUND;%d;%f;%f\n", System.currentTimeMillis(), x, y);
//            writeToFile(dataFileUri, content);
            return;
        }
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription clickStroke = new GestureDescription.StrokeDescription(path, delay, duration);
        GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
        clickBuilder.addStroke(clickStroke);
        boolean res = this.dispatchGesture(clickBuilder.build(), null, null);
//        String content = String.format(Locale.ENGLISH, "SIM_SINGLE_TAP;%d;%f;%f\n", System.currentTimeMillis(), x, y);
//        writeToFile(dataFileUri, content);
//        Log.d(TAG, "performClick: result is " + res);
    }


    public void performSwipe(int numFigure, Vector<Float>[] x, Vector<Float>[] y, long delay, long duration) {
        stepIncrease();
        if (x.length == 0 || y.length == 0) {
//            String content = String.format(Locale.ENGLISH, "NO_SWIPE_POINTS;%d\n", System.currentTimeMillis());
//            writeToFile(dataFileUri, content);
            return;
        }
//        Log.d(TAG, "performSwipe: \n" + numFigure);
        StringBuilder swipePointers = new StringBuilder();
        Path[] path = new Path[numFigure];
        GestureDescription.StrokeDescription[] swipeStroke = new GestureDescription.StrokeDescription[numFigure];
        GestureDescription.Builder swipeBuilder = new GestureDescription.Builder();
        for (int i = 0; i < numFigure; i++) {
            swipePointers.append(x[i].toString()).append(";").append(y[i].toString()).append(";");
            path[i] = new Path();
            if (reverseDirection) {
                Collections.reverse(x[i]);
                Collections.reverse(y[i]);
            }
            try {
                if (x[i].firstElement() + xOffset < 0 || y[i].firstElement() + yOffset < 0) {
                    Toast.makeText(this, "Offset swipe out of bound", Toast.LENGTH_SHORT).show();
//                    String content = String.format(Locale.ENGLISH, "SWIPE_OUT_OF_BOUND;%d\n", System.currentTimeMillis());
//                    writeToFile(dataFileUri, content);
                    return;
                }
            } catch (NoSuchElementException e) {
                return;
            }
            path[i].moveTo(x[i].firstElement() + xOffset, y[i].firstElement() + yOffset);
            for (int j = 1; j < x[i].size(); j++) {
                if (x[i].get(j) + xOffset < 0 || y[i].get(j) + yOffset < 0) {
                    Toast.makeText(this, "Offset swipe out of bound", Toast.LENGTH_SHORT).show();
//                    String content = String.format(Locale.ENGLISH, "SWIPE_OUT_OF_BOUND;%d\n", System.currentTimeMillis());
//                    writeToFile(dataFileUri, content);
                    return;
                }
                path[i].lineTo(x[i].get(j) + xOffset, y[i].get(j) + yOffset);
            }
//            Log.d(TAG, "performSwipe: \n");
            swipeStroke[i] = new GestureDescription.StrokeDescription(path[i], delay, duration);
            swipeBuilder.addStroke(swipeStroke[i]);
        }
        this.dispatchGesture(swipeBuilder.build(), null, null);
//        writeToFile(dataFileUri, String.format(Locale.ENGLISH, "SIM_SWIPE;%d;%d;%d;", System.currentTimeMillis(), x.length, numFigure) + swipePointers + '\n');
    }

    public void performDoubleTap(float x, float y, long delay, long duration, long interval) {
        stepIncrease();
        x = x + xOffset;
        y = y + yOffset;
        if (x < 0 || y < 0) {
            Toast.makeText(this, "Offset double tap out of bound", Toast.LENGTH_SHORT).show();
            return;
        }
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription clickStroke = new GestureDescription.StrokeDescription(path, delay, duration);
        GestureDescription.StrokeDescription clickStroke2 = new GestureDescription.StrokeDescription(path, delay + interval, duration);
        GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
        clickBuilder.addStroke(clickStroke);
        clickBuilder.addStroke(clickStroke2);
        this.dispatchGesture(clickBuilder.build(), null, null);
    }

    public void broadcast(String title, String txt, boolean hasIntent) {
        Toast.makeText(this, title, Toast.LENGTH_SHORT).show();
        Intent fullScreenIntent = new Intent(this, LabQuiz.class);
        fullScreenIntent.setAction(Intent.ACTION_MAIN);
        fullScreenIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "lab_study")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(txt)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM);
        if (hasIntent) {
            builder = builder.setFullScreenIntent(fullScreenPendingIntent, true);
        }
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(1, builder.build());
    }

    public void broadcastField(String title, String txt, int id, boolean isOngoing) {
        // make a ongoing notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this , "field_study")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSilent(true);
        if (isOngoing) {
                builder.setOngoing(true).setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(txt));
        } else {
            builder.setContentText(txt);
        }

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(id, builder.build());
    }

    public void broadcastInStudySurvey(String title, String txt) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this , "in_study_survey")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(txt)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://forms.gle/4hh6iihVkFTMj66U9"));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setFullScreenIntent(pendingIntent, true);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(3, builder.build());
    }
}
