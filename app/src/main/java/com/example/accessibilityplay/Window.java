package com.example.accessibilityplay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.ConditionVariable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

public class Window {
    private View view;
    private WindowManager windowManager;
    private WindowManager.LayoutParams paramsTouchable;
    private WindowManager.LayoutParams paramsNotTouchable;
    private LayoutInflater layoutInflater;
    private final static String TAG = "LALALA";
    private ConditionVariable cv = new ConditionVariable(false);
    private long downTime;
    private float x, y;


    public Window(Context context) {
        paramsTouchable = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSPARENT);
        paramsNotTouchable = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSPARENT);
        paramsTouchable.gravity = Gravity.CENTER;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = layoutInflater.inflate(R.layout.overlay_window, null);
//        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                Log.d(TAG, "onTouch: action " + MotionEvent.actionToString(action));
                view.dispatchGenericMotionEvent(motionEvent);
                TextView textView = (TextView) view.findViewById(R.id.textView2);
                TextView textView2 = (TextView) view.findViewById(R.id.textView3);
                switch (action) {
                    case (MotionEvent.ACTION_DOWN):
                        x = motionEvent.getX();
                        y = motionEvent.getY();
                        Log.d("CLICK X", "" + x);
                        Log.d("CLICK Y", "" + y);
                        textView.setText("ACTION_DOWN at " + motionEvent.getEventTime());
                        downTime = motionEvent.getEventTime();
                        overlayChangeTouchable(paramsNotTouchable);
                        return false;
                    case (MotionEvent.ACTION_UP):
                        Log.d("CLICK X", "" + motionEvent.getX());
                        Log.d("CLICK Y", "" + motionEvent.getY());
                        textView.setText("ACTION_UP at " + motionEvent.getEventTime());
                        long duration = motionEvent.getEventTime() - downTime;
                        textView2.setText("ACTION DURATION: " + duration + " ms");
                        MyAccessibilityService.myAccessibilityService.performClick(
                                motionEvent.getX(), motionEvent.getY(), 1000, duration);
//                        MyAccessibilityService.myAccessibilityService.performSwipe(100, 100, 0, 50);
                        cv.block(1000);
                        Log.d(TAG, "onTouch: cv opened");
                        overlayChangeTouchable(paramsTouchable);
                        return false;
                    case (MotionEvent.ACTION_CANCEL):
                        Log.d("CLICK X", "" + motionEvent.getX());
                        Log.d("CLICK Y", "" + motionEvent.getY());
                        textView.setText("ACTION_CANCEL at " + motionEvent.getEventTime());
                        overlayChangeTouchable(paramsTouchable);
                        return false;
                    case (MotionEvent.ACTION_MOVE):
                        Log.d("CLICK X", "" + motionEvent.getX());
                        Log.d("CLICK Y", "" + motionEvent.getY());
                        textView.setText("ACTION_MOVE at " + motionEvent.getEventTime());
                        return false;
                    default:
                        return false;
                }
            }
        });

        view.setBackgroundColor(Color.parseColor("#aa123567"));
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void open() {
        if (view.getWindowToken() == null && view.getParent() == null) {
//            Handler handler = new Handler();
//            handler.postDelayed(() -> {
            windowManager.addView(view, paramsTouchable);

            Log.d("Window", "Added");
//            }, 100);
        }
    }

    public void close() {
        windowManager.removeView(view);
        view.invalidate();
        ViewGroup viewGroup = (ViewGroup) view.getParent();
        if (viewGroup != null) {
            viewGroup.removeAllViews();
        }
    }

    private void overlayChangeTouchable(WindowManager.LayoutParams params) {
        try {
            windowManager.updateViewLayout(view, params);
            Log.d(TAG, "overlayChangeTouchable: changing overlay touchability");
        } catch (IllegalArgumentException e) {
            return;
        }
    }

    private float l1Distance(float x1, float y1, float x2, float y2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }
}
