package com.example.accessibilityplay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import android.view.View;


public class MyView extends View {
    private final static String TAG = "LALALA";
    Paint paint = new Paint();
    Path path = new Path();

    public MyView(Context context) {
        super(context);
    }

    public void setBackgroundResource(int resid) {
        super.setBackgroundResource(resid);
    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#aa123456"));
        c.drawPaint(paint);
        path.moveTo(150, 150);
        path.lineTo(150, 200);
//        path.close();
        paint.setStrokeWidth(13);
//        paint.setPathEffect(null);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        c.drawCircle(150, 150, 6, paint);
        c.drawPath(path, paint);
        c.drawCircle(150, 200, 6, paint);
        Log.d(TAG, "onDraw: DRAWED");
    }
}

