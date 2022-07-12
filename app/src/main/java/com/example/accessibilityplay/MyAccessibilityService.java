package com.example.accessibilityplay;


import android.accessibilityservice.AccessibilityGestureEvent;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
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
import java.util.Collections;
import java.util.Vector;

public class MyAccessibilityService extends AccessibilityService {
    public static MyAccessibilityService myAccessibilityService;
    public static boolean revertDirection = false;
    public static int tapDelay = 0;
    // 50 to 100 ms time is good for prolong
    public static int tapProlong = 0;
    public static float scrollRatio = 1;
    public static int swipeFingers = 1;
    public Window window;

    private final static String TAG = "MyAccessibilityService.java";
    private boolean isKeyboardOn = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        AccessibilityNodeInfo accessibilityNodeInfo = accessibilityEvent.getSource();
//        boolean checkResult = checkKeyboard();
//        if (!isKeyboardOn && checkResult) {
//            isKeyboardOn = true;
//            window.close();
//        } else if (isKeyboardOn && !checkResult) {
//            isKeyboardOn = false;
//            window.open();
//        }
        if (accessibilityEvent.getPackageName() == null) {
//            Log.d(TAG, "onAccessibilityEvent: NULL");
            return;
        }
        if (accessibilityEvent.getPackageName().equals("com.google.android.inputmethod.latin")) {
            String s = (String) accessibilityEvent.getContentDescription();
            if (s == null) {
                if (isKeyboardOn) {
                    window.open();
                } else {
                    window.close();
                }
                isKeyboardOn = !isKeyboardOn;
                return;
            }
            if (s.contains("Showing") || s.length() == 1) {
                isKeyboardOn = true;
                window.close();
            } else if (s.contains("hidden")) {
                isKeyboardOn = false;
                window.open();
            }
            Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
        }

//        Log.d(TAG, "onAccessibilityEvent: \n" + accessibilityEvent);
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
        window.close();
        return super.onUnbind(intent);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        int result = getStatusBarWidth();
        Log.d(TAG, "onServiceConnected: " + result);

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
//                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
//                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
//                        AccessibilityEvent.TYPE_WINDOWS_CHANGED |
//                        AccessibilityEvent.TYPE_VIEW_SCROLLED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        info.notificationTimeout = 10;
        this.setServiceInfo(info);
        Log.d(TAG, "onServiceConnected: Service connected");
        myAccessibilityService = this;
        window = new Window(this);
        window.open();
    }
    public void performSingleTap(float x, float y, long delay, long duration) {
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
            path[i].moveTo(x[i].firstElement(), y[i].firstElement());
            for (int j = 1; j < x[i].size(); j++) {
                path[i].lineTo(x[i].get(j), y[i].get(j));
            }
//            Log.d(TAG, "performSwipe: \n");
            swipeStroke[i] = new GestureDescription.StrokeDescription(path[i], delay, duration);
            swipeBuilder.addStroke(swipeStroke[i]);
        }
        boolean res = this.dispatchGesture(swipeBuilder.build(), null, null);
//        Log.d(TAG, "performSwipe: " + res);
    }

    public void performDoubleTap(float x, float y, long delay, long duration, long interval) {
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

    public int getStatusBarWidth() {
        int result = 0;
        int resourceId = this.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = this.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }


}
