package com.example.accessibilityplay;


import android.Manifest;
import android.accessibilityservice.AccessibilityGestureEvent;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Array;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class MyAccessibilityService extends AccessibilityService {
    public static MyAccessibilityService myAccessibilityService;
    public Window window;
    public static boolean revertDirection = false;
    public static int tapDelay = 0;
    // 50 to 100 ms time is good for prolong
    public static int swipeFingers = 1;
    public static int xOffset = 0;
    public static int yOffset = 0;
    public static int strength = 1;
    public static int screenWidth = 1080, screenHeight = 2400;
    public static int disablingWindowNum = 1, disablingWindowWidth = 100, disablingWindowHeight = 100;
    public static float scrollRatio = 1;
    public static long timeBeforeOverlaid;
    public static String currentForegroundPackage = "";
    public static Vector<String> appChosen = new Vector<>(), packageChosen = new Vector<>();
    public static ArrayMap<String, Long> appUsedTime = new ArrayMap<>();
    public static ArrayMap<String, String> packageNameMap = new ArrayMap<>();

    private final static String TAG = "MyAccessibilityService.java";
    private Calendar beginOfToday;
    private CountDownTimer countDownTimer;
    private boolean isKeyboardOn = false;
    private boolean isOverlayOn = false;
    private boolean isCountdownLaunched = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        if (accessibilityEvent.getPackageName() == null || accessibilityEvent.getPackageName().equals(getPackageName())) {
//            Log.d(TAG, "onAccessibilityEvent: NULL");
            return;
        }
//        if (accessibilityEvent.getPackageName().equals("com.google.android.inputmethod.latin")) {
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
//        }
        refreshUseTime();
//        Log.d(TAG, "onAccessibilityEvent: \n" + appUsedTime);
        boolean isTargetApp = appUsedTime.containsKey((String) accessibilityEvent.getPackageName());
        if (!isOverlayOn && isTargetApp) {
            currentForegroundPackage = (String) accessibilityEvent.getPackageName();
            long time = appUsedTime.get((String) accessibilityEvent.getPackageName());
            if (time > timeBeforeOverlaid) {
                launchOverlayWindow();
            } else if (!isCountdownLaunched) {
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
        } else if (!isTargetApp && isOverlayOn) {
            closeOverlayWindow();
            Toast.makeText(this, "Overlay turned off", Toast.LENGTH_SHORT).show();
            currentForegroundPackage = "";
        } else if (!isTargetApp) {
            if (isCountdownLaunched) {
                countDownTimer.cancel();
                isCountdownLaunched = false;
            Toast.makeText(this, "countdown turned off", Toast.LENGTH_SHORT).show();
            }
        }
        Log.d(TAG, "onAccessibilityEvent: \n" + accessibilityEvent);
    }

    public void refreshUseTime() {
        Map<String, UsageStats> stats = getUsageStats();
        MyAccessibilityService.packageChosen.forEach((item) -> {
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
            if (usedTime > timeBeforeOverlaid && currentForegroundPackage.equals(item)) {
                myAccessibilityService.launchOverlayWindow();
            }
            usedTime /= 1000;
            long hour = usedTime / 3600;
            long minute = (usedTime - hour * 3600) / 60;
            long second = usedTime - hour * 3600 - minute * 60;
            Log.d(TAG, String.format("%s running %d hour, %d minute and %d second", item, hour, minute, second));
        });
    }

    public MyAccessibilityService() {
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
        myAccessibilityService = this;

        beginOfToday = Calendar.getInstance();
        beginOfToday.set(Calendar.HOUR, 0);
        beginOfToday.set(Calendar.MINUTE, 0);
        beginOfToday.set(Calendar.SECOND, 0);

        final Map<String, UsageStats> stats = getUsageStats();

        if (stats.isEmpty()) {
            // not a good way, since it may get activated at the beginning of the day.
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        List<PackageInfo> packageInfos = getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < packageInfos.size(); i++) {
            PackageInfo packageInfo = packageInfos.get(i);
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                packageNameMap.put(packageInfo.packageName, packageInfo.applicationInfo.loadLabel(getPackageManager()).toString());
            }
//            Log.d(TAG, "onServiceConnected: \n" + packageInfo.applicationInfo.loadLabel(getPackageManager()).toString() + "     " + packageInfo.packageName);
        }
        Log.d(TAG, "onServiceConnected: Service connected");
    }

    public Map<String, UsageStats> getUsageStats() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        return usageStatsManager.queryAndAggregateUsageStats(
                beginOfToday.getTimeInMillis(), System.currentTimeMillis());
    }

    public void launchOverlayWindow() {
        if (!isOverlayOn) {
            window.open();
            isOverlayOn = true;
            Toast.makeText(this, "Overlay window launched", Toast.LENGTH_SHORT).show();
        }
//        else {
//            Toast.makeText(this, "Intervention already in function", Toast.LENGTH_SHORT).show();
//        }
    }

    public void closeOverlayWindow() {
        if (isOverlayOn) {
            window.close();
            isOverlayOn = false;
            Toast.makeText(this, "Overlay window close", Toast.LENGTH_SHORT).show();
        }
    }

    public void performSingleTap(float x, float y, long delay, long duration) {
        x = x + xOffset;
        y = y + yOffset;
        Log.d(TAG, "performSingleTap: \n" + x + ' ' + y);
        if (x < 0 || y < 0) {
            Toast.makeText(this, "Offset tap out of bound", Toast.LENGTH_SHORT).show();
            return;
        }
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription clickStroke = new GestureDescription.StrokeDescription(path, delay, duration);
        GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
        clickBuilder.addStroke(clickStroke);
        boolean res = this.dispatchGesture(clickBuilder.build(), null, null);
        Log.d(TAG, "performClick: result is " + res);
    }


    public void performSwipe(int numFigure, Vector<Float>[] x, Vector<Float>[] y, long delay, long duration) {
        if (x.length == 0 || y.length == 0) {
            return;
        }
//        Log.d(TAG, "performSwipe: \n" + numFigure);
        Path[] path = new Path[numFigure];
        GestureDescription.StrokeDescription[] swipeStroke = new GestureDescription.StrokeDescription[numFigure];
        GestureDescription.Builder swipeBuilder = new GestureDescription.Builder();
        for (int i = 0; i < numFigure; i++) {
            path[i] = new Path();
            if (revertDirection) {
                Collections.reverse(x[i]);
                Collections.reverse(y[i]);
            }
            try {
                if (x[i].firstElement() + xOffset < 0 || y[i].firstElement() + yOffset < 0) {
                    Toast.makeText(this, "Offset swipe out of bound", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NoSuchElementException e) {
                return;
            }
            path[i].moveTo(x[i].firstElement() + xOffset, y[i].firstElement() + yOffset);
            for (int j = 1; j < x[i].size(); j++) {
                if (x[i].get(j) + xOffset < 0 || y[i].get(j) + yOffset < 0) {
                    Toast.makeText(this, "Offset swipe out of bound", Toast.LENGTH_SHORT).show();
                    return;
                }
                path[i].lineTo(x[i].get(j) + xOffset, y[i].get(j) + yOffset);
            }
//            Log.d(TAG, "performSwipe: \n");
            swipeStroke[i] = new GestureDescription.StrokeDescription(path[i], delay, duration);
            swipeBuilder.addStroke(swipeStroke[i]);
        }
        boolean res = this.dispatchGesture(swipeBuilder.build(), null, null);
//        Log.d(TAG, "performSwipe: " + res);
    }

    public void performDoubleTap(float x, float y, long delay, long duration, long interval) {
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
        boolean res = this.dispatchGesture(clickBuilder.build(), null, null);
//        Log.d(TAG, "performDoubleTap: result 1 is " + res);
    }


}
