package com.wzjing.interview.jni;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

public class TextFilter {

    private final String TAG = TextFilter.class.getSimpleName();

    private Bitmap bitmap;

    public TextFilter(int width, int height) {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void drawText(String text, int size) {
        if (bitmap.isMutable()) {
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setTextSize(size);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(text, canvas.getWidth() / 2, canvas.getHeight() / 2, paint);
        } else {
            Log.e(TAG, "bitmap is not mutable");
        }
    }

    public void free() {
        if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
    }
}
