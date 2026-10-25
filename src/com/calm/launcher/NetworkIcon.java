package com.calm.launcher;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Wi-Fi fan with 0-3 lit bars, an Ethernet port, or a crossed-out fan when offline. */
final class NetworkIcon extends Drawable {

    static final int OFFLINE = 0, WIFI = 1, ETHERNET = 2;

    private static final int ON = 0xFFFFFFFF, OFF = 0x4DFFFFFF;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int kind = OFFLINE;
    private int bars;

    void set(int kind, int bars) {
        this.kind = kind;
        this.bars = bars;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        float s = Math.min(b.width(), b.height());
        float cx = b.exactCenterX();
        float bottom = b.exactCenterY() + s * 0.36f;
        paint.setStrokeCap(Paint.Cap.ROUND);

        if (kind == ETHERNET) {
            drawPort(canvas, cx, b.exactCenterY(), s);
            return;
        }

        // Dot, then three arcs; a lit arc means signal at least that strong.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(kind == WIFI ? ON : OFF);
        canvas.drawCircle(cx, bottom - s * 0.04f, s * 0.07f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(s * 0.09f);
        for (int i = 1; i <= 3; i++) {
            float r = s * 0.22f * i;
            paint.setColor(kind == WIFI && bars >= i ? ON : OFF);
            canvas.drawArc(new RectF(cx - r, bottom - r, cx + r, bottom + r), 225, 90, false, paint);
        }

        if (kind == OFFLINE) {
            paint.setColor(ON);
            paint.setStrokeWidth(s * 0.08f);
            canvas.drawLine(cx - s * 0.38f, b.exactCenterY() - s * 0.38f,
                    cx + s * 0.38f, b.exactCenterY() + s * 0.38f, paint);
        }
    }

    private void drawPort(Canvas canvas, float cx, float cy, float s) {
        paint.setColor(ON);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(s * 0.08f);
        float w = s * 0.34f, h = s * 0.28f;
        canvas.drawRoundRect(new RectF(cx - w, cy - h, cx + w, cy + h), s * 0.06f, s * 0.06f, paint);
        // The latch notch and pins of an RJ45 socket.
        canvas.drawLine(cx - w * 0.4f, cy - h, cx - w * 0.4f, cy - h * 1.5f, paint);
        canvas.drawLine(cx + w * 0.4f, cy - h, cx + w * 0.4f, cy - h * 1.5f, paint);
        canvas.drawLine(cx - w * 0.4f, cy - h * 1.5f, cx + w * 0.4f, cy - h * 1.5f, paint);
        paint.setStrokeWidth(s * 0.05f);
        for (int i = -1; i <= 1; i++) {
            float x = cx + i * w * 0.4f;
            canvas.drawLine(x, cy + h * 0.1f, x, cy + h * 0.55f, paint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
