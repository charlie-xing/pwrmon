package com.xcl.pwrmon;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.os.SystemClock;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Omarchy desktop look: a Waybar strip on top and Hyprland-tiled terminal windows running
 * btop-style boxes and fastfetch, in one of Omarchy's bundled colour themes.
 */
final class OmarchySkin extends Skin {
    // 3x5 block glyphs for the tty-clock style level readout: 0-9 then '%'
    private static final int[][] GLYPHS = {
            {7, 5, 5, 5, 7}, {6, 2, 2, 2, 7}, {7, 1, 7, 4, 7}, {7, 1, 7, 1, 7}, {5, 5, 7, 1, 1},
            {7, 4, 7, 1, 7}, {7, 4, 7, 5, 7}, {7, 1, 1, 1, 1}, {7, 5, 7, 5, 7}, {7, 5, 7, 1, 7},
            {5, 1, 2, 4, 5},
    };
    private static final int[] BATT = {0xF008E, 0xF007A, 0xF007B, 0xF007C, 0xF007D, 0xF007E,
            0xF007F, 0xF0080, 0xF0081, 0xF0082, 0xF0079};
    private static final int[] BATT_CHG = {0xF089C, 0xF089C, 0xF0086, 0xF0087, 0xF0088, 0xF089D,
            0xF0089, 0xF089E, 0xF008A, 0xF008B, 0xF0085};

    /** Omarchy's bundled themes, from /usr/share/omarchy/themes/{name}/colors.toml. */
    static Skin[] all(Typeface reg, Typeface bold) {
        return new Skin[]{
                new OmarchySkin("tokyo-night", "Tokyo Night", false, reg, bold, 0x1a1b26, 0x13141c, 0xa9b1d6,
                        0xc0caf5, 0x414868, 0x292e42, 0x7aa2f7, 0xf7768e, 0xe0af68, 0x9ece6a, 0x0db9d7, 0x7aa2f7, 0xbb9af7),
                new OmarchySkin("catppuccin", "Catppuccin", false, reg, bold, 0x1e1e2e, 0x161622, 0xcdd6f4,
                        0xcdd6f4, 0x585b70, 0x45475a, 0x89b4fa, 0xf38ba8, 0xf9e2af, 0xa6e3a1, 0x94e2d5, 0x89b4fa, 0xf5c2e7),
                new OmarchySkin("gruvbox", "Gruvbox", false, reg, bold, 0x282828, 0x1e1e1e, 0xd4be98,
                        0xd4be98, 0x665c54, 0x504945, 0x7daea3, 0xea6962, 0xd8a657, 0xa9b665, 0x89b482, 0x7daea3, 0xd3869b),
                new OmarchySkin("nord", "Nord", false, reg, bold, 0x2e3440, 0x222730, 0xd8dee9,
                        0xd8dee9, 0x4c566a, 0x434c5e, 0x81a1c1, 0xbf616a, 0xebcb8b, 0xa3be8c, 0x88c0d0, 0x81a1c1, 0xb48ead),
                new OmarchySkin("everforest", "Everforest", false, reg, bold, 0x2d353b, 0x21272c, 0xd3c6aa,
                        0xd3c6aa, 0x475258, 0x3d484d, 0x7fbbb3, 0xe67e80, 0xdbbc7f, 0xa7c080, 0x83c092, 0x7fbbb3, 0xd699b6),
                new OmarchySkin("kanagawa", "Kanagawa", false, reg, bold, 0x1f1f28, 0x17171e, 0xdcd7ba,
                        0xdcd7ba, 0x54546d, 0x363646, 0xdcd7ba, 0xc34043, 0xc0a36e, 0x76946a, 0x6a9589, 0x7e9cd8, 0x957fb8),
                new OmarchySkin("matte-black", "Matte Black", false, reg, bold, 0x121212, 0x0d0d0d, 0xbebebe,
                        0xbebebe, 0x333333, 0x2a2a2a, 0xe68e0d, 0xd35f5f, 0xffc107, 0x8a9a5b, 0xbebebe, 0xe68e0d, 0xd35f5f),
                new OmarchySkin("osaka-jade", "Osaka Jade", false, reg, bold, 0x111c18, 0x0c1512, 0xc1c497,
                        0xf7e8b2, 0x53685b, 0x32473b, 0x509475, 0xff5345, 0xe5c736, 0x549e6a, 0x2dd5b7, 0x509475, 0xd2689c),
                new OmarchySkin("ristretto", "Ristretto", false, reg, bold, 0x2c2525, 0x211b1b, 0xe6d9db,
                        0xe6d9db, 0x72696a, 0x403e41, 0xf38d70, 0xfd6883, 0xf9cc6c, 0xadda78, 0x85dacc, 0xf38d70, 0xa8a9eb),
                new OmarchySkin("rose-pine", "Rosé Pine", true, reg, bold, 0xfaf4ed, 0xede7e1, 0x575279,
                        0x575279, 0xcecacd, 0xdfdad9, 0x56949f, 0xb4637a, 0xea9d34, 0x286983, 0xd7827e, 0x56949f, 0x907aa9),
                new OmarchySkin("catppuccin-latte", "Catppuccin Latte", true, reg, bold, 0xeff1f5, 0xe3e4e8, 0x4c4f69,
                        0x4c4f69, 0xacb0be, 0xccd0da, 0x1e66f5, 0xd20f39, 0xdf8e1d, 0x40a02b, 0x179299, 0x1e66f5, 0xea76cb),
        };
    }

