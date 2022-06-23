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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.IOException;

public class MyAccessibilityService extends AccessibilityService {
    public static MyAccessibilityService myAccessibilityService;
    final static String TAG = "LALALA";
    private Window window;
    private int statusBarHeight;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        AccessibilityNodeInfo accessibilityNodeInfo = accessibilityEvent.getSource();
        if (AccessibilityEvent.eventTypeToString(accessibilityEvent.getEventType()).equals("TYPE_WINDOW_CONTENT_CHANGED")) {
            return;
        }
//        Log.d(TAG, "onAccessibilityEvent: \n" + accessibilityEvent);
//        AccessibilityNodeInfo rootWindow = getRootInActiveWindow();
//        Log.d(TAG, "onServiceConnected: \n" + rootWindow);
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
        try {
            window.close();
        } catch (IllegalArgumentException ignore){

        }

        return super.onUnbind(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        int result = getStatusBarWidth();
        Configurations.configuration.setStatusBarHeight(result);
        Log.d(TAG, "onServiceConnected: " + result);
        statusBarHeight = result;

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_HAPTIC;
        info.notificationTimeout = 10;
        this.setServiceInfo(info);
        Log.d(TAG, "onServiceConnected: Service connected");
        myAccessibilityService = this;
        window = new Window(this);
        window.open();

    }
    public void performSingleTap(float x, float y, long delay, long duration) {
        Path path = new Path();
        Log.d(TAG, "performClick: " + statusBarHeight);
        path.moveTo(x, y + 2 * statusBarHeight);
        GestureDescription.StrokeDescription clickStroke = new GestureDescription.StrokeDescription(path, delay, duration);
        GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
        clickBuilder.addStroke(clickStroke);
        boolean res = this.dispatchGesture(clickBuilder.build(), null, null);
        Log.d(TAG, "performClick: result is " + res);
    }

    @Override
    public boolean onGesture(@NonNull AccessibilityGestureEvent gestureEvent) {
        Log.d(TAG, "onGesture: \n" + gestureEvent);
        return super.onGesture(gestureEvent);
    }

    public void performSwipe(float x1, float y1, float x2, float y2, long delay, long duration) {
        Path path = new Path();
        Log.d(TAG, "performSwipe: " + x1 + ' ' + y1 + ' ' + x2 + ' ' + y2);
        path.moveTo(x1, y1 + 2 * statusBarHeight);
        path.lineTo(x2, y2 + 2 * statusBarHeight);
        GestureDescription.StrokeDescription swipeStroke = new GestureDescription.StrokeDescription(path, delay, duration);
        GestureDescription.Builder swipeBuilder = new GestureDescription.Builder();
        swipeBuilder.addStroke(swipeStroke);
        boolean res = this.dispatchGesture(swipeBuilder.build(), null, null);
        Log.d(TAG, "performSwipe: " + res);
    }

    public void performDoubleTap(float x, float y, long delay, long duration, long interval) {
        Path path = new Path();
        path.moveTo(x, y + 2 * statusBarHeight);
//        path.lineTo(x + 1, y + 2 * statusBarHeight);
        GestureDescription.StrokeDescription clickStroke = new GestureDescription.StrokeDescription(path, delay, duration);
        GestureDescription.StrokeDescription clickStroke2 = new GestureDescription.StrokeDescription(path, delay + interval, duration);
        GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
        clickBuilder.addStroke(clickStroke);
        clickBuilder.addStroke(clickStroke2);
        boolean res = this.dispatchGesture(clickBuilder.build(), null, null);
        Log.d(TAG, "performDoubleTap: result 1 is " + res);
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
