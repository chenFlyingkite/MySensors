package com.cyberlink.actiondirector.page.editor.moviekit;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.IntRange;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.cyberlink.util.MathUtils;

public class MovieROIKit implements View.OnTouchListener {
    private static final boolean DEBUG_TOUCH = false;
    private boolean isEnabled;
    private final ROIUnit mROIUnit;
    private final float SCALE_MIN = 1.0f;
    /**
     * SCALE_MAX will be changed by media size. {@link #setMaxScale(float)}
     */
    private float SCALE_MAX = 3.0f;

    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private final PointF lastTouch = new PointF(0, 0);

    private ScaleGestureDetector scaleDetector;
    private float scale = SCALE_MIN;
    private final PointF viewer = new PointF(0, 0);

    public static final RectF ALL_ROI = new RectF(0, 0, 1, 1);

    private ROIListener roiListener;

    /**
     * Ensure the Rect of ROI falls within image. Just as {@link RectF#contains(RectF)} says.
     * Thus those ROIs return by listener will have <b>ALL_ROI.contains(roi) = true</b>
     */
    public static final int ROI_CONTAIN = 0;

    /**
     * Ensure the Rect of ROI intersects with image. Just as {@link RectF#intersect(RectF)} says.
     * Thus those ROIs return by listener will have <b>ALL_ROI.intersect(roi) = true</b>
     */
    public static final int ROI_INTERSECT = 1;

    /** No constraint, free of the space */
    public static final int ROI_FREE = 2;

    private int modeOfROI = ROI_CONTAIN;

    public interface ROIListener {
        void onMoveROI(RectF roiMoving);
        void onNewROI(RectF roiEnded);
    }

    public MovieROIKit(Context context, ROIUnit roiUnit) {
        mROIUnit = roiUnit;
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        isEnabled = roiUnit != null;
    }

    public void reset() {
        activePointerId = MotionEvent.INVALID_POINTER_ID;
        lastTouch.set(0, 0);
        viewer.set(0, 0);
        scale = SCALE_MIN;
        if (isEnabled) {
            setROIUnitViewPosition();
        }
    }

    public void setROIListener(ROIListener listener) {
        roiListener = listener;
    }

    public void setROIMode(@IntRange(from=ROI_CONTAIN, to=ROI_FREE) int mode) {
        modeOfROI = mode;
    }

    public void setMaxScale(float maxScale) {
        SCALE_MAX = maxScale;
    }

    private RectF evaluateROI() {
        float width = mROIUnit.getMovieViewWidth();
        float height = mROIUnit.getMovieViewHeight();

        float offsetX = -viewer.x;
        float offsetY = -viewer.y;

        float left = offsetX / width / scale;
        float top = offsetY / height / scale;
        float right = left + 1 / scale; // = (offsetX + width * scale) / width / scale
        float bottom = top + 1 / scale; // = (offsetY + height * scale) / height / scale

        return new RectF(left, top, right, bottom);
    }

    public void showROI(RectF roi) {
        if (roi == null || !isEnabled) return;

        // Convert from RectF to scale, viewX & viewY
        // To show the crop view's position

        float width = mROIUnit.getMovieViewWidth();
        float height = mROIUnit.getMovieViewHeight();

        float sx = 1F / (roi.right - roi.left); // sx = scale
        float sy = 1F / (roi.bottom - roi.top); // sy = scale, sx & sy should be same
        scale = MathUtils.fallInRange((sx + sy) / 2, SCALE_MIN, SCALE_MAX); // Use mean if sx != sy (since it may have floating number missing when division)
        float viewX = - roi.left * width * scale;
        float viewY = - roi.top * height * scale;
        viewer.set(viewX, viewY);

        setROIUnitViewPosition();
    }

    private void setROIUnitViewPosition() {
        mROIUnit.setMovieViewPosition(Math.round(viewer.x), Math.round(viewer.y)
                , Math.round(scale * mROIUnit.getMovieViewWidth()), Math.round(scale * mROIUnit.getMovieViewHeight())
                , scale, scale, evaluateROI());
    }