    private final boolean light;
    private final int bg, darkBg, fg, brightFg, muted, sel, accent, red, yellow, green, cyan, blue, magenta;
    private final int dim;
    private final Typeface reg, bold;
    private final RectF[] wins = {new RectF(), new RectF(), new RectF(), new RectF(), new RectF()};
    private final RectF box = new RectF();
    private final SimpleDateFormat barFmt = new SimpleDateFormat("EEEE HH:mm", Locale.US);
    private final String kernel = String.valueOf(System.getProperty("os.version"));
    private String barClock = "";
    private long barSec = -1;
    private int focus;
    private float[] ptsIn = new float[0], ptsOut = new float[0];

    private Battery b;
    private float fs, cw, lh; // terminal font size, cell width, line height

    private OmarchySkin(String id, String name, boolean light, Typeface reg, Typeface bold, int bg,
                        int darkBg, int fg, int brightFg, int muted, int sel, int accent, int red,
                        int yellow, int green, int cyan, int blue, int magenta) {
        super(id, name, "omarchy");
        this.light = light;
        this.reg = reg;
        this.bold = bold;
        this.bg = 0xFF000000 | bg;
        this.darkBg = 0xFF000000 | darkBg;
        this.fg = 0xFF000000 | fg;
        this.brightFg = 0xFF000000 | brightFg;
        this.muted = 0xFF000000 | muted;
        this.sel = 0xFF000000 | sel;
        this.accent = 0xFF000000 | accent;
        this.red = 0xFF000000 | red;
        this.yellow = 0xFF000000 | yellow;
        this.green = 0xFF000000 | green;
        this.cyan = 0xFF000000 | cyan;
        this.blue = 0xFF000000 | blue;
        this.magenta = 0xFF000000 | magenta;
        dim = mix(this.fg, this.bg, 0.45f);
    }

    @Override
    int[] swatch() {
        return new int[]{accent, magenta, green, yellow};
    }

    @Override
    boolean light() {
        return light;
    }

    @Override
    boolean onTap(float x, float y) {
        for (int i = 0; i < wins.length; i++) {
            if (wins[i].contains(x, y)) {
                focus = i;
                return true;
            }
        }
        return false;
    }

    private int stateColor() {
        switch (b.state) {
            case Battery.FULL: return cyan;
            case Battery.CHARGING: return green;
            case Battery.LOW: return red;
            case Battery.DISCHARGING: return accent;
            default: return yellow;
        }
    }

    private static String lc(String s) {
        return s.toLowerCase(Locale.US);
    }

    private static String cp(int codePoint) {
        return new String(Character.toChars(codePoint));
    }

    // ---------------------------------------------------------------- text

