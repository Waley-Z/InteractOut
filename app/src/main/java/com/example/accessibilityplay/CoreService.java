package com.example.accessibilityplay;


import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.Path;
import android.os.CountDownTimer;
import android.util.ArrayMap;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
            isDoubleTapToSingleTap = false;
    public static int tapDelay = 0;
    public static int swipeDelay = 0;
    public static int minimumFingerToTap = 1;
    // 50 to 100 ms time is good for prolong
    public static int swipeFingers = 1;
    public static int xOffset = 0;
    public static int yOffset = 0;
    public static int strength = 1;
    // TODO need to find out a way refill show time every day.
    public static int prolongNoteShowTime = 15;
    public static int screenWidth = 1080, screenHeight = 2400;
    public static float scrollRatio = 1;
    public static long timeBeforeOverlaid;
    public static String currentForegroundPackage = "";
    public static String participantFilename;
    public static Vector<String> appChosen = new Vector<>(), packageChosen = new Vector<>();
    public static Vector<Boolean> appListDisplay = new Vector<>();
    public static ArrayMap<String, Long> appUsedTime = new ArrayMap<>();
    public static ArrayMap<String, String> packageNameMap = new ArrayMap<>();

    private final static String TAG = "MyAccessibilityService.java";
    private Calendar beginOfToday;
    private CountDownTimer countDownTimer;
    private boolean isCountdownLaunched = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        beginOfToday = Calendar.getInstance();
        beginOfToday.set(Calendar.HOUR, 0);
        beginOfToday.set(Calendar.MINUTE, 0);
        beginOfToday.set(Calendar.SECOND, 0);
        Log.d(TAG, "onAccessibilityEvent: \n" + accessibilityEvent);
        String content = String.format(Locale.ENGLISH, "WINDOW_TRANSITION;%d;%s\n", System.currentTimeMillis(), accessibilityEvent.toString());
        writeToFile(participantFilename, content, MODE_APPEND);
        if (accessibilityEvent.getPackageName() == null
                || (accessibilityEvent.getPackageName().equals(getPackageName()) && accessibilityEvent.getClassName().equals("android.view.ViewGroup")))