    private RectF boundIfUnderZoomed(RectF roiRect) {
        RectF result = new RectF(roiRect);
        if (modeOfROI == ROI_FREE) {
            // No need bound
        } else if (modeOfROI == ROI_CONTAIN) {
            if (result.left < 0) { // Hit left bound
                float newRight = result.right - result.left;
                result.left = 0;
                result.right = newRight;
            }

            if (result.right > 1) { // Hit right bound
                float newLeft = Math.max(0, result.left - (result.right - 1));
                result.left = newLeft;
                result.right = 1;
            }

            if (result.top < 0) { // Hit top bound
                float newBottom = result.bottom - result.top;
                result.top = 0;
                result.bottom = newBottom;
            }

            if (result.bottom > 1) { // Hit bottom bound
                float newTop = Math.max(0, result.top - (result.bottom - 1));
                result.top = newTop;
                result.bottom = 1;
            }
        } else if (modeOfROI == ROI_INTERSECT) {
            if (result.right < 0) { // Hit left bound
                float newLeft = result.left - result.right;
                result.left = newLeft;
                result.right = 0;
            }

            if (result.left > 1) { // Hit right bound
                float newRight = 1 + result.right - result.left;
                result.left = 1;
                result.right = newRight;
            }

            if (result.bottom < 0) { // Hit top bound
                float newTop = result.top - result.bottom;
                result.top = newTop;
                result.bottom = 0;
            }

            if (result.top > 1) { // Hit bottom bound
                float newBottom = 1 + result.bottom - result.top;
                result.top = 1;
                result.bottom = newBottom;
            }
        }

        return result;
    }

    private RectF getValidMoveRange() {
        int W = mROIUnit.getMovieViewWidth();
        int H = mROIUnit.getMovieViewHeight();
        float minX, maxX, minY, maxY;

        switch (modeOfROI) {
            default:
            case ROI_CONTAIN:
                // Let S = scale, W = movieViewWidth, H = movieViewHeight
                // +--+ => Image, size = (SW, SH)
                // #--# => ROI, size = (W, H)
                //
                //      minX,             maxX
                //       SW                SW
                //   +---------+       +---------+
                //   |         |       |         |
                //   |         |       |         |
                // S L     A---#     S #---#     |
                // H |     |   |     H |   |     |
                //   |     #---#       #---#     |
                //   |         |       |         |
                //   +---------+       +---------+
                //
                //      minY,             maxY
                //       SW                SW
                //   +--#---#--+       +--T------+
                //   |  |   |  |       |         |
                //   |  #---#  |       |         |
                // S |         |     S |         |
                // H |         |     H |         |
                //   |         |       |  A---#  |
                //   |         |       |  |   |  |
                //   +---------+       +--#---#--+
                //
                // minX = -|LA| = W - SW = (1 - S) * W, maxX = 0
                // minY = -|TA| = H - SH = (1 - S) * H, maxY = 0
                minX = (1 - scale) * W;
                maxX = 0;
                minY = (1 - scale) * H;
                maxY = 0;
                break;
            case ROI_INTERSECT:
                //       SW                   SW
                //   +---------+               +---------+
                //   |         |               |         |
                //   |         |               |         |
                // S L         A---#     S #---#         |
                // H |         |   |     H |   |         |
                //   |         #---#       #---#         |
                //   |         |               |         |
                //   +---------+               +---------+
                //
                //       SW               SW
                //      #---#
                //      |   |
                //   +--#---#--+       +--T------+
                //   |         |       |         |
                //   |         |       |         |
                // S |         |     S |         |
                // H |         |     H |         |
                //   |         |       |         |
                //   |         |       |         |
                //   +---------+       +--A---#--+
                //                        |   |
                //                        #---#
                //
                // minX = -|LA| = -SW, maxX = W
                // minY = -|TA| = -SH, maxY = H
                minX = -scale * W;
                maxX = W;
                minY = -scale * H;
                maxY = H;
                break;
            case ROI_FREE:
                minX = Float.NEGATIVE_INFINITY;
                maxX = Float.POSITIVE_INFINITY;
                minY = Float.NEGATIVE_INFINITY;
                maxY = Float.POSITIVE_INFINITY;
                break;
        }

        return new RectF(minX, minY, maxX, maxY);
    }