    private void tx(Canvas c, String s, float x, float y, float size, int color, boolean heavy,
                    Paint.Align align) {
        pT.setShader(null);
        pT.clearShadowLayer();
        pT.setTypeface(heavy ? bold : reg);
        pT.setTextSize(size);
        pT.setLetterSpacing(0f);
        pT.setColor(color);
        pT.setTextAlign(align);
        c.drawText(s, x, y, pT);
    }

    private float measure(String s, float size, boolean heavy) {
        pT.setTypeface(heavy ? bold : reg);
        pT.setTextSize(size);
        pT.setLetterSpacing(0f);
        return pT.measureText(s);
    }

    /** Label in the dim colour, value starting at {@code vx}. */
    private void kv(Canvas c, String k, String v, float x, float vx, float y, int vColor) {
        tx(c, k, x, y, fs, dim, false, Paint.Align.LEFT);
        tx(c, v, vx, y, fs, vColor, false, Paint.Align.LEFT);
    }

    // -------------------------------------------------------------- frames

    /** A Hyprland window: square corners, 2px border, accent when focused. */
    private void window(Canvas c, RectF r, boolean focused) {
        pF.setShader(null);
        pF.clearShadowLayer();
        pF.setColor(alpha(bg, 0xF4));
        c.drawRect(r, pF);
        resetStroke();
        pS.setStrokeWidth(1.6f * u);
        pS.setColor(focused ? accent : alpha(muted, 0xC0));
        c.drawRect(r, pS);
    }

    /** A btop box: rounded corners, "┐¹title┌" set into the top edge, optional right caption. */
    private void btopBox(Canvas c, RectF r, int color, String num, String title, String right,
                         int rightColor) {
        resetStroke();
        pS.setStrokeWidth(1.1f * u);
        pS.setColor(color);
        c.drawRoundRect(r, 5 * u, 5 * u, pS);
        float base = r.top + fs * 0.36f;
        float x = r.left + 2 * cw;
        float wn = measure(num, fs, false), wt = measure(title, fs, true);
        notch(c, x, r.top, wn + wt, color);
        tx(c, num, x, base, fs, red, false, Paint.Align.LEFT);
        tx(c, title, x + wn, base, fs, brightFg, true, Paint.Align.LEFT);
        if (right != null) {
            float wr = measure(right, fs, false);
            float rx = r.right - 2 * cw - wr;
            notch(c, rx, r.top, wr, color);
            tx(c, right, rx, base, fs, rightColor, false, Paint.Align.LEFT);
        }
    }

    private void notch(Canvas c, float x, float y, float w, int color) {
        float pad = cw * 0.45f;
        pF.setShader(null);
        pF.setColor(bg);
        c.drawRect(x - pad, y - 2 * u, x + w + pad, y + 2 * u, pF);
        pS.setColor(color);
        c.drawLine(x - pad, y, x - pad, y + lh * 0.3f, pS);
        c.drawLine(x + w + pad, y, x + w + pad, y + lh * 0.3f, pS);
    }

    /** btop meter: a row of small squares graded from {@code lo} to {@code hi} colour. */
    private void meter(Canvas c, float x0, float x1, float baseline, float frac, int lo, int hi) {
        float pitch = cw;
        int n = Math.max(1, (int) ((x1 - x0) / pitch));
        int on = Math.round(Math.max(0f, Math.min(1f, frac)) * n);
        float s = cw * 0.66f;
        float y = baseline - fs * 0.36f - s / 2;
        pF.setShader(null);
        pF.clearShadowLayer();
        for (int k = 0; k < n; k++) {
            pF.setColor(k < on ? mix(lo, hi, n > 1 ? k / (float) (n - 1) : 1f) : alpha(muted, 0x70));
            float x = x0 + k * pitch;
            c.drawRect(x, y, x + s, y + s, pF);
        }
    }

