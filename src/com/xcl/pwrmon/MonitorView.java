package com.xcl.pwrmon;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Insets;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowInsets;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Full-screen, self-drawn battery / charging dashboard. */
public class MonitorView extends View {
    private static final int CYAN = 0xFF00F0FF;
    private static final int MAGENTA = 0xFFFF2A6D;
    private static final int YELLOW = 0xFFFCEE0A;
    private static final int GREEN = 0xFF39FF14;
    private static final int RED = 0xFFFF3B3B;
    private static final int TEXT = 0xFFE2F9FF;
    private static final int DIM = 0xFF64748F;
    private static final int HIST = 120;
    private static final long SAMPLE_MS = 1000;
    private static final long FRAME_MS = 33;

    private final Typeface mono = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
    private final Typeface label = Typeface.create("sans-serif-condensed", Typeface.BOLD);
    private final Paint pT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pF = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pS = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF oval = new RectF();
    private final Matrix matrix = new Matrix();
    private final SimpleDateFormat clockFmt = new SimpleDateFormat("HH:mm:ss", Locale.US);
    private final BatteryManager bm;
    private final long t0 = SystemClock.uptimeMillis();

    private float u = 1f;
    private int insetTop, insetBottom;
    private boolean running;
    private boolean sysDenied;

    // latest sample
    private float level, volts, amps, ampsAvg, watts, temp, budgetW, peakW;
    private int status, plugged, health, cycles = -1;
    private final float[] hist = new float[HIST];
    private int histN;
    private int accent = CYAN, accent2 = MAGENTA;
    private String sLevel = "--", sStatus = "NO DATA", sSource = "", sClock = "";
    private String sWatts = "--", sVolt = "--", sVoltSub = "", sCur = "--", sCurSub = "";
    private String sTemp = "--", sTempSub = "", sEta = "--", sEtaSub = "";
    private String sCharge = "--", sChargeSub = "", sCycles = "--", sCyclesSub = "";
    private String sSrc = "--", sSrcSub = "", sPeak = "--", sPeakSub = "";
    private int tempColor = TEXT;