    @Override
    public boolean onTouch(View v, MotionEvent e) {
        if (!isEnabled) return false;

        scaleDetector.onTouchEvent(e);

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                // 1. Records last touch
                lastTouch.set(e.getX(), e.getY());
                LogF("_ touch  (x, y) = (%s, %s)", lastTouch.x, lastTouch.y);
                activePointerId = e.getPointerId(0);
            }   break;
            case MotionEvent.ACTION_MOVE: {
                // 1. Prepare offset's valid range
                RectF range = getValidMoveRange();

                // 2. Evaluate (user moved) offset x & y
                int pointerIndex = e.findPointerIndex(activePointerId);
                PointF pointer = new PointF(e.getX(pointerIndex), e.getY(pointerIndex));
                float offset_X = pointer.x - lastTouch.x;
                float offset_Y = pointer.y - lastTouch.y;

                // 3. Constraint user's move x & y into viewer's valid range
                // Constraint offset_X to ensure minX <= viewer.x + offset_X <= maxX
                offset_X = MathUtils.fallInRange(offset_X, range.left - viewer.x, range.right - viewer.x);

                // Constraint offset_Y to ensure minY <= viewer.y + offset_Y <= maxY
                offset_Y = MathUtils.fallInRange(offset_Y, range.top - viewer.y, range.bottom - viewer.y);

                LogF("~  viewer (x, y) = (%12s, %12s), (dx, dy) = (%12s, %12s)", viewer.x, viewer.y, offset_X, offset_Y);

                // 3-1. Move the MovieView if user is not performing scaling and moving with single pointer
                if (!scaleDetector.isInProgress() && e.getPointerCount() == 1) {
                    // 3-2. Move viewer to (viewer.x + offset_X, viewer.y + offset_Y)
                    viewer.offset(offset_X, offset_Y);
                    LogF("  ~ move to (x, y) = (%12s, %12s)", viewer.x, viewer.y);

                    setROIUnitViewPosition();
                }

                // 4. Records last touch
                lastTouch.set(pointer.x, pointer.y);

                // 5. Reports to listener
                if (roiListener != null) {
                    roiListener.onMoveROI(evaluateROI());
                }
            }   break;
            case MotionEvent.ACTION_UP: {
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                // 1. Evaluates ROI
                RectF oldRoi = evaluateROI();
                RectF fixedRoi = boundIfUnderZoomed(oldRoi);

                LogF("^ (x, y) = (%12s, %12s), same = %s, roi : old = %s, fixed = %s", e.getX(), e.getY(), oldRoi.equals(fixedRoi) ? "o" : "x", oldRoi, fixedRoi);

                // 2-1. If user has under-scaled (so outside's black strips appears)
                if (!oldRoi.equals(fixedRoi)) {
                    // 2-2. Move viewer back to valid range, (viewer.x + back_X, viewer.y + back_Y)
                    float back_X = scale * (oldRoi.left - fixedRoi.left) * mROIUnit.getMovieViewWidth();
                    float back_Y = scale * (oldRoi.top - fixedRoi.top) * mROIUnit.getMovieViewHeight();
                    viewer.offset(back_X, back_Y);

                    LogF("  ^ move to (%12s, %12s), shift (%12s, %12s)", viewer.x, viewer.y, back_X, back_Y);
                    setROIUnitViewPosition();
                }

                // 3. Report to listener
                if (roiListener != null) {
                    roiListener.onNewROI(fixedRoi);
                }
            }    break;
            case MotionEvent.ACTION_CANCEL: {
                activePointerId = MotionEvent.INVALID_POINTER_ID;
            }    break;
            case MotionEvent.ACTION_POINTER_UP: {
                int pointerIndex1 = e.getActionIndex();
                int pointerId = e.getPointerId(pointerIndex1);

                if (pointerId == activePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex1 == 0 ? 1 : 0;
                    lastTouch.set(e.getX(newPointerIndex), e.getY(newPointerIndex));
                    activePointerId = e.getPointerId(newPointerIndex);
                }
            }    break;
            default: {
                LogF("Omit e = %s", e);
            }
        }

        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (!isEnabled) return false;

            // 1. Evaluate new scale
            float oldScale = scale;
            scale *= detector.getScaleFactor();
            scale = MathUtils.fallInRange(scale, SCALE_MIN, SCALE_MAX);

            // 2. Update view's left/top value with respect to new scale
            float dx = (oldScale - scale) * 0.5F * mROIUnit.getMovieViewWidth();
            float dy = (oldScale - scale) * 0.5F * mROIUnit.getMovieViewHeight();
            viewer.offset(dx, dy);

            // 3. Update to MovieView
            setROIUnitViewPosition();

            LogF("scale = %s", scale);

            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (!isEnabled) return;

            if (scale == SCALE_MIN) {
                LogF("scale min");
                if (modeOfROI == ROI_CONTAIN) {
                    viewer.set(0, 0);
                    setROIUnitViewPosition();

                    // 5. Reports to listener
                    if (roiListener != null) {
                        roiListener.onMoveROI(ALL_ROI);
                    }
                }
            }
            LogF("scale end = %s", scale);
        }
    }

    private static void LogF(String format, Object... args) {
        if (DEBUG_TOUCH) {
            Log.e("ROIKit", String.format(format, args));
        }
    }
}
