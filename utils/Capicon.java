package com.node.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;

/**
 * Created by tnantoka on 8/11/16.
 */
public class Capicon {
    public static class Colors {
        public static int RED = Color.parseColor("#f44336");
        public static int PINK = Color.parseColor("#e91e63");
        public static int PURPLE = Color.parseColor("#9c27b0");
        public static int DEEP_PURPLE = Color.parseColor("#673ab7");
        public static int INDIGO = Color.parseColor("#3f51b5");
        public static int BLUE = Color.parseColor("#2196f3");
        public static int LIGHT_BLUE = Color.parseColor("#03a9f4");
        public static int CYAN = Color.parseColor("#00bcd4");
        public static int TEAL = Color.parseColor("#009688");
        public static int GREEN = Color.parseColor("#4caf50");
        public static int LIGHT_GREEN = Color.parseColor("#8bc34a");
        public static int LIME = Color.parseColor("#cddc39");
        public static int YELLOW = Color.parseColor("#ffeb3b");
        public static int AMBER = Color.parseColor("#ffc107");
        public static int ORANGE = Color.parseColor("#ff9800");
        public static int DEEP_ORANGE = Color.parseColor("#ff5722");
        public static int BROWN = Color.parseColor("#795548");
        public static int GREY = Color.parseColor("#9e9e9e");
        public static int BLUE_GREY = Color.parseColor("#607d8b");
        public static int BLACK = Color.parseColor("#000000");
        public static int WHITE = Color.parseColor("#ffffff");
    }

    int width;
    int height;

    public int backgroundColor = Colors.INDIGO;
    public int textColor = Colors.WHITE;
    public String string = "";
    public Typeface typeface = Typeface.DEFAULT;
    public float textScale = 0.7f;
    public float roundScale = 0.0f;

    private Paint paint = new Paint();

    public Capicon(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Capicon(int size) {
        this.width = size;
        this.height = size;
    }

    public Bitmap bitmap() {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        paint.setColor(backgroundColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.drawRoundRect(0, 0, width, height, width * roundScale, height * roundScale, paint);
        } else {
            canvas.drawRect(0, 0, width, height, paint);
        }

        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);

        paint.setTypeface(typeface);
        paint.setColor(textColor);

        paint.setTextSize(width * textScale);

        int centerX = width / 2;
        int centerY = height / 2;
        float x = centerX;

//        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
//        float y = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2;
        Rect bounds = new Rect();
        paint.getTextBounds(string, 0, string.length(), bounds);
        int height = bounds.height();
        float y = centerY + height / 2;

        canvas.drawText(string, x, y, paint);

        canvas.drawBitmap(bitmap, 0, 0, null);

        return bitmap;
    }
}
