package com.xcl.pwrmon;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Samples the battery once per call and keeps skin-neutral readings and display strings. */
final class Battery {
    static final int HIST = 120;

    static final int FULL = 0, CHARGING = 1, DISCHARGING = 2, LOW = 3, IDLE = 4;
    static final int TEMP_OK = 0, TEMP_WARM = 1, TEMP_HOT = 2;

    private final Context ctx;
    private final BatteryManager bm;
    private final SimpleDateFormat clockFmt = new SimpleDateFormat("HH:mm:ss", Locale.US);
    private boolean sysDenied;

    // latest sample
    float level, volts, amps, ampsAvg, watts, temp, budgetW, peakW;
    int status, plugged, health, cycles = -1;
    int state = IDLE, tempLevel = TEMP_OK;
    final float[] hist = new float[HIST];
    int histN;
    String sLevel = "--", sStatus = "NO DATA", sSource = "", sClock = "";
    String sWatts = "--", sVolt = "--", sVoltSub = "", sCur = "--", sCurSub = "";
    String sTemp = "--", sTempSub = "", sEta = "--", sEtaSub = "";
    String sCharge = "--", sChargeSub = "", sCycles = "--", sCyclesSub = "";
    String sSrc = "--", sSrcSub = "", sPeak = "--", sPeakSub = "";
    String sAvg = "--", sFull = "--", sHealth = "UNKNOWN";

    Battery(Context ctx) {
        this.ctx = ctx;
        bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
    }

    boolean charging() {
        return status == BatteryManager.BATTERY_STATUS_CHARGING;
    }

    boolean discharging() {
        return status == BatteryManager.BATTERY_STATUS_DISCHARGING;
    }

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

    static String fmt(String f, Object... a) {
        return String.format(Locale.US, f, a);
    }

    private static String hm(double hours) {
        if (hours <= 0 || hours > 99) return "--";
        int m = (int) Math.round(hours * 60);
        return m >= 60 ? fmt("%dh %02dm", m / 60, m % 60) : fmt("%d min", m);
    }

    void sample() {
        Intent i = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
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
        boolean charging = charging();
        boolean discharging = discharging();
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
            state = FULL;
            sStatus = "FULL";
        } else if (charging) {
            state = CHARGING;
            sStatus = "CHARGING";
        } else if (discharging) {
            state = level <= 15 ? LOW : DISCHARGING;
            sStatus = level <= 15 ? "LOW POWER" : "DISCHARGING";
        } else {
            state = IDLE;
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
        sAvg = fmt("%+d mA", Math.round(ampsAvg * 1000));
        sCurSub = "AVG " + sAvg;
        sTemp = fmt("%.1f °C", temp);
        if (temp >= 43) {
            sTempSub = "HOT";
            tempLevel = TEMP_HOT;
        } else if (temp >= 38) {
            sTempSub = "WARM";
            tempLevel = TEMP_WARM;
        } else {
            sTempSub = "NOMINAL";
            tempLevel = TEMP_OK;
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
        sFull = estFull > 0 ? fmt("%d mAh", Math.round(estFull)) : "--";
        sChargeSub = estFull > 0 ? "EST FULL " + sFull : "";
        sCycles = cycles >= 0 ? fmt("%d", cycles) : "--";
        sHealth = healthName(health);
        sCyclesSub = "HEALTH " + sHealth;
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
}