    @Override
    void draw(Canvas c, Battery b, float w, float h, int insetTop, int insetBottom, float t) {
        this.b = b;
        u = w / 411f;
        fs = 10.5f * u;
        cw = measure("0", fs, false);
        lh = fs * 1.4f;

        // wallpaper
        pF.clearShadowLayer();
        pF.setShader(new LinearGradient(0, 0, w * 0.5f, h, darkBg, mix(darkBg, accent, light ? 0.12f : 0.18f),
                Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, pF);
        pF.setShader(null);

        float barB = insetTop + 24 * u;
        drawBar(c, w, insetTop, barB);

        float go = 8 * u, gi = 7 * u;
        float x0 = go, x1 = w - go, y0 = barB + go, y1 = h - insetBottom - go;
        float hh = y1 - y0 - 3 * gi;
        float h1 = hh * 0.22f, h2 = hh * 0.18f, h5 = hh * 0.26f, h4 = hh - h1 - h2 - h5;
        float xm = (x0 + x1 - gi) / 2;
        wins[0].set(x0, y0, x1, y0 + h1);
        float ya = wins[0].bottom + gi;
        wins[1].set(x0, ya, xm, ya + h2);
        wins[2].set(xm + gi, ya, x1, ya + h2);
        float yb = ya + h2 + gi;
        wins[3].set(x0, yb, x1, yb + h4);
        wins[4].set(x0, yb + h4 + gi, x1, y1);

        for (int i = 0; i < wins.length; i++) {
            window(c, wins[i], i == focus);
            c.save();
            c.clipRect(wins[i]);
            box.set(wins[i]);
            box.inset(6 * u, 7 * u);
            switch (i) {
                case 0: drawLevel(c, box); break;
                case 1: drawSensors(c, box); break;
                case 2: drawCell(c, box); break;
                case 3: drawPower(c, box); break;
                default: drawFetch(c, box, t);
            }
            c.restore();
        }
    }

    private void drawBar(Canvas c, float w, float top, float barB) {
        pF.setShader(null);
        pF.clearShadowLayer();
        pF.setColor(bg);
        c.drawRect(0, 0, w, barB, pF);
        float size = 11 * u, base = top + 16.5f * u;

        tx(c, "", 12 * u, base, size * 1.15f, accent, false, Paint.Align.LEFT);
        menuButton.set(0, top, 34 * u, barB + 6 * u);
        float x = 38 * u;
        for (int k = 1; k <= 5; k++) {
            boolean on = k == 1;
            tx(c, on ? "■" : String.valueOf(k), x, base, size, on ? brightFg : dim, on, Paint.Align.CENTER);
            x += 17 * u;
        }

        long sec = System.currentTimeMillis() / 1000;
        if (sec != barSec) {
            barSec = sec;
            barClock = barFmt.format(new Date());
        }
        tx(c, barClock, w / 2, base, size, fg, true, Paint.Align.CENTER);

        int idx = Math.round(Math.max(0, Math.min(100, b.level)) / 10f);
        String icon = cp(b.charging() || b.state == Battery.FULL ? BATT_CHG[idx] : BATT[idx]);
        int bc = b.state == Battery.LOW ? red : b.charging() ? green : fg;
        float rx = w - 12 * u;
        String pct = b.sLevel + "%";
        tx(c, pct, rx, base, size, bc, false, Paint.Align.RIGHT);
        rx -= measure(pct, size, false) + 5 * u;
        tx(c, icon, rx, base, size * 1.2f, bc, false, Paint.Align.RIGHT);
        rx -= measure(icon, size * 1.2f, false) + 14 * u;
        int tc = b.tempLevel == Battery.TEMP_HOT ? red : b.tempLevel == Battery.TEMP_WARM ? yellow : fg;
        String temp = Battery.fmt("%.0f°", b.temp);
        tx(c, temp, rx, base, size, tc, false, Paint.Align.RIGHT);
        rx -= measure(temp, size, false) + 4 * u;
        tx(c, "", rx, base, size, tc, false, Paint.Align.RIGHT);
    }

    // ------------------------------------------------------------ windows

    private void drawLevel(Canvas c, RectF r) {
        int sc = stateColor();
        btopBox(c, r, mix(green, bg, 0.35f), "¹", "battery", lc(b.sStatus), sc);
        float l = r.left + cw * 1.5f, rr = r.right - cw * 1.5f;

        // level meter along the bottom
        float mb = r.bottom - lh * 0.55f;
        tx(c, "lvl", l, mb, fs, dim, false, Paint.Align.LEFT);
        String pct = Battery.fmt("%3d%%", Math.round(b.level));
        tx(c, pct, rr, mb, fs, fg, false, Paint.Align.RIGHT);
        meter(c, l + 4 * cw, rr - 5 * cw, mb, b.level / 100f, red, green);

        // tty-clock digits
        float top = r.top + lh * 0.9f, bot = mb - lh * 1.1f;
        float cs = Math.min(13 * u, (bot - top) / 5f);
        String digits = b.sLevel + "%";
        float y = top + ((bot - top) - 5 * cs) / 2, x = l;
        pF.setShader(null);
        pF.setColor(sc);
        for (int i = 0; i < digits.length(); i++) {
            char ch = digits.charAt(i);
            int[] g = ch == '%' ? GLYPHS[10] : (ch >= '0' && ch <= '9') ? GLYPHS[ch - '0'] : null;
            if (g != null) {
                for (int row = 0; row < 5; row++) {
                    for (int col = 0; col < 3; col++) {
                        if ((g[row] >> (2 - col) & 1) == 0) continue;
                        float px = x + col * cs, py = y + row * cs;
                        c.drawRect(px, py, px + cs - 1.2f * u, py + cs - 1.2f * u, pF);
                    }
                }
            }
            x += 4 * cs;
        }

        // side readout
        float ix = Math.max(x + cw, l + 25 * cw), vx = ix + 7 * cw;
        float iy = y + 5 * cs / 2 - lh * 1.5f + fs * 0.36f;
        kv(c, "power", b.sWatts, ix, vx, iy, b.watts >= 0 ? green : magenta);
        kv(c, "eta", b.sEta, ix, vx, iy + lh, fg);
        kv(c, "", lc(b.sEtaSub), ix, vx, iy + 2 * lh, dim);
        kv(c, "src", lc(b.plugged != 0 ? b.sSrc : "battery"), ix, vx, iy + 3 * lh, fg);
    }

    private void drawSensors(Canvas c, RectF r) {
        btopBox(c, r, mix(yellow, bg, 0.35f), "²", "sensors", null, 0);
        float l = r.left + cw * 1.5f, rr = r.right - cw * 1.5f;
        String[] k = {"volt", "curr", "avg", "temp"};
        String[] v = {b.sVolt, b.sCur, b.sAvg, Battery.fmt("%.1f°C", b.temp)};
        float[] f = {(b.volts - 3.4f) / 1.1f, Math.abs(b.amps) / 3f, Math.abs(b.ampsAvg) / 3f, (b.temp - 20f) / 30f};
        int[][] grad = {{red, green}, {cyan, blue}, {cyan, blue}, {green, red}};
        float step = Math.min(lh * 1.35f, (r.height() - lh) / k.length);
        float y = r.top + lh * 0.6f + (r.height() - lh * 0.6f - step * k.length) / 2 + step / 2 + fs * 0.36f;
        float vx = l + 13 * cw;
        for (int i = 0; i < k.length; i++) {
            tx(c, k[i], l, y, fs, dim, false, Paint.Align.LEFT);
            int vc = i == 3 && b.tempLevel != Battery.TEMP_OK ? (b.tempLevel == Battery.TEMP_HOT ? red : yellow) : fg;
            tx(c, v[i], vx, y, fs, vc, false, Paint.Align.RIGHT);
            if (rr - vx > 3 * cw) meter(c, vx + cw, rr, y, f[i], grad[i][0], grad[i][1]);
            y += step;
        }
    }

    private void drawCell(Canvas c, RectF r) {
        btopBox(c, r, mix(blue, bg, 0.35f), "³", "cell", null, 0);
        float l = r.left + cw * 1.5f;
        String input = b.budgetW > 0 ? Battery.fmt("%.0fW max", b.budgetW) : "none";
        String[] k = {"charge", "full", "cycles", "health", "source", "input"};
        String[] v = {b.sCharge, b.sFull, b.sCycles, lc(b.sHealth), lc(b.sSrc), input};
        float step = Math.min(lh * 1.1f, (r.height() - lh) / k.length);
        float y = r.top + lh * 0.6f + (r.height() - lh * 0.6f - step * k.length) / 2 + step / 2 + fs * 0.36f;
        for (int i = 0; i < k.length; i++) {
            int vc = i == 3 && !"GOOD".equals(b.sHealth) ? yellow : fg;
            kv(c, k[i], v[i], l, l + 7.5f * cw, y, vc);
            y += step;
        }
    }

    private void drawPower(Canvas c, RectF r) {
        btopBox(c, r, mix(red, bg, 0.35f), "⁴", "power", Battery.HIST + "s", dim);
        float l = r.left + cw * 1.5f, rr = r.right - cw * 1.5f;
        int flow = b.watts >= 0 ? green : magenta;
        float big = fs * 2.1f;
        float y = r.top + lh * 0.6f + big;
        tx(c, b.sWatts, l, y, big, flow, true, Paint.Align.LEFT);
        String dir = b.watts > 0.05f ? "▲ into cell" : b.watts < -0.05f ? "▼ out of cell" : "• idle";
        tx(c, dir, l + measure(b.sWatts, big, true) + cw * 1.5f, y, fs, dim, false, Paint.Align.LEFT);
        tx(c, "peak " + b.sPeak, rr, y, fs, dim, false, Paint.Align.RIGHT);

        y += lh * 1.3f;
        float ref = b.budgetW > 0 ? b.budgetW : 15f;
        float ratio = Math.min(1f, Math.abs(b.watts) / ref);
        String cap = Battery.fmt("%3d%% of %.0fW", Math.round(ratio * 100), ref);
        tx(c, "load", l, y, fs, dim, false, Paint.Align.LEFT);
        tx(c, cap, rr, y, fs, fg, false, Paint.Align.RIGHT);
        meter(c, l + 5 * cw, rr - measure(cap, fs, false) - cw, y, ratio, green, red);

        float gy0 = y + lh * 0.7f, gy1 = r.bottom - lh * 0.4f;
        if (gy1 - gy0 > lh * 2) dotGraph(c, l, gy0, rr, gy1);
    }

    /** btop-style braille graph: two dots per cell across, four per line down, newest on the right. */
    private void dotGraph(Canvas c, float gx0, float gy0, float gx1, float gy1) {
        float px = cw / 2f, py = lh / 4f;
        int cols = (int) ((gx1 - gx0) / px), rows = (int) ((gy1 - gy0) / py);
        float max = 1f;
        for (int k = 0; k < b.histN; k++) max = Math.max(max, Math.abs(b.hist[k]));
        max *= 1.1f;
        int need = cols * rows * 2;
        if (ptsIn.length < need) {
            ptsIn = new float[need];
            ptsOut = new float[need];
        }
        int nIn = 0, nOut = 0;
        for (int j = 0; j < cols; j++) {
            int k = b.histN - 1 - j;
            if (k < 0) break;
            float v = b.hist[k];
            int hgt = Math.round(Math.abs(v) / max * rows);
            if (hgt == 0 && Math.abs(v) > 0.02f) hgt = 1;
            float x = gx1 - px * (j + 0.5f);
            for (int row = 0; row < hgt; row++) {
                float y = gy1 - py * (row + 0.5f);
                if (v >= 0) {
                    ptsIn[nIn++] = x;
                    ptsIn[nIn++] = y;
                } else {
                    ptsOut[nOut++] = x;
                    ptsOut[nOut++] = y;
                }
            }
        }
        resetStroke();
        pS.setStrokeCap(Paint.Cap.ROUND);
        pS.setStrokeWidth(px * 0.62f);
        if (nIn > 0) {
            pS.setShader(new LinearGradient(0, gy1, 0, gy0, mix(green, bg, 0.5f), green, Shader.TileMode.CLAMP));
            c.drawPoints(ptsIn, 0, nIn, pS);
        }
        if (nOut > 0) {
            pS.setShader(new LinearGradient(0, gy1, 0, gy0, mix(magenta, bg, 0.5f), magenta, Shader.TileMode.CLAMP));
            c.drawPoints(ptsOut, 0, nOut, pS);
        }
        resetStroke();
        tx(c, Battery.fmt("%.1fW", max / 1.1f), gx0, gy0 + fs, fs * 0.9f, dim, false, Paint.Align.LEFT);
        tx(c, "0", gx0, gy1, fs * 0.9f, dim, false, Paint.Align.LEFT);
    }

    /** A plain terminal that has just run fastfetch. */
    private void drawFetch(Canvas c, RectF r, float t) {
        float l = r.left + cw * 0.5f;
        float y = r.top + fs;
        prompt(c, l, y, "fastfetch", false, t);

        String host = lc(Build.DEVICE);
        long up = SystemClock.elapsedRealtime() / 60000;
        String uptime = (up >= 1440 ? (up / 1440) + "d " : "") + ((up % 1440) / 60) + "h " + (up % 60) + "m";
        String status = b.charging() ? "Charging" : b.state == Battery.FULL ? "Full"
                : b.discharging() ? "Discharging" : "Idle";
        String[][] rows = {
                {"OS", "Android " + Build.VERSION.RELEASE},
                {"Host", Build.MANUFACTURER + " " + Build.MODEL},
                {"Kernel", kernel},
                {"Uptime", uptime},
                {"Battery", b.sLevel + "% [" + status + "]"},
                {"Theme", name + " (Omarchy)"},
        };

        // pixel-art battery logo, filled to the current level
        float ps = Math.min(7 * u, (r.height() - lh * 3) / 12f);
        float ly = y + lh * 0.7f, lx = l + cw;
        int sc = stateColor();
        int filled = Math.round(Math.max(0, Math.min(100, b.level)) / 100f * 7);
        pF.setShader(null);
        for (int row = 0; row < 12; row++) {
            for (int col = 0; col < 8; col++) {
                int color = 0;
                if (row == 0) color = col >= 2 && col <= 5 ? accent : 0;
                else if (row == 1 || row == 11 || col == 0 || col == 7) color = accent;
                else if (row >= 3 && row <= 9 && col >= 2 && col <= 5)
                    color = 9 - row < filled ? sc : alpha(muted, 0x50);
                if (color == 0) continue;
                pF.setColor(color);
                float px = lx + col * ps, py = ly + row * ps;
                c.drawRect(px, py, px + ps - 0.8f * u, py + ps - 0.8f * u, pF);
            }
        }

        float ix = lx + 8 * ps + 3 * cw;
        int maxCols = (int) ((r.right - ix) / cw) - 9;
        float iy = y + lh;
        tx(c, "android", ix, iy, fs, accent, true, Paint.Align.LEFT);
        float aw = measure("android", fs, true);
        tx(c, "@", ix + aw, iy, fs, fg, false, Paint.Align.LEFT);
        tx(c, host, ix + aw + cw, iy, fs, accent, true, Paint.Align.LEFT);
        iy += lh;
        tx(c, "-------------------".substring(0, Math.min(19, 8 + host.length())), ix, iy, fs, fg, false,
                Paint.Align.LEFT);
        for (String[] row : rows) {
            iy += lh;
            String v = row[1].length() > maxCols ? row[1].substring(0, Math.max(0, maxCols - 1)) + "…" : row[1];
            tx(c, row[0], ix, iy, fs, accent, true, Paint.Align.LEFT);
            tx(c, ": " + v, ix + measure(row[0], fs, true), iy, fs, fg, false, Paint.Align.LEFT);
        }
        iy += lh * 0.6f;
        int[] blocks = {darkBg, red, green, yellow, blue, magenta, cyan, fg};
        for (int k = 0; k < blocks.length; k++) {
            pF.setColor(blocks[k]);
            float bx = ix + k * cw * 3;
            c.drawRect(bx, iy, bx + cw * 3, iy + lh * 0.75f, pF);
        }

        float py = Math.max(iy + lh * 2.1f, ly + 12 * ps + lh * 1.2f);
        if (py < r.bottom) prompt(c, l, py, "", true, t);
    }

    private void prompt(Canvas c, float x, float y, String cmd, boolean cursor, float t) {
        tx(c, "~", x, y, fs, blue, true, Paint.Align.LEFT);
        tx(c, "❯", x + cw * 2, y, fs, green, true, Paint.Align.LEFT);
        tx(c, cmd, x + cw * 4, y, fs, fg, false, Paint.Align.LEFT);
        if (cursor && (t % 1.1f) < 0.65f) {
            pF.setShader(null);
            pF.setColor(fg);
            c.drawRect(x + cw * 4, y - fs * 0.92f, x + cw * 5, y + fs * 0.22f, pF);
        }
    }

    // -------------------------------------------------------------- picker

    /** Mirrors Omarchy's walker menu: an input line, then the list with the current row selected. */
    @Override
    void drawPicker(Canvas c, Skin[] all, int current, RectF[] hits, float w, float h,
                    int insetTop, int insetBottom, float t) {
        u = w / 411f;
        fs = 10.5f * u;
        cw = measure("0", fs, false);
        lh = fs * 1.4f;
        pF.setShader(null);
        pF.clearShadowLayer();
        pF.setColor(alpha(darkBg, 0xB8));
        c.drawRect(0, 0, w, h, pF);

        float bw = Math.min(w - 40 * u, 330 * u);
        float avail = h - insetTop - insetBottom - 140 * u;
        float rowH = Math.min(28 * u, avail / all.length);
        float bh = 48 * u + rowH * all.length + 26 * u;
        float x0 = (w - bw) / 2, x1 = x0 + bw;
        float y0 = Math.max(insetTop + 30 * u, (h - bh) / 2), y1 = y0 + bh;
        box.set(x0, y0, x1, y1);
        pF.setColor(bg);
        c.drawRect(box, pF);
        resetStroke();
        pS.setStrokeWidth(2 * u);
        pS.setColor(accent);
        c.drawRect(box, pS);

        float size = 12 * u, base = y0 + 27 * u;
        tx(c, cp(0xF03D8), x0 + 16 * u, base, size * 1.1f, accent, false, Paint.Align.LEFT);
        tx(c, "Theme", x0 + 38 * u, base, size, fg, false, Paint.Align.LEFT);
        if ((t % 1.1f) < 0.65f) {
            float cx = x0 + 38 * u + measure("Theme", size, false) + 2 * u;
            pF.setColor(fg);
            c.drawRect(cx, base - size * 0.9f, cx + 1.4f * u, base + size * 0.2f, pF);
        }
        pS.setStrokeWidth(1 * u);
        pS.setColor(muted);
        c.drawLine(x0 + 10 * u, y0 + 40 * u, x1 - 10 * u, y0 + 40 * u, pS);

        float y = y0 + 46 * u;
        for (int i = 0; i < all.length; i++) {
            Skin s = all[i];
            boolean on = i == current;
            float ry0 = y + i * rowH, ry1 = ry0 + rowH;
            hits[i].set(x0 + 8 * u, ry0, x1 - 8 * u, ry1);
            if (on) {
                pF.setColor(sel);
                c.drawRect(hits[i], pF);
            }
            float mid = (ry0 + ry1) / 2, sq = Math.min(8 * u, rowH * 0.32f);
            int[] sw = s.swatch();
            for (int k = 0; k < sw.length; k++) {
                pF.setColor(sw[k]);
                float sx = x0 + 18 * u + k * (sq + 2 * u);
                c.drawRect(sx, mid - sq / 2, sx + sq, mid + sq / 2, pF);
            }
            float ts = Math.min(size, rowH * 0.5f);
            tx(c, s.name, x0 + 18 * u + 4 * (sq + 2 * u) + 10 * u, mid + ts * 0.36f, ts, on ? brightFg : fg, on,
                    Paint.Align.LEFT);
            tx(c, on ? "current" : lc(s.family), x1 - 18 * u, mid + fs * 0.36f, fs, on ? accent : dim, false,
                    Paint.Align.RIGHT);
        }
        tx(c, "tap outside to close", (x0 + x1) / 2, y1 - 9 * u, fs * 0.9f, dim, false, Paint.Align.CENTER);
    }
}
