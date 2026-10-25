package com.calm.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.DisplayMetrics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * The background. A chosen image is cropped to the screen size once and cached as a sharp
 * RGB_565 JPEG (~4 MB in memory at 1080p). The built-in default is a tiny blurred glow
 * stretched with filtering (~130 KB).
 */
final class Wallpaper {

    private static final int W = 240, H = 135;
    private static final int RADIUS = 6;
    private static final int DIM = 0x55000000;
    // Photos only need a light shade to keep the white clock readable.
    private static final int PHOTO_DIM = 0x33000000;

    static Drawable load(Context context) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bm = BitmapFactory.decodeFile(cache(context).getPath(), opts);
        boolean photo = bm != null;
        if (!photo) bm = makeDefault();
        BitmapDrawable image = new BitmapDrawable(context.getResources(), bm);
        image.setFilterBitmap(true);
        return new LayerDrawable(new Drawable[]{image, new ColorDrawable(photo ? PHOTO_DIM : DIM)});
    }

    /** Crops the image at path to the screen and caches it. Slow-ish: call off the main thread. */
    static boolean apply(Context context, String path) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int sw = Math.max(dm.widthPixels, dm.heightPixels);
        int sh = Math.min(dm.widthPixels, dm.heightPixels);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opts);
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return false;
        int sample = 1;
        while (opts.outWidth / (sample * 2) >= sw && opts.outHeight / (sample * 2) >= sh) sample *= 2;
        opts.inJustDecodeBounds = false;
        opts.inSampleSize = sample;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap src = BitmapFactory.decodeFile(path, opts);
        if (src == null) return false;

        Bitmap screen = Bitmap.createBitmap(sw, sh, Bitmap.Config.RGB_565);
        new Canvas(screen).drawBitmap(src, centerCrop(src.getWidth(), src.getHeight(), sw, sh),
                new Rect(0, 0, sw, sh), new Paint(Paint.FILTER_BITMAP_FLAG));
        src.recycle();
        boolean ok = save(context, screen);
        screen.recycle();
        return ok;
    }

    static void reset(Context context) {
        //noinspection ResultOfMethodCallIgnored
        cache(context).delete();
    }

    private static File cache(Context context) {
        return new File(context.getFilesDir(), "wallpaper.jpg");
    }

    private static boolean save(Context context, Bitmap bm) {
        try (FileOutputStream out = new FileOutputStream(cache(context))) {
            return bm.compress(Bitmap.CompressFormat.JPEG, 95, out);
        } catch (IOException e) {
            return false;
        }
    }

    private static Rect centerCrop(int w, int h, int tw, int th) {
        if ((long) w * th > (long) h * tw) {
            int cw = h * tw / th;
            return new Rect((w - cw) / 2, 0, (w + cw) / 2, h);
        }
        int ch = w * th / tw;
        return new Rect(0, (h - ch) / 2, w, (h + ch) / 2);
    }

    /** Soft blue and violet glows on night blue. */
    private static Bitmap makeDefault() {
        Bitmap bm = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        c.drawColor(Color.rgb(12, 20, 36));
        glow(c, 50, 30, 110, Color.rgb(46, 84, 160));
        glow(c, 200, 110, 120, Color.rgb(110, 54, 140));
        glow(c, 160, 10, 70, Color.rgb(24, 112, 120));
        blur(bm);
        return bm;
    }

    private static void glow(Canvas c, float x, float y, float r, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new RadialGradient(x, y, r, color, color & 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, r, p);
    }

    /** Three box-blur passes approximate a gaussian. */
    private static void blur(Bitmap bm) {
        int[] px = new int[W * H];
        int[] tmp = new int[W * H];
        bm.getPixels(px, 0, W, 0, 0, W, H);
        for (int i = 0; i < 3; i++) {
            pass(px, tmp, true);
            pass(tmp, px, false);
        }
        bm.setPixels(px, 0, W, 0, 0, W, H);
    }

    private static void pass(int[] in, int[] out, boolean horizontal) {
        int n = RADIUS * 2 + 1;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int r = 0, g = 0, b = 0;
                for (int k = -RADIUS; k <= RADIUS; k++) {
                    int xx = horizontal ? clamp(x + k, W) : x;
                    int yy = horizontal ? y : clamp(y + k, H);
                    int c = in[yy * W + xx];
                    r += (c >> 16) & 0xFF;
                    g += (c >> 8) & 0xFF;
                    b += c & 0xFF;
                }
                out[y * W + x] = 0xFF000000 | ((r / n) << 16) | ((g / n) << 8) | (b / n);
            }
        }
    }

    private static int clamp(int v, int size) {
        return v < 0 ? 0 : (v >= size ? size - 1 : v);
    }
}
