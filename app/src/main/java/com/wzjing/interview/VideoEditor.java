package com.wzjing.interview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class VideoEditor {
    private static Paint textPaint;

    public static void drawText(int[] rgba, int width, int height, String text) {
        Bitmap bitmap = Bitmap.createBitmap(rgba, width, height, null);
        Canvas canvas = new Canvas(bitmap);
        if (textPaint == null) {
            textPaint = new Paint();
        }
//        float textWidth = textPaint.measureText(text);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30);
        canvas.drawText(text, width/2, height/2, textPaint);
        bitmap.recycle();
    }
}
