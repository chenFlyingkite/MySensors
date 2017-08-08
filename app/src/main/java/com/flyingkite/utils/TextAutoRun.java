package com.flyingkite.utils;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

public class TextAutoRun implements Runnable {
    private static final Handler ui = new Handler(Looper.getMainLooper());

    private static final String ruler
            = "123456789-123456789=123456789%123456789#123456789@"
            + "123456789-123456789=123456789%123456789#123456789@"
            + "123456789-123456789=123456789%123456789#123456789@";

    private TextView textView;
    // for (i = min:step:max)
    private int min = 1;
    private int step = 1;
    private int max = ruler.length();
    private int now = min;
    private long speed = 100; // 100ms
    private boolean started = false;

    public TextAutoRun(TextView txt) {
        textView = txt;
    }

    public TextAutoRun from(int start) {
        now = min = start;
        return this;
    }

    public TextAutoRun to(int end) {
        max = end;
        return this;
    }

    public TextAutoRun step(int add) {
        step = add;
        return this;
    }

    public TextAutoRun speed(int delayMillis) {
        speed = delayMillis;
        return this;
    }

    public void pause() {
        ui.removeCallbacks(this);
    }

    public void resume() {
        if (started) {
            ui.removeCallbacks(this);
            ui.postDelayed(this, speed);
        }
    }

    @Override
    public void run() {
        started = true;

        if (textView == null) return;

        now = setText(textView, now, max);
        ui.postDelayed(this, speed);
    }

    private int setText(TextView v, int n, int max) {
        v.setText(ruler.substring(0, n));
        return next(n, max);
    }

    private int next(int value, int max) {
        return value == max ? min : (value + step);
    }
}
