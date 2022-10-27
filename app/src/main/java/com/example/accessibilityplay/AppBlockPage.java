package com.example.accessibilityplay;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

public class AppBlockPage extends AppCompatActivity {
    String packageName;
    String TAG = "AppBlockPage";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_block_page);
        packageName = getIntent().getStringExtra("package_name");
        TextView limitText = findViewById(R.id.limitText);
        limitText.setText("You've reached your limit on " + CoreService.packageNameMap.get(packageName));
        Log.d("AppBlockPage", "onCreate: " + packageName);
        Button okBtn = findViewById(R.id.okBtn);
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                okClickHandler();
            }
        });

        TextView ignoreLimitBtn = findViewById(R.id.ignoreLimitTextBtn);
        ignoreLimitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ignoreLimitHandler();
            }
        });
        String content = String.format(Locale.ENGLISH, "DEFAULT_INTERVENTION;%d;block window launch\n", System.currentTimeMillis());
        CoreService.coreService.writeToFile(content);
    }

    public void okClickHandler() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
        String content = String.format(Locale.ENGLISH, "DEFAULT_INTERVENTION;%d;go back\n", System.currentTimeMillis());
        CoreService.coreService.writeToFile(content);
        finish();
    }

    public void ignoreLimitHandler() {
        View menu = CoreService.coreService.launchIgnoreLimitMenu();
        TextView oneMore = menu.findViewById(R.id.oneMoreMinute);
        oneMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long oldValue = CoreService.appUsedTime.get(packageName);
                CoreService.appGrantedTime.put(packageName, oldValue + 60000L);
                String content = String.format(Locale.ENGLISH, "DEFAULT_INTERVENTION;%d;one more minute\n", System.currentTimeMillis());
                CoreService.coreService.writeToFile(content);
                CoreService.coreService.closeIgnoreLimitMenu(menu, packageName);
            }
        });
        TextView remindIn15 = menu.findViewById(R.id.remindIn15Min);
        remindIn15.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long oldValue = CoreService.appUsedTime.get(packageName);
                CoreService.appGrantedTime.put(packageName, oldValue + 15*60000L);
                String content = String.format(Locale.ENGLISH, "DEFAULT_INTERVENTION;%d;15 more minutes\n", System.currentTimeMillis());
                CoreService.coreService.writeToFile(content);
                CoreService.coreService.closeIgnoreLimitMenu(menu, packageName);
            }
        });
        TextView ignore = menu.findViewById(R.id.ignoreForToday);
        ignore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CoreService.appGrantedTime.put(packageName, 86400000L);
                String content = String.format(Locale.ENGLISH, "DEFAULT_INTERVENTION;%d;ignore for today\n", System.currentTimeMillis());
                CoreService.coreService.writeToFile(content);
                CoreService.coreService.closeIgnoreLimitMenu(menu, packageName);
            }
        });

        TextView cancel = menu.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = String.format(Locale.ENGLISH, "DEFAULT_INTERVENTION;%d;cancel\n", System.currentTimeMillis());
                CoreService.coreService.writeToFile(content);
                CoreService.coreService.closeIgnoreLimitMenu(menu, null);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        CoreService.isDefaultInterventionLaunched = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }
}