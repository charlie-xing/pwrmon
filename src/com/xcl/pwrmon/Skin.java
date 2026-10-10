package com.xcl.pwrmon;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** One complete look for the dashboard: draws every frame and its own theme picker. */
abstract class Skin {
    final String id, name, family;
    protected float u = 1f;
    protected final Paint pT = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected final Paint pF = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected final Paint pS = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected final Path path = new Path();
    protected final RectF oval = new RectF();
    protected final Matrix matrix = new Matrix();
    /** Tap target that opens the theme picker, laid out by {@link #draw}. */
    final RectF menuButton = new RectF();

    Skin(String id, String name, String family) {
        this.id = id;
        this.name = name;
        this.family = family;
        pS.setStyle(Paint.Style.STROKE);
    }

    abstract void draw(Canvas c, Battery b, float w, float h, int insetTop, int insetBottom, float t);

    /** Draws the picker over the dashboard and stores each entry's tap area in {@code hits}. */
    abstract void drawPicker(Canvas c, Skin[] all, int current, RectF[] hits, float w, float h,
                             int insetTop, int insetBottom, float t);

    /** A few colours that represent this skin in a picker. */
    abstract int[] swatch();

    /** Whether the system bar icons should be dark. */
    boolean light() {
        return false;
    }

    /** Taps that are not the menu button; returns true if handled. */
    boolean onTap(float x, float y) {
        return false;
    }

    static int alpha(int color, int a) {
        return (color & 0x00FFFFFF) | (a << 24);
    }

    static int mix(int a, int b, float f) {
        f = Math.max(0f, Math.min(1f, f));
        int r = (int) (((a >> 16) & 0xFF) * (1 - f) + ((b >> 16) & 0xFF) * f);
        int g = (int) (((a >> 8) & 0xFF) * (1 - f) + ((b >> 8) & 0xFF) * f);
        int bl = (int) ((a & 0xFF) * (1 - f) + (b & 0xFF) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    protected void resetStroke() {
        pS.setShader(null);
        pS.setPathEffect(null);
        pS.clearShadowLayer();
        pS.setStrokeCap(Paint.Cap.BUTT);
    }
}
