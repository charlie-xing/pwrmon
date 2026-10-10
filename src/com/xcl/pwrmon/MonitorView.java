package com.xcl.pwrmon;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

/** Full-screen, self-drawn battery / charging dashboard with switchable skins. */
public class MonitorView extends View {
    private static final long SAMPLE_MS = 1000;
    private static final long FRAME_MS = 33;
    private static final String PREF_SKIN = "skin";

    private final Battery battery;
    private final Skin[] skins;
    private final RectF[] pickerHits;
    private final SharedPreferences prefs;
    private final long t0 = SystemClock.uptimeMillis();
    private final int touchSlop;

    private int current;
    private boolean pickerOpen;
    private OnBackInvokedCallback backCallback;
    private int insetTop, insetBottom;
    private boolean running;
    private float downX, downY;

    private final Runnable sampler = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            battery.sample();
            postDelayed(this, SAMPLE_MS);
        }
    };

    public MonitorView(Context ctx) {
        super(ctx);
        battery = new Battery(ctx);
        Typeface reg = Typeface.createFromAsset(ctx.getAssets(), "fonts/JetBrainsMonoNF-Regular.ttf");
        Typeface bold = Typeface.createFromAsset(ctx.getAssets(), "fonts/JetBrainsMonoNF-Bold.ttf");
        Skin[] omarchy = OmarchySkin.all(reg, bold);
        skins = new Skin[omarchy.length + 1];
        skins[0] = new CyberSkin();
        System.arraycopy(omarchy, 0, skins, 1, omarchy.length);
        pickerHits = new RectF[skins.length];
        for (int i = 0; i < skins.length; i++) pickerHits[i] = new RectF();

        prefs = ctx.getSharedPreferences("pwrmon", Context.MODE_PRIVATE);
        String saved = prefs.getString(PREF_SKIN, skins[0].id);
        for (int i = 0; i < skins.length; i++) if (skins[i].id.equals(saved)) current = i;

        touchSlop = ViewConfiguration.get(ctx).getScaledTouchSlop();
        setContentDescription("PWR//MON battery monitor");
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
        applyBarAppearance();
        removeCallbacks(sampler);
        post(sampler);
        invalidate();
    }

    public void stop() {
        running = false;
        removeCallbacks(sampler);
    }

    /** Closes the theme picker if it is open; returns whether it was. */
    public boolean closePicker() {
        if (!pickerOpen) return false;
        setPicker(false);
        return true;
    }

    private void setPicker(boolean open) {
        pickerOpen = open;
        invalidate();
        if (Build.VERSION.SDK_INT < 33) return;
        OnBackInvokedDispatcher d = findOnBackInvokedDispatcher();
        if (d == null) return;
        if (open && backCallback == null) {
            backCallback = () -> setPicker(false);
            d.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
        } else if (!open && backCallback != null) {
            d.unregisterOnBackInvokedCallback(backCallback);
            backCallback = null;
        }
    }

    private void select(int i) {
        current = i;
        prefs.edit().putString(PREF_SKIN, skins[i].id).apply();
        applyBarAppearance();
    }

    private void applyBarAppearance() {
        WindowInsetsController wic = getWindowInsetsController();
        if (wic == null) return;
        int light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        wic.setSystemBarsAppearance(skins[current].light() ? light : 0, light);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        applyBarAppearance();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                return true;
            case MotionEvent.ACTION_UP:
                if (Math.hypot(e.getX() - downX, e.getY() - downY) > touchSlop) return true;
                tap(e.getX(), e.getY());
                performClick();
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void tap(float x, float y) {
        if (pickerOpen) {
            for (int i = 0; i < skins.length; i++) {
                if (pickerHits[i].contains(x, y)) {
                    select(i);
                    break;
                }
            }
            setPicker(false);
        } else if (skins[current].menuButton.contains(x, y)) {
            setPicker(true);
        } else {
            skins[current].onTap(x, y);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float t = (SystemClock.uptimeMillis() - t0) / 1000f;
        Skin s = skins[current];
        s.draw(c, battery, w, h, insetTop, insetBottom, t);
        if (pickerOpen) s.drawPicker(c, skins, current, pickerHits, w, h, insetTop, insetBottom, t);
        if (running) postInvalidateDelayed(FRAME_MS);
    }
}
