package com.example.accessibilityplay;

import static com.example.accessibilityplay.MyAccessibilityService.scrollRatio;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.ConditionVariable;
import android.os.Handler;
import android.util.Log;
//import com.example.accessibilityplay.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;
import java.util.Vector;

public class Window {
    private final View overlay;
    private final View listener;
    private final View disableArea;
    private final WindowManager windowManager;
    private final WindowManager.LayoutParams paramsTouchable;
    private final WindowManager.LayoutParams paramsListener, paramDisableArea;
    private final static String TAG = "Window.java";
    private final ConditionVariable cv = new ConditionVariable(false);
    private boolean isTouchable, disableActive = false;
    private long downTime, lastScrollTime, upTime;
    private final Vector<Float>[] x = new Vector[10];
    private final Vector<Float>[] y = new Vector[10];
    private int scrollNumber = 0, downNumber = 0, numFingers = 0;
    private final GestureDetector mDetector;
    private final Handler handler = new Handler();
    private boolean isTapTooShort = false;




    private final Runnable periodicToggle = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "run: \n" + disableActive);
            disableActive = !disableActive;
            toggleDisableArea(disableActive);
            handler.postDelayed(periodicToggle, 5000);
        }
    };


    public Window(Context context) {
        paramsTouchable = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                        WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSPARENT);
        paramsTouchable.gravity = Gravity.CENTER;
        paramsListener = new WindowManager.LayoutParams(1, 1,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSPARENT);
        paramDisableArea = new WindowManager.LayoutParams(200, 200,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSPARENT);
        paramsListener.gravity = Gravity.TOP | Gravity.START;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        overlay = layoutInflater.inflate(R.layout.overlay_window, null);
        // listener view is used for listening to the touch event during the temporary disable of long press
        listener = layoutInflater.inflate(R.layout.overlay_window, null);
        disableArea = layoutInflater.inflate(R.layout.overlay_window, null);
        TextView textViewAction = (TextView) overlay.findViewById(R.id.textView2);
        TextView textViewDuration = (TextView) overlay.findViewById(R.id.textView3);
        final long[] duration = new long[1];
        for (int i = 0; i < 10; i++) {
            x[i] = new Vector<>(100);
            y[i] = new Vector<>(100);
        }

        // get the gesture detector
        mDetector = new GestureDetector(context, new MyGestureListener());

        // This touch listener passes everything on to the gesture detector.
        // That saves us the trouble of interpreting the raw touch events
        // ourselves.
        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // pass the events to the gesture detector
                // a return value of true means the detector is handling it
                // a return value of false means the detector didn't
                // recognize the event
                String action = MotionEvent.actionToString(event.getAction());
//                Log.d(TAG, "onTouch: \n" + event.toString());
                action = action.split("\\(")[0];
//                Log.d(TAG, "onTouch: \n" + action + ' ' + event.getPointerCount());
                numFingers = Math.max(event.getPointerCount(), numFingers);
                if (action.equals("ACTION_UP") && event.getEventTime() - event.getDownTime() < GestureDetector.TAP_THRESHOLD) {
                    textViewDuration.setText(String.format(Locale.ENGLISH, "Tap duration too short (%d ms)", event.getEventTime() - event.getDownTime()));
                    isTapTooShort = true;
                    return false;
                } else {
                    upTime = event.getEventTime();
                    isTapTooShort = false;
                }
                if (action.equals("ACTION_DOWN") || action.equals("ACTION_POINTER_DOWN")) {
                    downTime = event.getDownTime();
                    for (int i = 0; i < numFingers; i++) {
                        x[i].removeAllElements();
                        y[i].removeAllElements();
                    }
                    for (int i = 0; i < event.getPointerCount(); i++) {
                        x[event.getPointerId(i)].add(event.getRawX(i));
                        y[event.getPointerId(i)].add(event.getRawY(i));
                    }
                } else if (action.equals("ACTION_UP") && scrollNumber > 0 && lastScrollTime > downTime && numFingers >= MyAccessibilityService.swipeFingers) {
                    // end of scroll
                    changeToNotTouchable(overlay, MyAccessibilityService.tapDelay);
                    cv.block(100);
                    MyAccessibilityService.myAccessibilityService.performSwipe(
                            numFingers,
                            x,
                            y,
                            MyAccessibilityService.tapDelay + 50, (long) ((lastScrollTime - downTime) * scrollRatio));
                    changeToTouchable(overlay, paramsTouchable, (long) ((lastScrollTime - downTime) * scrollRatio + MyAccessibilityService.tapDelay));
                    scrollNumber = 0;
                    numFingers = 0;
                }
                textViewAction.setText(action + " at " + event.getEventTime());
                switch (action) {
                    case "ACTION_DOWN":
                        duration[0] = event.getDownTime();
                        break;
                    case "ACTION_UP":
                    case "ACTION_CANCEL":
                        duration[0] = event.getEventTime() - duration[0];
                        textViewDuration.setText("ACTION_DURATION: " + duration[0] + " ms");
                        break;
                }
                return mDetector.onTouchEvent(event);
