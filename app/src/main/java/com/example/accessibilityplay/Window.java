package com.example.accessibilityplay;

import static android.content.Context.MODE_APPEND;
import static com.example.accessibilityplay.CoreService.currentForegroundPackage;
import static com.example.accessibilityplay.CoreService.isInLabMode;
import static com.example.accessibilityplay.CoreService.isInTutorial;
import static com.example.accessibilityplay.CoreService.isLongpressListenerOn;
import static com.example.accessibilityplay.CoreService.isOverlayOn;
import static com.example.accessibilityplay.CoreService.participantFilename;
import static com.example.accessibilityplay.CoreService.prolongNoteShowTime;
import static com.example.accessibilityplay.CoreService.scrollRatio;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
//import com.example.accessibilityplay.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.Vector;

public class Window {
    private final View overlay;
    private final View listener;
    private final View disableArea;
    private WindowManager windowManager;
    private final WindowManager.LayoutParams paramsTouchable, paramsTutorial;
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
            disableActive = !disableActive;
            toggleDisableArea(disableActive);
//            handler.postDelayed(periodicToggle, 5000);
        }
    };


    public Window(Context context) {
        Log.d(TAG, "Window: Window Created");
        paramsTutorial = new WindowManager.LayoutParams(1080, 1700, 0, 194,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                        WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSPARENT);
        paramsTutorial.gravity = Gravity.TOP;
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
//                Log.d(TAG, "onTouch: \n" + event);
                action = action.split("\\(")[0];
//                Log.d(TAG, "onTouch: \n" + action + ' ' + event.getPointerCount());
                numFingers = Math.max(event.getPointerCount(), numFingers);
//                    Log.d(TAG, "onTouch: \n" + scrollNumber);
                if (action.equals("ACTION_UP") && scrollNumber <= 1 && event.getEventTime() - event.getDownTime() < GestureDetector.TAP_THRESHOLD) {
                    String content = String.format(Locale.ENGLISH, "USER_SINGLE_TAP_TOO_SHORT;%d;%d\n", System.currentTimeMillis(), event.getEventTime() - event.getDownTime());
                    CoreService.coreService.writeToFile(participantFilename, content, MODE_APPEND);
                    textViewDuration.setText(String.format(Locale.ENGLISH, "Tap duration too short (%d/%d ms)", event.getEventTime() - event.getDownTime(), GestureDetector.TAP_THRESHOLD));
                    if (CoreService.prolongNoteShowTime > 0) {
                        CoreService.prolongNoteShowTime --;
                        Toast.makeText(context, String.format(Locale.ENGLISH, "Tap duration too short (%d/%d ms)", event.getEventTime() - event.getDownTime(), GestureDetector.TAP_THRESHOLD), Toast.LENGTH_SHORT).show();
                    }
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
                } else if (action.equals("ACTION_UP") && scrollNumber > 0 && lastScrollTime > downTime && numFingers >= CoreService.swipeFingers) {
                    // end of scroll
                    String content = String.format(Locale.ENGLISH, "USER_SWIPE;%d\n", System.currentTimeMillis());
                    CoreService.coreService.writeToFile(participantFilename, content, MODE_APPEND);
                    changeToNotTouchable(overlay, CoreService.swipeDelay);
//                    cv.block(100);
                    WindowManager.LayoutParams params = (isInTutorial) ? paramsTutorial : paramsTouchable;
                    CoreService.coreService.performSwipe(
                            numFingers,
                            x,
                            y,
                            50, (long) ((lastScrollTime - downTime) * scrollRatio));
                    changeToTouchable(overlay, params, (long) ((lastScrollTime - downTime) * scrollRatio + 100));
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
//                    MyAccessibilityService.isLongpressListenerOn = false;
//                    MyAccessibilityService.isOverlayOn = true;
//                    Log.d(TAG, "onTouch: change back");
                    Toast.makeText(CoreService.coreService, "Interventions are enabled.", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

        overlay.setBackgroundColor(Color.parseColor("#00123567"));
    }

    public void open(Context context) {
        if (overlay.getWindowToken() == null && overlay.getParent() == null) {
            windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (isInTutorial) {
                overlay.setBackgroundColor(Color.parseColor("#3b123456"));
                windowManager.addView(overlay, paramsTutorial);
                Log.d(TAG, "open: tutorial");
            } else {
                overlay.setBackgroundColor(Color.parseColor("#00123567"));
                windowManager.addView(overlay, paramsTouchable);
                Log.d(TAG, "open: normal");
            }
            isTouchable = true;
            Log.d("Window", "Added");

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
        if (windowManager == null) return;
        deactivateDisablingWindow();
        try {
            windowManager.removeViewImmediate(overlay);
            overlay.invalidate();
            ViewGroup viewGroup = (ViewGroup) overlay.getParent();
            if (viewGroup != null) {
                viewGroup.removeAllViews();
            }
            Log.d(TAG, "close: windows closed");
        } catch (IllegalArgumentException ignore){

        }
        try {
            Log.d(TAG, "close: listener closed");
            windowManager.removeViewImmediate(listener);
            listener.invalidate();
            ViewGroup viewGroup = (ViewGroup) listener.getParent();
            if (viewGroup != null) {
                viewGroup.removeAllViews();
            }
        } catch (IllegalArgumentException ignore) {

        }
        windowManager = null;
        isTouchable = false;
    }


    private void changeToNotTouchable(View view, long delay) {
        Log.d(TAG, "changeToNotTouchable: " + isTouchable);
        ConditionVariable conditionVariable = new ConditionVariable();

        if (view == overlay && isOverlayOn) {
            if (isTouchable) {
                isTouchable = false;
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "run: cv open called");
                                try {
                                    windowManager.removeView(view);
                                    CoreService.isOverlayOn = false;
                                    Log.d(TAG, "changeToNotTouchable: " + CoreService.isOverlayOn + ' ' + CoreService.isLongpressListenerOn);
                                } catch (IllegalArgumentException ignore) {

                                } catch (NullPointerException ig) {
                                    Log.d(TAG, "changeToNotTouchable: window is null");
                                } finally {
                                    conditionVariable.open();
                                }
                            }
                        }, delay);
                        Looper.loop();
                    }
                };
                thread.start();
                Log.d(TAG, "changeToNotTouchable: is overlay view");
            }
        } else if (view == listener && isLongpressListenerOn) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                windowManager.removeView(view);
                                CoreService.isLongpressListenerOn = false;
                                Log.d(TAG, "changeToNotTouchable: " + CoreService.isOverlayOn + ' ' + CoreService.isLongpressListenerOn);
                            } catch (IllegalArgumentException ignore) {

                            } catch (NullPointerException ig) {
                                Log.d(TAG, "changeToNotTouchable: window is null");
                            } finally {
                                conditionVariable.open();
                            }
                        }
                    }, delay);
                    Looper.loop();
                }
            };
            thread.start();
        }
        Log.d(TAG, "changeToNotTouchable: cv block called");
        conditionVariable.block(1000);
    }

    private void changeToTouchable(View view, WindowManager.LayoutParams params, long delay) {
        Log.d(TAG, "changeToTouchable: " + isTouchable);
        if (view == overlay && !isOverlayOn) {
            if (!isTouchable) {
                isTouchable = true;
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            windowManager.addView(view, params);
                            CoreService.isOverlayOn = true;
                            Log.d(TAG, "changeToTouchable: " + CoreService.isOverlayOn + ' ' + CoreService.isLongpressListenerOn);
                        } catch (WindowManager.BadTokenException | IllegalArgumentException | IllegalStateException | NullPointerException ignore) {
                            Log.d(TAG, "changeToTouchable: window is null");

                        }
                    }
                }, delay);
            }
        } else if (view == listener && !isLongpressListenerOn) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        windowManager.addView(view, params);
                        CoreService.isLongpressListenerOn = true;
                        Log.d(TAG, "changeToTouchable: " + CoreService.isOverlayOn + ' ' + CoreService.isLongpressListenerOn);
                    } catch (WindowManager.BadTokenException | IllegalArgumentException | IllegalStateException | NullPointerException ignore) {
                        Log.d(TAG, "changeToTouchable: window is null");

                    }
                }
            }, delay);
        }
    }


    // In the SimpleOnGestureListener subclass you should override
    // onDown and any other gesture that you want to detect.
    public class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        TextView detectedEvent = overlay.findViewById(R.id.textView4);

        @Override
        public boolean onDown(MotionEvent event) {
//            Log.d("GESTURE", "onDown: \n" + event.toString());

            // don't return false here or else none of the other
            // gestures will work
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (CoreService.minimumFingerToTap > 1 || CoreService.isDoubleTapToSingleTap) return true;
            String content = String.format(Locale.ENGLISH, "USER_SINGLE_TAP;%d;%s\n", System.currentTimeMillis(), e.toString());
            CoreService.coreService.writeToFile(participantFilename, content, MODE_APPEND);
            WindowManager.LayoutParams params = (isInTutorial) ? paramsTutorial : paramsTouchable;
            detectedEvent.setText("Single tap");
//            Log.i("GESTURE", "onSingleTapConfirmed: \n" + e.toString());
            changeToNotTouchable(overlay, CoreService.tapDelay);
            long tapDuration = Math.max(upTime - downTime - GestureDetector.TAP_THRESHOLD, 40);
            CoreService.coreService.performSingleTap(
                    e.getRawX(), e.getRawY(), 50, tapDuration);
            changeToTouchable(overlay, params, tapDuration + 60);
            return true;
        }

        @Override
        public boolean onMultifingerTap(MotionEvent e) {
            if (CoreService.minimumFingerToTap == 1) return true;
            String content = String.format(Locale.ENGLISH, "USER_MULTIFINGER_TAP;%d;%s\n", System.currentTimeMillis(), e.toString());
            CoreService.coreService.writeToFile(participantFilename, content, MODE_APPEND);
            WindowManager.LayoutParams params = (isInTutorial) ? paramsTutorial : paramsTouchable;
            detectedEvent.setText("Multifinger tap");
//            Log.d(TAG, "onMultifingerTap: " + e);
            changeToNotTouchable(overlay, CoreService.tapDelay);
            CoreService.coreService.performSingleTap(
                    e.getRawX(), e.getRawY(), CoreService.tapDelay + 50, e.getEventTime() - e.getDownTime() - GestureDetector.TAP_THRESHOLD);
            changeToTouchable(overlay, params, CoreService.tapDelay + 150);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Log.d(TAG, "onLongPress: \n" + currentForegroundPackage);
            Log.i("GESTURE", "onLongPress: \n" + e.toString());
            String content = String.format(Locale.ENGLISH, "USER_LONG_PRESS;%d\n", System.currentTimeMillis());
            CoreService.coreService.writeToFile(participantFilename, content, MODE_APPEND);
            if (isInLabMode) {
                if (CoreService.prolongNoteShowTime > 0) {
                    prolongNoteShowTime --;
                    Toast.makeText(CoreService.coreService, "Your tap is too long, please tap shorter", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            if (isTapTooShort || currentForegroundPackage.equals("com.google.android.apps.nexuslauncher")) return;
            detectedEvent.setText("Long press");
//            cv.block(2000);
            changeToNotTouchable(overlay, 0);
            changeToTouchable(listener, paramsListener, 0);
//            MyAccessibilityService.isLongpressListenerOn = true;
//            MyAccessibilityService.isOverlayOn = false;
            Toast.makeText(CoreService.coreService, "Interventions are temporarily disabled, you can long press for one time.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Can be more precise simulation, not just using fixed one.
//            Log.i("GESTURE", "onDoubleTap: \n" + e.toString());
            String content = String.format(Locale.ENGLISH, "USER_DOUBLE_TAP;%d;%s\n", System.currentTimeMillis(), e.toString());
            CoreService.coreService.writeToFile(participantFilename, content, MODE_APPEND);
            WindowManager.LayoutParams params = (isInTutorial) ? paramsTutorial : paramsTouchable;
            if (CoreService.isDoubleTapToSingleTap) {
                cv.block(100);
                changeToNotTouchable(overlay, CoreService.tapDelay);
                CoreService.coreService.performSingleTap(e.getRawX(), e.getRawY(), 30, 50);
                changeToTouchable(overlay, params, 100);
            } else {
                detectedEvent.setText("Double tap");
                // block to let the finger leave the screen. If the figure do not leave the screen, the simulated double tap does not work
                cv.block(200);
                changeToNotTouchable(overlay, CoreService.tapDelay);
                CoreService.coreService.performDoubleTap(
                        e.getRawX(), e.getRawY(), 30, 40, downTime - e.getDownTime());
                changeToTouchable(overlay, params, downTime - e.getDownTime() + CoreService.tapDelay + 50);
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            if (e1.getToolType(0) == MotionEvent.TOOL_TYPE_UNKNOWN || e2.getToolType(0) == MotionEvent.TOOL_TYPE_UNKNOWN) {
                return true;
            }
            scrollNumber++;
            lastScrollTime = e2.getEventTime();
            detectedEvent.setText("Scroll");
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
            if (e1.getToolType(0) == MotionEvent.TOOL_TYPE_UNKNOWN || e2.getToolType(0) == MotionEvent.TOOL_TYPE_UNKNOWN) {
                return true;
            }
            for (int i = 0; i < e2.getPointerCount(); i++) {
                x[e2.getPointerId(i)].add(e2.getRawX(i));
                y[e2.getPointerId(i)].add(e2.getRawY(i));
            }
            detectedEvent.setText("Fling");
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