//                (accessibilityEvent.getPackageName().equals("com.google.android.apps.nexuslauncher") && (accessibilityEvent.getContentChangeTypes() != 0 || accessibilityEvent.getWindowChanges() != 0)))
        {
            Log.d(TAG, "onAccessibilityEvent: Returned");
            Log.d(TAG, "onAccessibilityEvent: isOverlay = " + isOverlayOn);
            return;
        }
        currentForegroundPackage = (String) accessibilityEvent.getPackageName();
        if (isInLabMode && accessibilityEvent.getClassName().equals(getPackageName() + ".Tutorial")) {
            if (!isOverlayOn && !inTutorialMainPage) {
                launchOverlayWindow();
            }
            return;
        }
        if (currentForegroundPackage.equals("com.android.hbmsvmanager")) {
            return;
        }
        if (isInLabMode && (currentForegroundPackage.equals("com.twitter.android") || currentForegroundPackage.equals("com.teamlava.bubble"))) {
            // in lab mode the intervention of these two app is control on purpose
            return;
        }
        if (isInLabMode && accessibilityEvent.getPackageName().equals("com.android.chrome")) {
            return;
        }
        if (isInLabMode && accessibilityEvent.getPackageName().equals("com.android.systemui")) {
            return;
        }
        if (accessibilityEvent.getPackageName().equals("com.google.android.googlequicksearchbox")) {
            return;
        }
        if (isInLabMode && accessibilityEvent.getPackageName().equals("com.google.android.apps.nexuslauncher")) {
            return;
        }

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
        Log.d(TAG, "onAccessibilityEvent: \n" + appUsedTime);
        boolean isTargetApp = appUsedTime.containsKey((String) accessibilityEvent.getPackageName());
        if (isTargetApp) {
            if (!isOverlayOn) {
                long time = appUsedTime.get((String) accessibilityEvent.getPackageName());
                if (time > timeBeforeOverlaid) {
                    launchOverlayWindow();
                } else if (!isCountdownLaunched && timeBeforeOverlaid > time) {
                    long timeRemain = timeBeforeOverlaid - time;
                    countDownTimer = new CountDownTimer(timeRemain, timeRemain) {
                        @Override
                        public void onTick(long millisUntilFinished) {
//                        Log.d(TAG, "onTick: " + millisUntilFinished / 1000 + "s remaining");
                        }

                        @Override
                        public void onFinish() {
                            launchOverlayWindow();
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
            usedTime /= 1000;
            long hour = usedTime / 3600;
            long minute = (usedTime - hour * 3600) / 60;
            long second = usedTime - hour * 3600 - minute * 60;
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
    }

    private void getAllInstalledApps() {
        List<PackageInfo> packageInfos = getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < packageInfos.size(); i++) {
            PackageInfo packageInfo = packageInfos.get(i);
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                packageNameMap.put(packageInfo.packageName, packageInfo.applicationInfo.loadLabel(getPackageManager()).toString());
                appListDisplay.add(false);
            }
//            Log.d(TAG, "onServiceConnected: \n" + packageInfo.applicationInfo.loadLabel(getPackageManager()).toString() + "     " + packageInfo.packageName);
        }
    }

    public Map<String, UsageStats> getUsageStats(long startTime, long endTime) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        return usageStatsManager.queryAndAggregateUsageStats(
                startTime, endTime);
    }

    public void launchOverlayWindow() {
        if (!isOverlayOn) {
            Log.d(TAG, "launchOverlayWindow: " + isInTutorial);
            window.open(this);
            isOverlayOn = true;
            isLongpressListenerOn = false;
            Toast.makeText(this, "Overlay window launched", Toast.LENGTH_SHORT).show();
        }
        if (isDisablingWindowAllowed) {
            window.activateDisablingWindow();
        }
//        else {
//            Toast.makeText(this, "Intervention already in function", Toast.LENGTH_SHORT).show();
//        }
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
//        Log.d(TAG, "performSingleTap: \n" + x + ' ' + y);
        if (x < 0 || y < 0) {
            // TODO landscape mode upper bound set.
            Toast.makeText(this, "Offset tap out of bound", Toast.LENGTH_SHORT).show();
            String content = String.format(Locale.ENGLISH, "SINGLE_TAP_OUT_OF_BOUND;%d;%f;%f\n", System.currentTimeMillis(), x, y);
            writeToFile(participantFilename, content, MODE_APPEND);
            return;
        }
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription clickStroke = new GestureDescription.StrokeDescription(path, delay, duration);
        GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
        clickBuilder.addStroke(clickStroke);
        boolean res = this.dispatchGesture(clickBuilder.build(), null, null);
        String content = String.format(Locale.ENGLISH, "SIM_SINGLE_TAP;%d;%f;%f\n", System.currentTimeMillis(), x, y);
        writeToFile(participantFilename, content, MODE_APPEND);
//        Log.d(TAG, "performClick: result is " + res);
    }


    public void performSwipe(int numFigure, Vector<Float>[] x, Vector<Float>[] y, long delay, long duration) {
        if (x.length == 0 || y.length == 0) {
            String content = String.format(Locale.ENGLISH, "NO_SWIPE_POINTS;%d\n", System.currentTimeMillis());
            writeToFile(participantFilename, content, MODE_APPEND);
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
                    String content = String.format(Locale.ENGLISH, "SWIPE_OUT_OF_BOUND;%d\n", System.currentTimeMillis());
                    writeToFile(participantFilename, content, MODE_APPEND);
                    return;
                }
            } catch (NoSuchElementException e) {
                return;
            }
            path[i].moveTo(x[i].firstElement() + xOffset, y[i].firstElement() + yOffset);
            for (int j = 1; j < x[i].size(); j++) {
                if (x[i].get(j) + xOffset < 0 || y[i].get(j) + yOffset < 0) {
                    Toast.makeText(this, "Offset swipe out of bound", Toast.LENGTH_SHORT).show();
                    String content = String.format(Locale.ENGLISH, "SWIPE_OUT_OF_BOUND;%d\n", System.currentTimeMillis());
                    writeToFile(participantFilename, content, MODE_APPEND);
                    return;
                }
                path[i].lineTo(x[i].get(j) + xOffset, y[i].get(j) + yOffset);
            }
//            Log.d(TAG, "performSwipe: \n");
            swipeStroke[i] = new GestureDescription.StrokeDescription(path[i], delay, duration);
            swipeBuilder.addStroke(swipeStroke[i]);
        }
        this.dispatchGesture(swipeBuilder.build(), null, null);
        writeToFile(participantFilename, String.format(Locale.ENGLISH, "SIM_SWIPE;%d;%d;%d;", System.currentTimeMillis(), x.length, numFigure) + swipePointers + '\n', MODE_APPEND);
    }

    public void performDoubleTap(float x, float y, long delay, long duration, long interval) {
        x = x + xOffset;
        y = y + yOffset;
        if (x < 0 || y < 0) {
            Toast.makeText(this, "Offset double tap out of bound", Toast.LENGTH_SHORT).show();
            String content = String.format(Locale.ENGLISH, "DOUBLE_TAP_OUT_OF_BOUND;%d\n", System.currentTimeMillis());
            writeToFile(participantFilename, content, MODE_APPEND);
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
        String content = String.format(Locale.ENGLISH, "SIM_DOUBLE_TAP;%d;%f;%f\n", System.currentTimeMillis(), x, y);
        writeToFile(participantFilename, content, MODE_APPEND);
    }

//    @RequiresApi(api = Build.VERSION_CODES.S)
    public void broadcast(String title, String txt, boolean hasIntent) {
        Toast.makeText(this, title, Toast.LENGTH_SHORT).show();
        Intent fullScreenIntent = new Intent(this, LabQuiz.class);
        fullScreenIntent.setAction(Intent.ACTION_MAIN);
        fullScreenIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "lalala")
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
}
