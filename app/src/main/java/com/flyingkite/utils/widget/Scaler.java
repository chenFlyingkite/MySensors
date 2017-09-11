package com.cyberlink.actiondirector.widget;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Keep ScaleDetector information for Timeline related component.
 * This component only handle one axis, depend on caller.
 */
public class Scaler {

    interface OnScaleListener {
        /**
         * Notify the listener to scale its component. This will be fired after all listeners accept the request.
         *
         * @param focus
         */
        void onScale(float focus);

        /**
         * Notify the listener that the scaling is end. This will be fired only after scaling is occurred. (No begin, no end).
         */
        void onScaleEnd();
    }

    private static final int MIN_ZOOM_IN_US = 2200000; // 2.2 seconds
    private static final int MAX_ZOOM_OUT_US = 11 * 60 * 1000000; // 11 minutes

    private final double baseUsPerPx;
    private final double minUsPerPx;
    private final double maxUsPerPx;

    private double usPerPx = 0;

    // Reference from View#OnLayoutChangeListener
    // We use ArrayList instead of HashSet because ArrayList support clone method.
    private final List<OnScaleListener> listeners = new ArrayList<>();

    public Scaler(Context context, double usPerPx_unit) {
        baseUsPerPx = usPerPx = usPerPx_unit;

        int halfWidthInPixel = getScreenMaxWidth(context) / 2;
        if (halfWidthInPixel == 0) halfWidthInPixel = 960;
        minUsPerPx = MIN_ZOOM_IN_US / halfWidthInPixel;
        maxUsPerPx = MAX_ZOOM_OUT_US / halfWidthInPixel;
    }

    private static int getScreenMaxWidth(Context context) {
        try {
            Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            return Math.max(metrics.widthPixels, metrics.heightPixels);
        } catch (Throwable e) {
            return 1920; // Allow ADT to go
        }
    }

    /**
     * Scale with the factor argument.
     * @param factor
     * @param focus
     * @return <code>true</code> means scaling successfully. <code>false</code> means cannot scale.
     */
    protected boolean scale(final float factor, final float focus) {
        final double newUsPerPx = usPerPx / factor;
        if (usPerPx != newUsPerPx && isInRange(newUsPerPx)) {
            usPerPx = newUsPerPx;
            notifyScaleListeners(focus);
            return true;
        }
        return false;
    }

    private boolean isInRange(double usPerPx) {
        return usPerPx >= minUsPerPx && usPerPx <= maxUsPerPx;
    }

    protected void scaleEnd() {
        for (OnScaleListener listener : listeners) {
            listener.onScaleEnd();
        }
    }

    /**
     * How many microseconds per pixel identify.
     *
     * <p><b>NOTE</b>: expose {@code double} type to keep precision.
     * @return
     */
    public double getUsPerPixel() {
        return usPerPx;
    }

    public double getPixelsPerUs() {
        return 1.0 / usPerPx;
    }

    public double getDisplayRatio() {
        return baseUsPerPx / usPerPx;
    }

    protected void addOnScaleListener(OnScaleListener listener) {
        listeners.add(listener);
    }

    protected void removeOnScaleListener(OnScaleListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    private void notifyScaleListeners(final float focus) {
        for (OnScaleListener listener : listeners) {
            listener.onScale(focus);
        }
    }
}
