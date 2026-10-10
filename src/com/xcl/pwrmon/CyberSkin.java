package com.xcl.pwrmon;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.os.Build;

import java.util.Locale;

/** The original neon / CRT look. */
final class CyberSkin extends Skin {
    private static final int CYAN = 0xFF00F0FF;
    private static final int MAGENTA = 0xFFFF2A6D;
    private static final int YELLOW = 0xFFFCEE0A;
    private static final int GREEN = 0xFF39FF14;
    private static final int RED = 0xFFFF3B3B;
    private static final int TEXT = 0xFFE2F9FF;
    private static final int DIM = 0xFF64748F;

    private final Typeface mono = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
    private final Typeface label = Typeface.create("sans-serif-condensed", Typeface.BOLD);

    private Battery b;
    private int accent = CYAN, accent2 = MAGENTA, tempColor = TEXT;

    CyberSkin() {
        super("cyberpunk", "Cyberpunk", "PWR//MON");
    }

    @Override
    int[] swatch() {
        return new int[]{CYAN, MAGENTA, YELLOW, GREEN};
    }

    private void pickColors() {
        switch (b.state) {
            case Battery.FULL: accent = GREEN; accent2 = CYAN; break;
            case Battery.CHARGING: accent = CYAN; accent2 = MAGENTA; break;
            case Battery.DISCHARGING: accent = MAGENTA; accent2 = CYAN; break;
            case Battery.LOW: accent = RED; accent2 = YELLOW; break;
            default: accent = YELLOW; accent2 = MAGENTA;
        }
        tempColor = b.tempLevel == Battery.TEMP_HOT ? RED : b.tempLevel == Battery.TEMP_WARM ? YELLOW : TEXT;
    }

    private void txt(Canvas c, String s, float x, float y, float size, int color, Typeface tf,
                     Paint.Align align, int glow) {
        pT.setShader(null);
        pT.setTypeface(tf);
        pT.setTextSize(size);
        pT.setColor(color);
        pT.setTextAlign(align);
        pT.setLetterSpacing(tf == label ? 0.14f : 0f);
        if (glow != 0) pT.setShadowLayer(size * 0.45f, 0, 0, glow);
        else pT.clearShadowLayer();
        c.drawText(s, x, y, pT);
    }

    /** Panel with the top-left and bottom-right corners cut off. */
    private void panel(Canvas c, float x0, float y0, float x1, float y1, int color) {
        float cut = 10 * u;
        path.rewind();
        path.moveTo(x0 + cut, y0);
        path.lineTo(x1, y0);
        path.lineTo(x1, y1 - cut);
        path.lineTo(x1 - cut, y1);
        path.lineTo(x0, y1);
        path.lineTo(x0, y0 + cut);
        path.close();
        pF.setShader(null);
        pF.clearShadowLayer();
        pF.setColor(0xB30A0E18);
        c.drawPath(path, pF);
        resetStroke();
        pS.setStrokeWidth(1 * u);
        pS.setColor(alpha(color, 0x55));
        c.drawPath(path, pS);
        // accent notches
        pS.setStrokeWidth(2 * u);
        pS.setColor(color);
        c.drawLine(x0, y0 + cut, x0 + cut, y0, pS);
        c.drawLine(x1 - cut, y1, x1, y1 - cut, pS);
    }

    @Override
    void draw(Canvas c, Battery b, float w, float h, int insetTop, int insetBottom, float t) {
        this.b = b;
        u = w / 411f;
        pickColors();

        drawBackground(c, w, h);

        float x0 = 14 * u, x1 = w - 14 * u, gap = 8 * u;
        float y = insetTop + 8 * u;
        float bottom = h - insetBottom - 8 * u;

        drawHeader(c, x0, y, x1, t);
        y += 58 * u;

        float r = 96 * u;
        drawRing(c, w / 2, y + r + 26 * u, r, t, x0, x1);
        y += 222 * u;

        drawPower(c, x0, y, x1, y + 64 * u, t);
        y += 64 * u + gap;

        float th = 62 * u, cw = (x1 - x0 - gap) / 2;
        float xb = x0 + cw + gap;
        tile(c, x0, y, cw, th, "VOLTAGE", b.sVolt, b.sVoltSub, CYAN, TEXT);
        tile(c, xb, y, cw, th, "CURRENT", b.sCur, b.sCurSub, MAGENTA, TEXT);
        y += th + gap;
        tile(c, x0, y, cw, th, "TEMPERATURE", b.sTemp, b.sTempSub, YELLOW, tempColor);
        tile(c, xb, y, cw, th, "TIME REMAINING", b.sEta, b.sEtaSub, CYAN, TEXT);
        y += th + gap;
        tile(c, x0, y, cw, th, "CHARGE", b.sCharge, b.sChargeSub, MAGENTA, TEXT);
        tile(c, xb, y, cw, th, "CYCLES", b.sCycles, b.sCyclesSub, YELLOW, TEXT);
        y += th + gap;
        tile(c, x0, y, cw, th, "SOURCE", b.sSrc, b.sSrcSub, CYAN, TEXT);
        tile(c, xb, y, cw, th, "PEAK POWER", b.sPeak, b.sPeakSub, MAGENTA, TEXT);
        y += th + gap;

        if (bottom - y > 70 * u) drawChart(c, x0, y, x1, bottom);

        drawOverlay(c, w, h, t);
    }

