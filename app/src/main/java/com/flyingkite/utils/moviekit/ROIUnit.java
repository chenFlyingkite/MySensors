package com.cyberlink.actiondirector.page.editor.moviekit;

import android.graphics.RectF;

public interface ROIUnit {
    /**
     * Returns the width of ROI range is 1.
     * Value should be stable and unchanged during ROIKit is receiving touch event
     */
    int getMovieViewWidth();

    /**
     * Returns the height of ROI range is 1.
     * Value should be stable and unchanged during ROIKit is receiving touch event
     */
    int getMovieViewHeight();

    void setMovieViewPosition(int x, int y, int width, int height, float scaleX, float scaleY, RectF roi);

    void setMovieViewVisible(boolean visible);
}