    private final Runnable sampler = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            sample();
            postDelayed(this, SAMPLE_MS);
        }
    };

    public MonitorView(Context ctx) {
        super(ctx);
        bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
        pS.setStyle(Paint.Style.STROKE);
        setOnApplyWindowInsetsListener((v, in) -> {
            Insets i = in.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            insetTop = i.top;
            insetBottom = i.bottom;
            return in;
        });
    }

    public void start() {
        if (running) return;
        running = true;
        removeCallbacks(sampler);
        post(sampler);
        invalidate();
    }

    public void stop() {
        running = false;
        removeCallbacks(sampler);
    }

    // ---------------------------------------------------------------- data

    private long readSys(String file) {
        if (sysDenied) return -1;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            return Long.parseLong(r.readLine().trim());
        } catch (Exception e) {
            sysDenied = true;
            return -1;
        }
    }

    private static long microAmps(long raw) {
        if (raw == Integer.MIN_VALUE || raw == Long.MIN_VALUE) return 0;
        // a few vendors report mA instead of the documented µA
        return Math.abs(raw) < 30000 ? raw * 1000 : raw;
    }

    private static String fmt(String f, Object... a) {
        return String.format(Locale.US, f, a);
    }

    private static String hm(double hours) {
        if (hours <= 0 || hours > 99) return "--";
        int m = (int) Math.round(hours * 60);
        return m >= 60 ? fmt("%dh %02dm", m / 60, m % 60) : fmt("%d min", m);
    }

    private void sample() {
        Intent i = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i == null) return;
        int lv = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int sc = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        level = sc > 0 ? 100f * lv / sc : lv;
        status = i.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        health = i.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);
        temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f;
        cycles = i.getIntExtra("android.os.extra.CYCLE_COUNT", -1);
        String tech = i.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
        int maxI = i.getIntExtra("max_charging_current", -1);
        int maxV = i.getIntExtra("max_charging_voltage", -1);

        volts = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000f;
        long sv = readSys("/sys/class/power_supply/battery/voltage_now");
        boolean liveVolt = sv > 0;
        if (liveVolt) volts = sv / 1e6f;

        long cur = microAmps(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW));
        long avg = microAmps(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE));
        long cc = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING;
        boolean discharging = status == BatteryManager.BATTERY_STATUS_DISCHARGING;
        // normalise to "positive = into the battery"
        if ((charging && cur < 0) || (discharging && cur > 0)) cur = -cur;
        avg = cur < 0 ? -Math.abs(avg) : Math.abs(avg);
        amps = cur / 1e6f;
        ampsAvg = avg / 1e6f;
        watts = volts * amps;
        peakW = Math.max(peakW, Math.abs(watts));
        budgetW = (maxI > 0 && maxV > 0) ? (maxI / 1e6f) * (maxV / 1e6f) : 0;

        if (histN < HIST) {
            hist[histN++] = watts;
        } else {
            System.arraycopy(hist, 1, hist, 0, HIST - 1);
            hist[HIST - 1] = watts;
        }

        boolean full = status == BatteryManager.BATTERY_STATUS_FULL;
        if (full) {
            accent = GREEN;
            accent2 = CYAN;
            sStatus = "FULL";
        } else if (charging) {
            accent = CYAN;
            accent2 = MAGENTA;
            sStatus = "CHARGING";
        } else if (discharging) {
            accent = level <= 15 ? RED : MAGENTA;
            accent2 = level <= 15 ? YELLOW : CYAN;
            sStatus = level <= 15 ? "LOW POWER" : "DISCHARGING";
        } else {
            accent = YELLOW;
            accent2 = MAGENTA;
            sStatus = status == BatteryManager.BATTERY_STATUS_NOT_CHARGING ? "IDLE" : "UNKNOWN";
        }

        String src;
        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_AC: src = "AC"; break;
            case BatteryManager.BATTERY_PLUGGED_USB: src = "USB"; break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS: src = "WIRELESS"; break;
            case BatteryManager.BATTERY_PLUGGED_DOCK: src = "DOCK"; break;
            default: src = "BATTERY";
        }
        sSource = plugged != 0 ? "SRC " + src : "ON BATTERY";

        sLevel = fmt("%d", Math.round(level));
        sClock = clockFmt.format(new Date());
        sWatts = fmt("%+.2f W", watts);
        sVolt = fmt("%.3f V", volts);
        sVoltSub = (tech != null ? tech.toUpperCase(Locale.US) : "CELL") + (liveVolt ? " · LIVE" : " · SYSTEM");
        sCur = fmt("%+d mA", Math.round(amps * 1000));
        sCurSub = fmt("AVG %+d mA", Math.round(ampsAvg * 1000));
        sTemp = fmt("%.1f °C", temp);
        if (temp >= 43) {
            sTempSub = "HOT";
            tempColor = RED;
        } else if (temp >= 38) {
            sTempSub = "WARM";
            tempColor = YELLOW;
        } else {
            sTempSub = "NOMINAL";
            tempColor = TEXT;
        }

        double mAh = cc > 0 ? cc / 1000.0 : -1;
        double estFull = (mAh > 0 && level >= 5) ? mAh / (level / 100.0) : -1;
        if (full) {
            sEta = "DONE";
            sEtaSub = "FULLY CHARGED";
        } else if (charging) {
            long ms = bm.computeChargeTimeRemaining();
            double h = ms > 0 ? ms / 3.6e6 : (estFull > 0 && avg > 0 ? (estFull - mAh) / (avg / 1000.0) : -1);
            sEta = hm(h);
            sEtaSub = "TO FULL";
        } else {
            double h = (mAh > 0 && avg < 0) ? mAh / (-avg / 1000.0) : -1;
            sEta = hm(h);
            sEtaSub = "TO EMPTY";
        }
        sCharge = mAh > 0 ? fmt("%d mAh", Math.round(mAh)) : "--";
        sChargeSub = estFull > 0 ? fmt("EST FULL %d mAh", Math.round(estFull)) : "";
        sCycles = cycles >= 0 ? fmt("%d", cycles) : "--";
        sCyclesSub = "HEALTH " + healthName(health);
        sSrc = src;
        sSrcSub = budgetW > 0 ? fmt("%.1fV · %.1fA · %.0fW MAX", maxV / 1e6f, maxI / 1e6f, budgetW) : "NO EXTERNAL POWER";
        sPeak = fmt("%.2f W", peakW);
        sPeakSub = "SESSION PEAK";
    }

    private static String healthName(int h) {
        switch (h) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "GOOD";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "OVERHEAT";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "DEAD";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "OVERVOLT";
            case BatteryManager.BATTERY_HEALTH_COLD: return "COLD";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE: return "FAILURE";
            default: return "UNKNOWN";
        }
    }

    // ------------------------------------------------------------- drawing

    private static int alpha(int color, int a) {
        return (color & 0x00FFFFFF) | (a << 24);
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
        pS.setShader(null);
        pS.setPathEffect(null);
        pS.clearShadowLayer();
        pS.setStrokeCap(Paint.Cap.BUTT);
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
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        u = w / 411f;
        float t = (SystemClock.uptimeMillis() - t0) / 1000f;

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
        tile(c, x0, y, cw, th, "VOLTAGE", sVolt, sVoltSub, CYAN, TEXT);
        tile(c, xb, y, cw, th, "CURRENT", sCur, sCurSub, MAGENTA, TEXT);
        y += th + gap;
        tile(c, x0, y, cw, th, "TEMPERATURE", sTemp, sTempSub, YELLOW, tempColor);
        tile(c, xb, y, cw, th, "TIME REMAINING", sEta, sEtaSub, CYAN, TEXT);
        y += th + gap;
        tile(c, x0, y, cw, th, "CHARGE", sCharge, sChargeSub, MAGENTA, TEXT);
        tile(c, xb, y, cw, th, "CYCLES", sCycles, sCyclesSub, YELLOW, TEXT);
        y += th + gap;
        tile(c, x0, y, cw, th, "SOURCE", sSrc, sSrcSub, CYAN, TEXT);
        tile(c, xb, y, cw, th, "PEAK POWER", sPeak, sPeakSub, MAGENTA, TEXT);
        y += th + gap;

        if (bottom - y > 70 * u) drawChart(c, x0, y, x1, bottom);

        drawOverlay(c, w, h, t);

        if (running) postInvalidateDelayed(FRAME_MS);
    }

    private void drawBackground(Canvas c, float w, float h) {
        pF.clearShadowLayer();
        pF.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xFF05060B, 0xFF080A16, 0xFF0D0616}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, pF);
        pF.setShader(null);
        pS.setShader(null);
        pS.setPathEffect(null);
        pS.clearShadowLayer();
        pS.setStrokeWidth(1);
        pS.setColor(0x0F00F0FF);
        float step = 27.4f * u;
        for (float x = step / 2; x < w; x += step) c.drawLine(x, 0, x, h, pS);
        for (float yy = step / 2; yy < h; yy += step) c.drawLine(0, yy, w, yy, pS);
    }

    private void drawHeader(Canvas c, float x0, float y, float x1, float t) {
        float base = y + 26 * u;
        // chromatic split, exaggerated for a moment every few seconds
        float phase = t % 5f;
        float split = phase < 0.12f ? 4 * u : (phase > 2.5f && phase < 2.56f ? -3 * u : 0.8f * u);
        txt(c, "PWR//MON", x0 - split, base, 27 * u, alpha(MAGENTA, 0xB0), mono, Paint.Align.LEFT, 0);
        txt(c, "PWR//MON", x0 + split, base, 27 * u, alpha(CYAN, 0xB0), mono, Paint.Align.LEFT, 0);
        txt(c, "PWR//MON", x0, base, 27 * u, TEXT, mono, Paint.Align.LEFT, CYAN);
        String dev = Build.MODEL.toUpperCase(Locale.US) + " // ANDROID " + Build.VERSION.RELEASE;
        txt(c, dev, x0 + 1 * u, base + 16 * u, 9.5f * u, DIM, label, Paint.Align.LEFT, 0);

        txt(c, sClock, x1, base - 4 * u, 17 * u, accent, mono, Paint.Align.RIGHT, accent);
        boolean blink = (t % 1f) < 0.6f;
        txt(c, "● LIVE", x1, base + 16 * u, 9.5f * u, blink ? GREEN : alpha(GREEN, 0x40), label,
                Paint.Align.RIGHT, 0);

        float ly = y + 50 * u;
        pS.setShader(null);
        pS.setPathEffect(null);
        pS.clearShadowLayer();
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
        float frac = Math.max(0f, Math.min(1f, level / 100f));
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING;
        boolean discharging = status == BatteryManager.BATTERY_STATUS_DISCHARGING;

        // corner brackets + side captions
        float top = cy - r - 24 * u, bot = cy + r * 0.72f + 18 * u, b = 12 * u;
        pS.setShader(null);
        pS.setPathEffect(null);
        pS.clearShadowLayer();
        pS.setStrokeCap(Paint.Cap.BUTT);
        pS.setStrokeWidth(1.5f * u);
        pS.setColor(alpha(accent, 0x90));
        c.drawLine(x0, top, x0 + b, top, pS);
        c.drawLine(x0, top, x0, top + b, pS);
        c.drawLine(x1, top, x1 - b, top, pS);
        c.drawLine(x1, top, x1, top + b, pS);
        c.drawLine(x0, bot, x0 + b, bot, pS);
        c.drawLine(x0, bot, x0, bot - b, pS);
        c.drawLine(x1, bot, x1 - b, bot, pS);
        c.drawLine(x1, bot, x1, bot - b, pS);
        txt(c, "SOC.GAUGE", x0 + 6 * u, top + 16 * u, 8.5f * u, DIM, label, Paint.Align.LEFT, 0);
        txt(c, "ID " + Build.DEVICE.toUpperCase(Locale.US), x1 - 6 * u, top + 16 * u, 8.5f * u, DIM,
                label, Paint.Align.RIGHT, 0);
        txt(c, sSource, x0 + 6 * u, bot - 8 * u, 8.5f * u, DIM, label, Paint.Align.LEFT, 0);
        txt(c, sVolt, x1 - 6 * u, bot - 8 * u, 8.5f * u, DIM, label, Paint.Align.RIGHT, 0);

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
        float nw = pT.measureText(sLevel);
        float pw = 16 * u;
        float nx = cx - (nw + pw) / 2;
        txt(c, sLevel, nx, cy + 22 * u, 68 * u, TEXT, mono, Paint.Align.LEFT, accent);
        txt(c, "%", nx + nw + 3 * u, cy + 22 * u, 22 * u, accent, mono, Paint.Align.LEFT, 0);
        boolean blink = !charging || (t % 1.2f) < 0.8f;
        String st = charging ? "▲ " + sStatus : discharging ? "▼ " + sStatus : sStatus;
        txt(c, st, cx, cy + 46 * u, 12 * u, blink ? accent : alpha(accent, 0x55), label,
                Paint.Align.CENTER, 0);
    }

    private void drawPower(Canvas c, float x0, float y0, float x1, float y1, float t) {
        panel(c, x0, y0, x1, y1, accent);
        txt(c, "POWER FLOW", x0 + 14 * u, y0 + 19 * u, 9.5f * u, DIM, label, Paint.Align.LEFT, 0);
        txt(c, sWatts, x0 + 14 * u, y0 + 48 * u, 30 * u, TEXT, mono, Paint.Align.LEFT, accent);

        // chevrons: run right when energy flows into the cell, left when it drains
        int dir = watts > 0.05f ? 1 : watts < -0.05f ? -1 : 0;
        int n = 7;
        float cw = 13 * u, chH = 9 * u;
        float cxr = x1 - 18 * u - n * cw, cy = y0 + 24 * u;
        float head = (t * 9f) % (n + 3);
        pS.setShader(null);
        pS.setPathEffect(null);
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
        float ref = budgetW > 0 ? budgetW : 15f;
        float ratio = Math.min(1f, Math.abs(watts) / ref);
        int segs = 16;
        float bw = n * cw, sx = x1 - 18 * u - bw, sw = bw / segs, sy = y0 + 45 * u;
        pF.setShader(null);
        pF.clearShadowLayer();
        for (int k = 0; k < segs; k++) {
            pF.setColor(k < Math.round(ratio * segs) ? accent : 0x26FFFFFF);
            c.drawRect(sx + k * sw, sy, sx + (k + 1) * sw - 1.5f * u, sy + 6 * u, pF);
        }
        txt(c, fmt("%d%% OF %.0fW", Math.round(ratio * 100), ref), x1 - 18 * u, y1 - 5 * u,
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
        txt(c, "POWER TRACE // " + HIST + "S", x0 + 14 * u, y0 + 19 * u, 9.5f * u, DIM, label,
                Paint.Align.LEFT, 0);

        float lo = 0, hi = 1;
        for (int k = 0; k < histN; k++) {
            lo = Math.min(lo, hist[k]);
            hi = Math.max(hi, hist[k]);
        }
        float span = (hi - lo) * 1.15f;
        hi = lo + span;
        txt(c, fmt("MAX %.1fW", hi / 1.15f + lo * (1 - 1 / 1.15f)), x1 - 14 * u, y0 + 19 * u, 9.5f * u,
                accent, label, Paint.Align.RIGHT, 0);

        float gx0 = x0 + 12 * u, gx1 = x1 - 12 * u, gy0 = y0 + 28 * u, gy1 = y1 - 12 * u;
        pS.setShader(null);
        pS.setPathEffect(null);
        pS.clearShadowLayer();
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

        float dx = (gx1 - gx0) / (HIST - 1);
        float sx = gx1 - (histN - 1) * dx;
        path.rewind();
        for (int k = 0; k < histN; k++) {
            float px = sx + k * dx;
            float py = gy1 - (hist[k] - lo) / span * (gy1 - gy0);
            if (k == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        float lastY = gy1 - (hist[histN - 1] - lo) / span * (gy1 - gy0);

        Path fill = new Path(path);
        fill.lineTo(gx1, zeroY);
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
        c.drawCircle(gx1, lastY, 3.5f * u, pF);
        pF.clearShadowLayer();
    }

    private void drawOverlay(Canvas c, float w, float h, float t) {
        // CRT scanlines
        pS.setShader(null);
        pS.setPathEffect(null);
        pS.clearShadowLayer();
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
}
