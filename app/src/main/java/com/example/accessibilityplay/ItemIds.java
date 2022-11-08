package com.example.accessibilityplay;

import android.os.CountDownTimer;
import android.util.Log;

public class ItemIds {
    int textId;
    int pause15;
    int pauseToday;
    int timePermitted;
    CountDownTimer countDownTimer;

    ItemIds(int w, int x, int y, int z) {
        textId = w;
        pause15 = x;
        pauseToday = y;
        timePermitted = z;
    }

}
