package com.flyingkite.utils;

import android.util.Log;

import java.util.Locale;

public class Say {
    private static final String TAG = "Hi";
    public static void Log(String s) {
        Log.e(TAG, s);
    }

    public static void LogF(String format, Object... args) {
        Log.e(TAG, String.format(format, args));
    }

    public static void sleep(long ms) {
        Log("zz... " + ms + " ms");
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Log("!!! Interrupted " + e);
        } finally {
            Log("awake ^_^");
        }
    }

    public static String MMSSFFF(long ms) {
        if (ms < 0) return "-" + MMSSFFF(-ms);

        //ms = (ms + 50); // Round on frame
        final long s = ms / 1000;
        final long sec = s % 60;
        final long min = s / 60;
        //final long f = ms / 100 % 10;
        final long f = ms % 1000;
        return String.format(Locale.US, "%02d:%02d.%03d", min, sec, f);
    }
}
