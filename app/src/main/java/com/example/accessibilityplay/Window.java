package com.example.accessibilityplay;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.ConditionVariable;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

public class Window {
    private View view;
    private WindowManager windowManager;
    private WindowManager.LayoutParams paramsTouchable;
    private final static String TAG = "LALALA";
    private ConditionVariable cv = new ConditionVariable(false);
    private boolean isTouchable;
    private long downTime;
    private GestureDetector mDetector;


    public Window(Context context) {
        paramsTouchable = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                        WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSPARENT);
        paramsTouchable.gravity = Gravity.CENTER;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = layoutInflater.inflate(R.layout.overlay_window, null);
        TextView textViewAction = (TextView) view.findViewById(R.id.textView2);
        TextView textViewDuration = (TextView) view.findViewById(R.id.textView3);
        final long[] duration = new long[1];

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
//                Log.d(TAG, "onTouch: \n" + event.toString());
                String action = MotionEvent.actionToString(event.getAction());
                Log.d(TAG, "onTouch: \n" + event);
                if (action.equals("ACTION_DOWN")) {
                    downTime = event.getDownTime();
                }
                textViewAction.setText(action + " at " + event.getEventTime());
                switch (action) {
                    case "ACTION_DOWN":
                        duration[0] = event.getDownTime();
                        break;
                    case "ACTION_UP":
                        duration[0] = event.getEventTime() - duration[0];
                        textViewDuration.setText("ACTION_DURATION: " + duration[0] + " ms");
                        break;
                    case "ACTION_CANCEL":
                        duration[0] = event.getEventTime() - duration[0];
                        textViewDuration.setText("ACTION_DURATION: " + duration[0] + " ms");
                        break;
                }
                return mDetector.onTouchEvent(event);

            }
        };

        // Add a touch listener to the view
        // The touch listener passes all its events on to the gesture detector
        view.setOnTouchListener(touchListener);
//        view.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent motionEvent) {
//                int action = motionEvent.getAction();
//                Log.d(TAG, "onTouch: \n" + motionEvent);
//                v.performClick();
//                return false;
//            }
//        });

        view.setBackgroundColor(Color.parseColor("#aa123567"));
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void open() {
        if (view.getWindowToken() == null && view.getParent() == null) {
//            Handler handler = new Handler();
//            handler.postDelayed(() -> {
            windowManager.addView(view, paramsTouchable);
            isTouchable = true;

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


    private void changeToNotTouchable(long delay) {
        if (isTouchable) {
            isTouchable = false;
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    windowManager.removeView(view);
                }
            }, delay);
        }
    }

    private void changeToTouchable(long delay) {
        if (!isTouchable) {
            isTouchable = true;
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    windowManager.addView(view, paramsTouchable);
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
            changeToNotTouchable(950);
            MyAccessibilityService.myAccessibilityService.performSingleTap(
                    e.getX(), e.getY(), 1000, 50);
            changeToTouchable(1000);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Log.i("GESTURE", "onLongPress: \n" + e.toString());
//            Log.d(TAG, "onLongPress: Before isTouchable " + isTouchable);
//            MyAccessibilityService.myAccessibilityService.performClick(
//                    500, 1000, 1000, 400);
//            Log.d(TAG, "onLongPress: After isTouchable " + isTouchable);
//            cv.block(1000);
//            overlayChangeTouchable(paramsTouchable, "onLongPress");
            cv.block(2000);
            changeToNotTouchable(0);
            // Use handler to perform delay
//            Handler handler = new Handler();
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    MyAccessibilityService.myAccessibilityService.performSingleTap(
//                            e.getX(), e.getY(), 0, 5000);
//                }
//            }, 200);
//            changeToTouchable(6000);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Can be more precise simulation, not just using fixed one.
            Log.i("GESTURE", "onDoubleTap: \n" + e.toString());
            // block to let the finger leave the screen. If the figure do not leave the screen, the simulated double tap does not work
            cv.block(200);
            changeToNotTouchable(0);
            MyAccessibilityService.myAccessibilityService.performDoubleTap(
                    e.getX(), e.getY(), 30, 40, downTime - e.getDownTime());
            changeToTouchable(1200);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            Log.d("GESTURE", "onScroll: \n" + e1.toString() + "\n" + e2.toString() + "\ndistance: (" + distanceX + ", " + distanceY + ")");
//            overlayChangeTouchable(paramsTouchable, "onScroll");
            return true;
        }

        @Override
        public boolean onContextClick(MotionEvent e) {
            Log.d(TAG, "onContextClick: \n" + e.toString());
            return super.onContextClick(e);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            Log.d(TAG, "onDoubleTapEvent: \n" + e.toString());
            return true;
        }


        @Override
        public void onShowPress(MotionEvent e) {
            Log.d(TAG, "onShowPress: \n" + e);
            super.onShowPress(e);
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.d(TAG, "onSingleTapUp: \n" + e.toString());
            return super.onSingleTapUp(e);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2,
                               float velocityX, float velocityY) {
            Log.d("GESTURE", "onFling: \n" + e1.toString() + "\n" + e2.toString() + "\nvelocity: (" + velocityX + ", " + velocityY + ")");
//            overlayChangeTouchable(paramsTouchable, "onFling");
            return true;
        }
    }
}