//                return false;
            }
        };

        // Add a touch listener to the view
        // The touch listener passes all its events on to the gesture detector
        overlay.setOnTouchListener(touchListener);
        listener.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d(TAG, "onTouch: listener \n" + event);
                downNumber++;
                if (downNumber == 2) {
                    downNumber = 0;
                    changeToNotTouchable(listener, 0);
                    changeToTouchable(overlay, paramsTouchable, 0);
//                    Log.d(TAG, "onTouch: change back");
                }
                return false;
            }
        });

        overlay.setBackgroundColor(Color.parseColor("#00123567"));
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void open() {
        if (overlay.getWindowToken() == null && overlay.getParent() == null) {
            windowManager.addView(overlay, paramsTouchable);
            isTouchable = true;
//            Log.d("Window", "Added");
        }
    }

    public void activateDisablingWindow() {
        windowManager.addView(disableArea, paramDisableArea);
        disableActive = true;
        periodicToggle.run();
    }

    public void deactivateDisablingWindow() {
        try {
            handler.removeCallbacks(periodicToggle);
            windowManager.removeView(disableArea);
            disableArea.invalidate();
            disableActive = false;
        } catch (IllegalArgumentException ignore){

        }
    }

    public void close() {
        try {
            deactivateDisablingWindow();
            windowManager.removeView(overlay);
            overlay.invalidate();
            ViewGroup viewGroup = (ViewGroup) overlay.getParent();
            if (viewGroup != null) {
                viewGroup.removeAllViews();
            }
        } catch (IllegalArgumentException ignore){

        }
    }


    private void changeToNotTouchable(View view, long delay) {
        if (isTouchable) {
            isTouchable = false;
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        windowManager.removeView(view);
                    } catch (IllegalArgumentException ignore) {

                    }
                }
            }, delay);
        }
    }

    private void changeToTouchable(View view, WindowManager.LayoutParams params, long delay) {
        if (!isTouchable) {
            isTouchable = true;
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        windowManager.addView(view, params);
                    } catch (WindowManager.BadTokenException | IllegalArgumentException | IllegalStateException ignore) {

                    }
                }
            }, delay);
        }
    }

    // In the SimpleOnGestureListener subclass you should override
    // onDown and any other gesture that you want to detect.
    public class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent event) {
            Log.d("GESTURE", "onDown: \n" + event.toString());

            // don't return false here or else none of the other
            // gestures will work
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.i("GESTURE", "onSingleTapConfirmed: \n" + e.toString());
            changeToNotTouchable(overlay, MyAccessibilityService.tapDelay);
            MyAccessibilityService.myAccessibilityService.performSingleTap(
                    e.getRawX(), e.getRawY(), MyAccessibilityService.tapDelay + 50, upTime - downTime - GestureDetector.TAP_THRESHOLD);
            changeToTouchable(overlay, paramsTouchable, MyAccessibilityService.tapDelay + 150);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (isTapTooShort) return;
            Log.i("GESTURE", "onLongPress: \n" + e.toString());
            cv.block(2000);
            changeToNotTouchable(overlay, 0);
            changeToTouchable(listener, paramsListener, 0);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Can be more precise simulation, not just using fixed one.
            Log.i("GESTURE", "onDoubleTap: \n" + e.toString());
            // block to let the finger leave the screen. If the figure do not leave the screen, the simulated double tap does not work
            cv.block(200);
            changeToNotTouchable(overlay, MyAccessibilityService.tapDelay);
            MyAccessibilityService.myAccessibilityService.performDoubleTap(
                    e.getRawX(), e.getRawY(), MyAccessibilityService.tapDelay + 30, 40, downTime - e.getDownTime());
            changeToTouchable(overlay, paramsTouchable, downTime - e.getDownTime() + MyAccessibilityService.tapDelay + 30);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            scrollNumber++;
            lastScrollTime = e2.getEventTime();
            for (int i = 0; i < e2.getPointerCount(); i++) {
                x[e2.getPointerId(i)].add(e2.getRawX(i));
                y[e2.getPointerId(i)].add(e2.getRawY(i));
            }
//            Log.d("GESTURE", "onScroll: \n" + e1.toString() + "\n" + e2.toString() + "\ndistance: (" + distanceX + ", " + distanceY + ")");
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2,
                               float velocityX, float velocityY) {
            for (int i = 0; i < e2.getPointerCount(); i++) {
                x[e2.getPointerId(i)].add(e2.getRawX(i));
                y[e2.getPointerId(i)].add(e2.getRawY(i));
            }
            scrollNumber++;
            lastScrollTime = e2.getEventTime();
//            Log.d("GESTURE", "onFling: \n" + e1.toString() + "\n" + e2.toString() + "\nvelocity: (" + velocityX + ", " + velocityY + ")");
            return true;
        }
    }

    private void toggleDisableArea(boolean b) {
        if (b) {
            disableArea.setBackgroundColor(Color.parseColor("#aaff0000"));
            disableArea.setOnTouchListener(null);
        } else {
            disableArea.setBackgroundColor(Color.parseColor("#aa00ff00"));
            disableArea.setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d("TAG", "onTouch: \n" + event);
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = paramDisableArea.x;
                            initialY = paramDisableArea.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            paramDisableArea.x = initialX + (int) (event.getRawX() - initialTouchX);
                            paramDisableArea.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(disableArea, paramDisableArea);
                            return true;
                        case MotionEvent.ACTION_UP:
                    }
                    return true;
                }
            });
        }
        windowManager.updateViewLayout(disableArea, paramDisableArea);
    }
}
