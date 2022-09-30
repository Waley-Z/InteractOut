package com.example.accessibilityplay;


import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.ArrayMap;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
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
    public static CountDownTimer stageTimer = null;
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
            customizeIntervention = false;
    public static int tapDelay = 0, tapDelayMax = 0;
    public static int swipeDelay = 0, swipeDelayMax = 0;
    public static int minimumFingerToTap = 1;
    // 50 to 100 ms time is good for prolong
    public static int swipeFingers = 1, swipeFingersMax = 0;
    public static int xOffset = 0;
    public static int yOffset = 0;
    public static int prolongMax = 0;
    public static float scrollRatio = 1, scrollRatioMax = 1, scrollRatioExp = 0;
    public static int strength = 1;
    // TODO need to find out a way refill show time every day.
    public static int prolongNoteShowTime = 100;
    public static int screenWidth = 1080, screenHeight = 2400;
    public static long timeBeforeOverlaid;
    public static String currentForegroundPackage = "", currrentClassName = "";
    public static String participantFilename = "Field Study Data.txt";
    public static Vector<String> appChosen = new Vector<>(), packageChosen = new Vector<>(), systemPackages = new Vector<>();
    public static ArrayMap<String, Long> appUsedTime = new ArrayMap<>(), appTargetTime = new ArrayMap<>(), appGrantedTime = new ArrayMap<>();
    public static ArrayMap<String, String> packageNameMap = new ArrayMap<>();
    public static Uri dataFileUri;

    private final static String TAG = "MyAccessibilityService.java";
    private Calendar beginOfToday;
    private CountDownTimer countDownTimer;
    private boolean isCountdownLaunched = false;
    private final long SATURATION_NUM = 1; // after 10 interactions, call increaseIntensity()
    private int currentStep = 0;
    private PendingIntent inStudySurveyPopUpIntent;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        int date = (beginOfToday == null) ? 0 : beginOfToday.get(Calendar.DATE);
        beginOfToday = Calendar.getInstance();
        if (date != 0 && date != beginOfToday.get(Calendar.DATE)) {
            dailyClean();
        }
        beginOfToday.set(Calendar.HOUR, 0);
        beginOfToday.set(Calendar.MINUTE, 0);
        beginOfToday.set(Calendar.SECOND, 0);
        Log.d(TAG, "onAccessibilityEvent: \n" + accessibilityEvent);
//        String content = String.format(Locale.ENGLISH, "WINDOW_TRANSITION;%d;%s\n", System.currentTimeMillis(), accessibilityEvent.toString());
//        writeToFile(dataFileUri, content);

        if (accessibilityEvent.getPackageName() == null
                || (accessibilityEvent.getPackageName().equals(getPackageName()) && (accessibilityEvent.getClassName().equals("android.view.ViewGroup") || accessibilityEvent.getClassName().equals("android.app.AlertDialog"))))
//                (accessibilityEvent.getPackageName().equals("com.google.android.apps.nexuslauncher") && (accessibilityEvent.getContentChangeTypes() != 0 || accessibilityEvent.getWindowChanges() != 0)))
        {
            Log.d(TAG, "onAccessibilityEvent: Returned");
            Log.d(TAG, "onAccessibilityEvent: isOverlay = " + isOverlayOn);
            return;
        }
        currentForegroundPackage = (String) accessibilityEvent.getPackageName();
        currrentClassName = (String) accessibilityEvent.getClassName();
        if (isInLabMode && accessibilityEvent.getClassName().equals(getPackageName() + ".Tutorial")) {
            if (!isOverlayOn && !inTutorialMainPage) {
                    launchOverlayWindow();
            }
            return;
        }
        if (isInLabMode && (currentForegroundPackage.equals("com.twitter.android") || currentForegroundPackage.equals("com.teamlava.bubble"))) {
            // in lab mode the intervention of these two app is control on purpose
            return;
        }
        if (systemPackages.contains(currentForegroundPackage)) {
            if (!currrentClassName.equals("com.android.systemui.volume.VolumeDialogImpl$CustomDialog") && isCountdownLaunched) {
                countDownTimer.cancel();
                isCountdownLaunched = false;
                Toast.makeText(this, "countdown turned off", Toast.LENGTH_SHORT).show();
            }
            return;
        }