    private void drawBackground(Canvas c, float w, float h) {
        pF.clearShadowLayer();
        pF.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xFF05060B, 0xFF080A16, 0xFF0D0616}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, pF);
        pF.setShader(null);
        resetStroke();
        pS.setStrokeWidth(1);
        pS.setColor(0x0F00F0FF);
        float step = 27.4f * u;
        for (float x = step / 2; x < w; x += step) c.drawLine(x, 0, x, h, pS);
        for (float yy = step / 2; yy < h; yy += step) c.drawLine(0, yy, w, yy, pS);
    }

    /** Draws the title in two runs so the monospace slashes sit tight together. */
    private void title(Canvas c, float x, float base, int color, int glow) {
        float size = 27 * u;
        txt(c, "PWR/", x, base, size, color, mono, Paint.Align.LEFT, glow);
        float overlap = pT.measureText("/") * 0.42f;
        txt(c, "/MON", x + pT.measureText("PWR/") - overlap, base, size, color, mono, Paint.Align.LEFT, glow);
    }

    private void drawHeader(Canvas c, float x0, float y, float x1, float t) {
        float base = y + 26 * u;
        // chromatic split, exaggerated for a moment every few seconds
        float phase = t % 5f;
        float split = phase < 0.12f ? 4 * u : (phase > 2.5f && phase < 2.56f ? -3 * u : 0.8f * u);
        title(c, x0 - split, base, alpha(MAGENTA, 0xB0), 0);
        title(c, x0 + split, base, alpha(CYAN, 0xB0), 0);
        title(c, x0, base, TEXT, CYAN);
        String dev = Build.MODEL.toUpperCase(Locale.US) + " // ANDROID " + Build.VERSION.RELEASE;
        txt(c, dev, x0 + 1 * u, base + 16 * u, 9.5f * u, DIM, label, Paint.Align.LEFT, 0);

        txt(c, b.sClock, x1, base - 4 * u, 17 * u, accent, mono, Paint.Align.RIGHT, accent);
        float clockW = pT.measureText(b.sClock.isEmpty() ? "00:00:00" : b.sClock);
        boolean blink = (t % 1f) < 0.6f;
        txt(c, "● LIVE", x1, base + 16 * u, 9.5f * u, blink ? GREEN : alpha(GREEN, 0x40), label,
                Paint.Align.RIGHT, 0);

        // theme chip, left of the clock
        float bw = 58 * u, bh = 22 * u;
        float bx1 = x1 - clockW - 12 * u, bx0 = bx1 - bw;
        float by0 = base - 4 * u - 6 * u - bh / 2, by1 = by0 + bh;
        menuButton.set(bx0 - 8 * u, by0 - 10 * u, bx1 + 6 * u, by1 + 12 * u);
        float cut = 6 * u;
        path.rewind();
        path.moveTo(bx0 + cut, by0);
        path.lineTo(bx1, by0);
        path.lineTo(bx1, by1 - cut);
        path.lineTo(bx1 - cut, by1);
        path.lineTo(bx0, by1);
        path.lineTo(bx0, by0 + cut);
        path.close();
        pF.setShader(null);
        pF.clearShadowLayer();
        pF.setColor(alpha(accent2, 0x22));
        c.drawPath(path, pF);
        resetStroke();
        pS.setStrokeWidth(1.2f * u);
        pS.setColor(alpha(accent2, 0xC0));
        c.drawPath(path, pS);
        txt(c, "THEME", (bx0 + bx1) / 2, by1 - 7.5f * u, 9.5f * u, TEXT, label, Paint.Align.CENTER, 0);

        float ly = y + 50 * u;
        resetStroke();
        pS.setStrokeWidth(1 * u);
        pS.setColor(alpha(accent, 0x50));
        c.drawLine(x0, ly, x1, ly, pS);
        pS.setStrokeWidth(3 * u);
        pS.setColor(accent);
        c.drawLine(x0, ly, x0 + 46 * u, ly, pS);
        pS.setColor(accent2);
        c.drawLine(x0 + 50 * u, ly, x0 + 58 * u, ly, pS);
    }

    private void drawRing(Canvas c, float cx, float cy, float r, float t, float x0, float x1) {
        float frac = Math.max(0f, Math.min(1f, b.level / 100f));
        boolean charging = b.charging();
        boolean discharging = b.discharging();

        // corner brackets + side captions
        float top = cy - r - 24 * u, bot = cy + r * 0.72f + 18 * u, bk = 12 * u;
        resetStroke();
        pS.setStrokeWidth(1.5f * u);
        pS.setColor(alpha(accent, 0x90));
        c.drawLine(x0, top, x0 + bk, top, pS);
        c.drawLine(x0, top, x0, top + bk, pS);
        c.drawLine(x1, top, x1 - bk, top, pS);
        c.drawLine(x1, top, x1, top + bk, pS);
        c.drawLine(x0, bot, x0 + bk, bot, pS);
        c.drawLine(x0, bot, x0, bot - bk, pS);
        c.drawLine(x1, bot, x1 - bk, bot, pS);
        c.drawLine(x1, bot, x1, bot - bk, pS);
        txt(c, "SOC.GAUGE", x0 + 6 * u, top + 16 * u, 8.5f * u, DIM, label, Paint.Align.LEFT, 0);
        txt(c, "ID " + Build.DEVICE.toUpperCase(Locale.US), x1 - 6 * u, top + 16 * u, 8.5f * u, DIM,
                label, Paint.Align.RIGHT, 0);
        txt(c, b.sSource, x0 + 6 * u, bot - 8 * u, 8.5f * u, DIM, label, Paint.Align.LEFT, 0);
        txt(c, b.sVolt, x1 - 6 * u, bot - 8 * u, 8.5f * u, DIM, label, Paint.Align.RIGHT, 0);

        // tick ring
        int n = 61;
        pS.setStrokeWidth(2 * u);
        for (int k = 0; k < n; k++) {
            float f = k / (float) (n - 1);
            double a = Math.toRadians(135 + 270 * f);
            float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
            float r1 = r + 13 * u, r2 = r + (k % 6 == 0 ? 22 : 18) * u;
            pS.setColor(f <= frac + 0.001f ? accent : 0x30FFFFFF);
            c.drawLine(cx + ca * r1, cy + sa * r1, cx + ca * r2, cy + sa * r2, pS);
        }

        // track + progress arc
        oval.set(cx - r, cy - r, cx + r, cy + r);
        pS.setStrokeWidth(11 * u);
        pS.setColor(0x1CFFFFFF);
        c.drawArc(oval, 135, 270, false, pS);
        if (frac > 0) {
            SweepGradient sg = new SweepGradient(cx, cy, new int[]{accent2, accent, accent},
                    new float[]{0f, 0.6f, 1f});
            matrix.setRotate(133, cx, cy);
            sg.setLocalMatrix(matrix);
            pS.setShader(sg);
            pS.setShadowLayer(14 * u, 0, 0, accent);
            c.drawArc(oval, 135, 270 * frac, false, pS);
            pS.clearShadowLayer();
            pS.setShader(null);

            double a = Math.toRadians(135 + 270 * frac);
            float px = cx + (float) Math.cos(a) * r, py = cy + (float) Math.sin(a) * r;
            float pulse = 0.5f + 0.5f * (float) Math.sin(t * 5);
            pF.setShader(null);
            pF.setColor(0xFFFFFFFF);
            pF.setShadowLayer((8 + 8 * pulse) * u, 0, 0, accent);
            c.drawCircle(px, py, 4.5f * u, pF);
            pF.clearShadowLayer();
        }

        // rotating inner rings: speed and direction follow the current
        float spin = charging ? 50 : discharging ? -18 : 6;
        c.save();
        c.rotate(t * spin, cx, cy);
        float ri = r - 17 * u;
        oval.set(cx - ri, cy - ri, cx + ri, cy + ri);
        pS.setStrokeWidth(1.5f * u);
        pS.setColor(alpha(accent, 0x80));
        pS.setPathEffect(new DashPathEffect(new float[]{9 * u, 7 * u}, 0));
        c.drawOval(oval, pS);
        pS.setPathEffect(null);
        c.restore();
        c.save();
        c.rotate(-t * spin * 0.6f, cx, cy);
        float ro = r - 25 * u;
        oval.set(cx - ro, cy - ro, cx + ro, cy + ro);
        pS.setStrokeWidth(3 * u);
        pS.setColor(alpha(accent2, 0xA0));
        for (int k = 0; k < 3; k++) c.drawArc(oval, k * 120, 46, false, pS);
        c.restore();

        // centre readout
        txt(c, "CELL LEVEL", cx, cy - 38 * u, 9.5f * u, DIM, label, Paint.Align.CENTER, 0);
        pT.setTypeface(mono);
        pT.setTextSize(68 * u);
        pT.setLetterSpacing(0f);
        float nw = pT.measureText(b.sLevel);
        float pw = 16 * u;
        float nx = cx - (nw + pw) / 2;
        txt(c, b.sLevel, nx, cy + 22 * u, 68 * u, TEXT, mono, Paint.Align.LEFT, accent);
        txt(c, "%", nx + nw + 3 * u, cy + 22 * u, 22 * u, accent, mono, Paint.Align.LEFT, 0);
        boolean blink = !charging || (t % 1.2f) < 0.8f;
        String st = charging ? "▲ " + b.sStatus : discharging ? "▼ " + b.sStatus : b.sStatus;
        txt(c, st, cx, cy + 46 * u, 12 * u, blink ? accent : alpha(accent, 0x55), label,
                Paint.Align.CENTER, 0);
    }

    private void drawPower(Canvas c, float x0, float y0, float x1, float y1, float t) {
        panel(c, x0, y0, x1, y1, accent);
        txt(c, "POWER FLOW", x0 + 14 * u, y0 + 19 * u, 9.5f * u, DIM, label, Paint.Align.LEFT, 0);
        txt(c, b.sWatts, x0 + 14 * u, y0 + 48 * u, 30 * u, TEXT, mono, Paint.Align.LEFT, accent);

        // chevrons: run right when energy flows into the cell, left when it drains
        int dir = b.watts > 0.05f ? 1 : b.watts < -0.05f ? -1 : 0;
        int n = 7;
        float cw = 13 * u, chH = 9 * u;
        float cxr = x1 - 18 * u - n * cw, cy = y0 + 24 * u;
        float head = (t * 9f) % (n + 3);
        resetStroke();
        pS.setStrokeWidth(2.5f * u);
        pS.setStrokeCap(Paint.Cap.SQUARE);
        for (int k = 0; k < n; k++) {
            int idx = dir >= 0 ? k : n - 1 - k;
            float d = head - idx;
            int a = dir == 0 ? 0x30 : (d >= 0 && d < 3 ? (int) (255 - d * 60) : 0x30);
            pS.setColor(alpha(accent, a));
            float x = cxr + k * cw;
            float tip = dir >= 0 ? x + cw * 0.5f : x, tail = dir >= 0 ? x : x + cw * 0.5f;
            c.drawLine(tail, cy - chH, tip, cy, pS);
            c.drawLine(tip, cy, tail, cy + chH, pS);
        }
        pS.setStrokeCap(Paint.Cap.BUTT);

        // load bar against the negotiated input budget
        float ref = b.budgetW > 0 ? b.budgetW : 15f;
        float ratio = Math.min(1f, Math.abs(b.watts) / ref);
        int segs = 16;
        float bw = n * cw, sx = x1 - 18 * u - bw, sw = bw / segs, sy = y0 + 45 * u;
        pF.setShader(null);
        pF.clearShadowLayer();
        for (int k = 0; k < segs; k++) {
            pF.setColor(k < Math.round(ratio * segs) ? accent : 0x26FFFFFF);
            c.drawRect(sx + k * sw, sy, sx + (k + 1) * sw - 1.5f * u, sy + 6 * u, pF);
        }
        txt(c, Battery.fmt("%d%% OF %.0fW", Math.round(ratio * 100), ref), x1 - 18 * u, y1 - 5 * u,
                7.5f * u, DIM, label, Paint.Align.RIGHT, 0);
    }

    private void tile(Canvas c, float x, float y, float w, float h, String name, String value,
                      String sub, int color, int valueColor) {
        panel(c, x, y, x + w, y + h, color);
        pF.setShader(null);
        pF.clearShadowLayer();
        pF.setColor(color);
        c.drawRect(x + 12 * u, y + 11 * u, x + 15 * u, y + 19 * u, pF);
        txt(c, name, x + 20 * u, y + 19 * u, 9.5f * u, DIM, label, Paint.Align.LEFT, 0);
        txt(c, value, x + 12 * u, y + 42 * u, 21 * u, valueColor, mono, Paint.Align.LEFT,
                alpha(color, 0xA0));
        txt(c, sub, x + 12 * u, y + 55 * u, 8.5f * u, alpha(color, 0xC0), label, Paint.Align.LEFT, 0);
    }

    private void drawChart(Canvas c, float x0, float y0, float x1, float y1) {
        panel(c, x0, y0, x1, y1, accent);
        txt(c, "POWER TRACE // " + Battery.HIST + "S", x0 + 14 * u, y0 + 19 * u, 9.5f * u, DIM, label,
                Paint.Align.LEFT, 0);

        float[] hist = b.hist;
        int histN = b.histN;
        float lo = 0, hi = 1;
        for (int k = 0; k < histN; k++) {
            lo = Math.min(lo, hist[k]);
            hi = Math.max(hi, hist[k]);
        }
        float span = (hi - lo) * 1.15f;
        hi = lo + span;
        txt(c, Battery.fmt("MAX %.1fW", hi / 1.15f + lo * (1 - 1 / 1.15f)), x1 - 14 * u, y0 + 19 * u,
                9.5f * u, accent, label, Paint.Align.RIGHT, 0);

        float gx0 = x0 + 12 * u, gx1 = x1 - 12 * u, gy0 = y0 + 28 * u, gy1 = y1 - 12 * u;
        resetStroke();
        pS.setStrokeWidth(1);
        pS.setColor(0x20FFFFFF);
        for (int k = 0; k <= 4; k++) {
            float gy = gy0 + (gy1 - gy0) * k / 4f;
            c.drawLine(gx0, gy, gx1, gy, pS);
        }
        for (int k = 0; k <= 6; k++) {
            float gx = gx0 + (gx1 - gx0) * k / 6f;
            c.drawLine(gx, gy0, gx, gy1, pS);
        }
        float zeroY = gy1 - (0 - lo) / span * (gy1 - gy0);
        if (lo < 0) {
            pS.setColor(0x60FFFFFF);
            c.drawLine(gx0, zeroY, gx1, zeroY, pS);
        }
        if (histN < 2) return;

        // grow from the left and compress until the window is full
        float dx = (gx1 - gx0) / (Math.max(histN, 30) - 1);
        float sx = gx0;
        float ex = sx + (histN - 1) * dx;
        path.rewind();
        for (int k = 0; k < histN; k++) {
            float px = sx + k * dx;
            float py = gy1 - (hist[k] - lo) / span * (gy1 - gy0);
            if (k == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        float lastY = gy1 - (hist[histN - 1] - lo) / span * (gy1 - gy0);

        Path fill = new Path(path);
        fill.lineTo(ex, zeroY);
        fill.lineTo(sx, zeroY);
        fill.close();
        pF.clearShadowLayer();
        pF.setShader(new LinearGradient(0, gy0, 0, gy1, alpha(accent, 0x60), alpha(accent, 0x05),
                Shader.TileMode.CLAMP));
        c.drawPath(fill, pF);
        pF.setShader(null);

        pS.setStrokeWidth(2 * u);
        pS.setStrokeJoin(Paint.Join.ROUND);
        pS.setColor(accent);
        pS.setShadowLayer(8 * u, 0, 0, accent);
        c.drawPath(path, pS);
        pS.clearShadowLayer();

        pF.setColor(0xFFFFFFFF);
        pF.setShadowLayer(8 * u, 0, 0, accent);
        c.drawCircle(ex, lastY, 3.5f * u, pF);
        pF.clearShadowLayer();
    }

    private void drawOverlay(Canvas c, float w, float h, float t) {
        // CRT scanlines
        resetStroke();
        pS.setStrokeWidth(1);
        pS.setColor(0x14000000);
        for (float y = 0; y < h; y += 4) c.drawLine(0, y, w, y, pS);
        // slow sweep beam
        float band = 90 * u;
        float by = (t * 140 * u) % (h + band * 4) - band;
        pF.clearShadowLayer();
        pF.setShader(new LinearGradient(0, by, 0, by + band,
                new int[]{0x00000000, alpha(accent, 0x12), 0x00000000}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, by, w, by + band, pF);
        pF.setShader(null);
    }

    @Override
    void drawPicker(Canvas c, Skin[] all, int current, RectF[] hits, float w, float h,
                    int insetTop, int insetBottom, float t) {
        u = w / 411f;
        pF.setShader(null);
        pF.clearShadowLayer();
        pF.setColor(0xD805060B);
        c.drawRect(0, 0, w, h, pF);

        float x0 = 22 * u, x1 = w - 22 * u;
        float avail = h - insetTop - insetBottom - 120 * u;
        float rowH = Math.min(46 * u, avail / all.length);
        float panelH = 64 * u + rowH * all.length;
        float y0 = Math.max(insetTop + 20 * u, (h - panelH) / 2), y1 = y0 + panelH;
        panel(c, x0, y0, x1, y1, CYAN);

        float phase = t % 3f;
        float split = phase < 0.1f ? 3 * u : 0.7f * u;
        float base = y0 + 32 * u;
        txt(c, "SELECT // THEME", x0 + 18 * u - split, base, 19 * u, alpha(MAGENTA, 0xB0), mono,
                Paint.Align.LEFT, 0);
        txt(c, "SELECT // THEME", x0 + 18 * u + split, base, 19 * u, alpha(CYAN, 0xB0), mono,
                Paint.Align.LEFT, 0);
        txt(c, "SELECT // THEME", x0 + 18 * u, base, 19 * u, TEXT, mono, Paint.Align.LEFT, CYAN);
        txt(c, "TAP OUTSIDE TO CLOSE", x1 - 16 * u, base, 8 * u, DIM, label, Paint.Align.RIGHT, 0);

        float y = y0 + 50 * u;
        String lastFamily = null;
        for (int i = 0; i < all.length; i++) {
            Skin s = all[i];
            float ry0 = y + i * rowH, ry1 = ry0 + rowH - 4 * u;
            float rx0 = x0 + 12 * u, rx1 = x1 - 12 * u;
            hits[i].set(rx0, ry0, rx1, ry1);
            boolean on = i == current;
            if (on) {
                pF.setColor(alpha(CYAN, 0x26));
                c.drawRect(rx0, ry0, rx1, ry1, pF);
                pF.setColor(CYAN);
                c.drawRect(rx0, ry0, rx0 + 3 * u, ry1, pF);
            } else {
                resetStroke();
                pS.setStrokeWidth(1);
                pS.setColor(0x22FFFFFF);
                c.drawLine(rx0, ry1, rx1, ry1, pS);
            }
            float mid = (ry0 + ry1) / 2;
            int[] sw = s.swatch();
            float sq = Math.min(10 * u, rowH * 0.3f);
            for (int k = 0; k < sw.length; k++) {
                pF.setColor(sw[k]);
                float sx = rx0 + 12 * u + k * (sq + 3 * u);
                c.drawRect(sx, mid - sq / 2, sx + sq, mid + sq / 2, pF);
            }
            float tx = rx0 + 12 * u + 4 * (sq + 3 * u) + 8 * u;
            float nameSize = Math.min(15 * u, rowH * 0.42f);
            txt(c, s.name.toUpperCase(Locale.US), tx, mid + nameSize * 0.35f, nameSize, on ? TEXT : 0xFFB8C6D9,
                    mono, Paint.Align.LEFT, on ? CYAN : 0);
            boolean newFamily = !s.family.equals(lastFamily);
            lastFamily = s.family;
            if (on) {
                txt(c, "◆ ACTIVE", rx1 - 10 * u, mid + 3 * u, 8.5f * u, CYAN, label, Paint.Align.RIGHT, 0);
            } else if (newFamily) {
                txt(c, s.family, rx1 - 10 * u, mid + 3 * u, 8.5f * u, DIM, label, Paint.Align.RIGHT, 0);
            }
        }
        drawOverlay(c, w, h, t);
    }
}
