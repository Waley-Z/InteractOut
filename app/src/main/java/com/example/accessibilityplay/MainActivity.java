package com.example.accessibilityplay;

import static android.content.ContentValues.TAG;

import static com.example.accessibilityplay.MyAccessibilityService.myAccessibilityService;

import androidx.appcompat.app.AppCompatActivity;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ServiceInfo;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    final static String TAG = "LALALA";
    boolean isAccessibilityServiceRunning = false;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyAccessibilityService.myAccessibilityService.disableSelf();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ContentResolver contentResolver = getContentResolver();
        Log.d(TAG, "onCreate: accessibility status is " + Settings.Secure.getString(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED));
        String status = Settings.Secure.getString(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED);
        if (status.equals("0")) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
//        Intent intent = new Intent(this, MyAccessibilityService.class);
//        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        Button btn = findViewById(R.id.button);
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        Configurations.configuration.setStatusBarHeight(result);
        Log.d(TAG, "onCreate: " + result);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            final WindowInsetsController insetsController = getWindow().getInsetsController();
//            if (insetsController != null) {
//                insetsController.hide(WindowInsets.Type.statusBars());
//            }
//        }
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                myAccessibilityService.performClick(559, 1656);
//                Log.d(TAG, "onClick: Button clicked");
                MyAccessibilityService.myAccessibilityService.disableSelf();

            }
        });
    }
//
//    private final ServiceConnection connection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
//            MyAccessibilityService.MyBinder myBinder = (MyAccessibilityService.MyBinder) iBinder;
//            myAccessibilityService = myBinder.getService();
//            isAccessibilityServiceRunning = true;
//            Log.d(TAG, "onServiceConnected: connected");
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName componentName) {
//            isAccessibilityServiceRunning = false;
//            Log.d(TAG, "onServiceDisconnected: disconnected");
//        }
//    };

}