//        if (currentForegroundPackage.equals("com.android.hbmsvmanager")) {
//            return;
//        }
//        if (isInLabMode && accessibilityEvent.getPackageName().equals("com.android.chrome")) {
//            return;
//        }
//        if (accessibilityEvent.getPackageName().equals("com.android.systemui")) {
//            return;
//        }
//        if (accessibilityEvent.getPackageName().equals("com.google.android.googlequicksearchbox")) {
//            return;
//        }
//        if (accessibilityEvent.getPackageName().equals("com.google.android.apps.nexuslauncher")) {
//            return;
//        }

        if (accessibilityEvent.getPackageName().equals("com.google.android.inputmethod.latin")) {
//            Log.d(TAG, "onAccessibilityEvent: " + accessibilityEvent.getContentDescription());
//            if (accessibilityEvent.getContentDescription() == null) {
//                if (isKeyboardOn) {
//                    launchOverlayWindow();
//                } else {
//                    closeOverlayWindow();
//                }
//                isKeyboardOn = !isKeyboardOn;
//            } else {
//                String s = (String) accessibilityEvent.getContentDescription();
//                if (s.contains("Showing") || s.length() == 1) {
//                    isKeyboardOn = true;
//                    closeOverlayWindow();
//                } else if (s.contains("hidden")) {
//                    isKeyboardOn = false;
//                    launchOverlayWindow();
//                }
//                Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
//            }
            return;
        }
        refreshUseTime();
        Log.d(TAG, "onAccessibilityEvent: \n" + appUsedTime + '\n' + appTargetTime);
        boolean isTargetApp = appUsedTime.containsKey(currentForegroundPackage);
        if (isTargetApp) {
            String content = String.format(Locale.ENGLISH, "USAGE;%d;%s;%s;%s\n", System.currentTimeMillis(), appUsedTime, appTargetTime, appGrantedTime);
            writeToFile(dataFileUri, content);
            if (!isOverlayOn) {
                long time = appUsedTime.get(currentForegroundPackage);
                long targetTime = appTargetTime.get(currentForegroundPackage) + appGrantedTime.get(currentForegroundPackage);
                if (time > targetTime) {
                    if (usingDefaultIntervention) {
                        launchDefaultIntervention(currentForegroundPackage);
                    } else {
                        launchOverlayWindow();
                    }
                } else if (!isCountdownLaunched && targetTime > time) {
                    long timeRemain = targetTime - time;
                    countDownTimer = new CountDownTimer(timeRemain, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                        Log.d(TAG, "onTick: " + millisUntilFinished / 1000 + "s remaining");
                        }

                        @Override
                        public void onFinish() {
                            if (usingDefaultIntervention) {
                                launchDefaultIntervention(currentForegroundPackage);
                            } else {
                                launchOverlayWindow();
                            }
                            isCountdownLaunched = false;
                        }
                    }.start();
                    isCountdownLaunched = true;
                    Toast.makeText(this, "countdown turned on", Toast.LENGTH_SHORT).show();
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

    private void dailyClean() {
        // clean extra permitted time and reset intervention values
        String content = String.format(Locale.ENGLISH, "NEW_DAY;%d\n", System.currentTimeMillis());
        writeToFile(dataFileUri, content);
        resetInterventions();
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
//                Log.d(TAG, "run: has item " + item);
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
//            if (usedTime > timeBeforeOverlaid && currentForegroundPackage.equals(item)) {
//                launchOverlayWindow();
//            }
//            usedTime /= 1000;
//            long hour = usedTime / 3600;
//            long minute = (usedTime - hour * 3600) / 60;
//            long second = usedTime - hour * 3600 - minute * 60;
//            Log.d(TAG, String.format("%s running %d hour, %d minute and %d second", item, hour, minute, second));
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
        return super.onUnbind(intent);
    }


    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        window = new Window(this);
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        info.notificationTimeout = 10;
        this.setServiceInfo(info);
        Log.d(TAG, "onServiceConnected: \n" + screenWidth + ' ' + screenHeight);
        coreService = this;

        // get all installed apps
        getAllInstalledApps();
        Log.d(TAG, "onServiceConnected: Service connected");
        dataFileUri = createFile(participantFilename);
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

    public void writeToFile(Uri uri, String txt) {
        try {
            OutputStream fileOutputStream = this.getContentResolver().openOutputStream(uri, "wa");
            fileOutputStream.write(txt.getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Uri createFile(String filename) {
        File file = new File(Environment.DIRECTORY_DOCUMENTS, getString(R.string.app_name));
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, file.toString());
        Uri uri = this.getContentResolver().insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues);
        return uri;
    }


    private void getAllInstalledApps() {
        List<PackageInfo> packageInfos = getPackageManager().getInstalledPackages(PackageManager.GET_PERMISSIONS);
        for (int i = 0; i < packageInfos.size(); i++) {
            PackageInfo packageInfo = packageInfos.get(i);
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                packageNameMap.put(packageInfo.packageName, packageInfo.applicationInfo.loadLabel(getPackageManager()).toString());
            } else {
                systemPackages.add(packageInfo.packageName);
            }
//            Log.d(TAG, "onServiceConnected: \n" + packageInfo.applicationInfo.loadLabel(getPackageManager()).toString() + "     " + packageInfo.packageName);
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
        broadcastField("Current Intervention", getCurrentInterventions(), 2, true);
    }

    public String getCurrentInterventions() {
        if (CoreService.usingDefaultIntervention) {
            return "Lockout Window";
        }
        String res = "";
        if (tapDelayMax != 0) res += String.format(Locale.ENGLISH, "Tap delay: %dms\n", CoreService.tapDelay + 200);
        if (prolongMax != 0) res += String.format(Locale.ENGLISH, "Tap prolong: %dms\n", GestureDetector.TAP_THRESHOLD);
        if (isDoubleTapToSingleTap) res += "Double tap to single tap\n";
        if (xOffset != 0 || yOffset != 0) res += String.format(Locale.ENGLISH, "Tap x offset: %ddp; y offset: %ddp\n", CoreService.xOffset, CoreService.yOffset);
        if (swipeDelayMax != 0) res += String.format(Locale.ENGLISH, "Swipe delay: %dms\n", CoreService.swipeDelay);
        if (scrollRatioMax != 1) res += String.format(Locale.ENGLISH, "Swipe ratio: x%.2f\n", CoreService.scrollRatio);
        if (reverseDirection) res += "Swipe direction reversed\n";
        if (swipeFingers != 1) res += String.format(Locale.ENGLISH, "%d fingers to swipe\n", CoreService.swipeFingers);
        return (!res.equals("")) ? res.substring(0, res.length() - 1) : "";
    }

    private void launchDefaultIntervention(String currentForegroundPackage) {
        if (isDefaultInterventionLaunched) {
            return;
        }
        isDefaultInterventionLaunched = true;
        AlertDialog dialog;
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("You reached the time limit!");
        alertBuilder.setSingleChoiceItems(new String[]{"Stop use", "1 more minute", "5 more minutes", "Rest of the day"}, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isDefaultInterventionLaunched = false;
                long oldValue;
                String content;
                switch (which) {
                    case 0:
                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                        startMain.addCategory(Intent.CATEGORY_HOME);
                        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(startMain);
                        dialog.dismiss();
                        content = String.format(Locale.ENGLISH, "DEFAULT_INTERVENTION;%d;0\n", System.currentTimeMillis());
                        writeToFile(dataFileUri, content);
                        return;
                    case 1:
                        oldValue = appGrantedTime.get(currentForegroundPackage);
                        appGrantedTime.put(currentForegroundPackage, oldValue + 60000);
                        dialog.dismiss();
                        content = String.format(Locale.ENGLISH, "DEFAULT_INTERVENTION;%d;1\n", System.currentTimeMillis());
                        writeToFile(dataFileUri, content);
                        return;
                    case 2:
                        oldValue = appGrantedTime.get(currentForegroundPackage);
                        appGrantedTime.put(currentForegroundPackage, oldValue + 300000);
                        dialog.dismiss();
                        content = String.format(Locale.ENGLISH, "DEFAULT_INTERVENTION;%d;2\n", System.currentTimeMillis());
                        writeToFile(dataFileUri, content);
                        return;
                    case 3:
                        appGrantedTime.put(currentForegroundPackage, 86400000L);
                        dialog.dismiss();
                        content = String.format(Locale.ENGLISH, "DEFAULT_INTERVENTION;%d;3\n", System.currentTimeMillis());
                        writeToFile(dataFileUri, content);
                }
            }
        });
        alertBuilder.setCancelable(false);
        dialog = alertBuilder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
        dialog.show();
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
//            String content = String.format(Locale.ENGLISH, "DOUBLE_TAP_OUT_OF_BOUND;%d\n", System.currentTimeMillis());
//            writeToFile(dataFileUri, content);
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
//        String content = String.format(Locale.ENGLISH, "SIM_DOUBLE_TAP;%d;%f;%f\n", System.currentTimeMillis(), x, y);
//        writeToFile(dataFileUri, content);
    }

//    @RequiresApi(api = Build.VERSION_CODES.S)
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
