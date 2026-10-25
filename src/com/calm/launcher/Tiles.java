package com.calm.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Rounded glass tiles that grow and outline themselves when the remote lands on them. */
final class Tiles {

    static final class Holder {
        final FrameLayout box;
        final ImageView icon;
        final TextView label;

        Holder(FrameLayout box, ImageView icon, TextView label) {
            this.box = box;
            this.icon = icon;
            this.label = label;
        }
    }

    private static final float FOCUS_SCALE = 1.14f;

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    /** A tile: icon box with a label below. Dock tiles hide the label until focused. */
    static LinearLayout make(Context c, int boxDp, boolean alwaysLabel) {
        LinearLayout tile = new LinearLayout(c);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        tile.setClipChildren(false);

        FrameLayout box = new FrameLayout(c);
        box.setBackground(background(c, false));
        ImageView icon = new ImageView(c);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int iconPx = dp(c, boxDp * 0.58f);
        box.addView(icon, new FrameLayout.LayoutParams(iconPx, iconPx, Gravity.CENTER));
        tile.addView(box, new LinearLayout.LayoutParams(dp(c, boxDp), dp(c, boxDp)));

        TextView label = new TextView(c);
        label.setTextColor(0xFFFFFFFF);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, alwaysLabel ? 14 : 17);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER);
        label.setShadowLayer(6, 0, 1, 0xAA000000);
        label.setPadding(0, dp(c, 10), 0, 0);
        label.setMaxWidth(dp(c, boxDp + 40));
        label.setVisibility(alwaysLabel ? View.VISIBLE : View.INVISIBLE);
        tile.addView(label, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        tile.setPivotY(dp(c, boxDp) / 2f);
        tile.setTag(new Holder(box, icon, label));
        return tile;
    }

    static void setFocused(View tile, boolean focused, boolean alwaysLabel) {
        Holder h = (Holder) tile.getTag();
        h.box.setBackground(background(tile.getContext(), focused));
        if (!alwaysLabel) h.label.setVisibility(focused ? View.VISIBLE : View.INVISIBLE);
        float s = focused ? FOCUS_SCALE : 1f;
        tile.animate().scaleX(s).scaleY(s).setDuration(140).start();
    }

    static void reset(View tile) {
        Holder h = (Holder) tile.getTag();
        h.box.setBackground(background(tile.getContext(), false));
        tile.animate().cancel();
        tile.setScaleX(1f);
        tile.setScaleY(1f);
    }

    private static Drawable background(Context c, boolean focused) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(c, 22));
        d.setColor(focused ? 0x4DFFFFFF : 0x24FFFFFF);
        d.setStroke(dp(c, focused ? 3 : 1), focused ? 0xFFFFFFFF : 0x33FFFFFF);
        return d;
    }

    /** A 3x3 grid of dots for the "Apps" tile. */
    static final class Dots extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Dots() {
            paint.setColor(0xFFFFFFFF);
        }

        @Override
        public void draw(Canvas canvas) {
            float size = Math.min(getBounds().width(), getBounds().height());
            float step = size / 3f;
            float r = step * 0.18f;
            float left = getBounds().exactCenterX() - size / 2f;
            float top = getBounds().exactCenterY() - size / 2f;
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    canvas.drawCircle(left + step * (i + 0.5f), top + step * (j + 0.5f), r, paint);
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
}
