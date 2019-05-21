package com.wzjing.interview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

public class VideoEditor {
    private static Paint textPaint;

    public static void drawText(int[] rgba, int width, int height, String text) {
        Log.d("VideoEditor", "array: "+rgba.length);
        Bitmap bitmap = Bitmap.createBitmap(rgba, width, height, Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(bitmap);
//        if (textPaint == null) {
//            textPaint = new Paint();
//        }
//        float textWidth = textPaint.measureText(text);
//        textPaint.setTextAlign(Paint.Align.CENTER);
//        textPaint.setColor(Color.WHITE);
//        textPaint.setTextSize(30);
//        canvas.drawText(text, width/2, height/2, textPaint);
//        bitmap.recycle();
    }
}
