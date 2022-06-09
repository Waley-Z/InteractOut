package com.example.accessibilityplay;

import android.content.Context;
import android.graphics.Color;
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

import androidx.annotation.RequiresApi;

public class Window {
    private Context context;
    private View view;
    private WindowManager windowManager;
    private WindowManager.LayoutParams paramsTouchable;
    private WindowManager.LayoutParams paramsNotTouchable;
    private LayoutInflater layoutInflater;
    private final static String TAG = "LALALA";
    private ConditionVariable cv = new ConditionVariable(false);


    public Window(Context context) {
        this.context = context;
        paramsTouchable = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
//                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSPARENT);
        paramsNotTouchable = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
//                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
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
                switch (action) {
                    case (MotionEvent.ACTION_DOWN):
                        Log.d("CLICK X", "" + motionEvent.getX());
                        Log.d("CLICK Y", "" + motionEvent.getY());
                        overlayChangeTouchable(paramsNotTouchable);
                        return false;
                    case (MotionEvent.ACTION_UP):
                        Log.d("CLICK X", "" + motionEvent.getX());
                        Log.d("CLICK Y", "" + motionEvent.getY());

                        MyAccessibilityService.myAccessibilityService.performClick(
                                motionEvent.getX(), motionEvent.getY());
                        cv.block(1000);
                        Log.d(TAG, "onTouch: cv opened");
                        overlayChangeTouchable(paramsTouchable);
                        return false;
                    case (MotionEvent.ACTION_CANCEL):
                        Log.d("CLICK X", "" + motionEvent.getX());
                        Log.d("CLICK Y", "" + motionEvent.getY());
                        overlayChangeTouchable(paramsTouchable);
                        return false;
                    default:
//                        Log.d("ACTION", "others");
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
}
