package com.cyberlink.actiondirector.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.cyberlink.actiondirector.R;

public class ResizerView extends FrameLayout { // Use FrameLayout to help onMeasure & onLayout
    private static final boolean DEBUG_TOUCH = false;
    private static final int MIN_CLICK_WIDTH = 50;
    private static final int MIN_CLICK_HEIGHT = 50;
    private static final int MIN_CONTENT_WIDTH = 50;
    private static final int MIN_CONTENT_HEIGHT = 50;
    private final int[] CORNER_IDS = {R.id.rz_control_corner_left_top
            , R.id.rz_control_corner_right_top
            , R.id.rz_control_corner_left_bottom
            , R.id.rz_control_corner_right_bottom};

    private View controller;
    private View contentRV; // content to resize view
    private ImageView contentRVImage; // content of image
    private View[] corners = new View[CORNER_IDS.length]; // Corner buttons

    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private final PointF contentSize = new PointF(0, 0);
    private final PointF viewer = new PointF(0, 0);
    private float contentDegree = 0;
    private boolean performCornerClick = true;

    private ScaleGestureDetector scaleDetector;
    private DegreeListener degreeListener;
    private OnContentResizeListener resizeListener;
    private OnCornerButtonClickListener buttonListener;

    public interface OnContentResizeListener {
        void onNewPosition(float centerX, float centerY, float width, float height, float degree);
    }

    public interface DegreeListener {
        /**
         * Notified when user is performing rotation
         * @param originalDegree the original degree resizer provides, value range = [0, 360)
         * @return the degree that resizer need to show up
         */
        float onRotating(float originalDegree);
    }

    public interface OnCornerButtonClickListener extends OnClickListener { }

    public ResizerView(Context context) {
        this(context, null);
    }

    public ResizerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ResizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ResizerView);
        a.recycle();

        controller = inflate(context, R.layout.layout_resize_controller_view, null);
        addView(controller);

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        contentRV = findViewById(R.id.rz_content);
        contentRVImage = (ImageView) contentRV.findViewById(R.id.rz_content_image);
        setOnTouchListener(touchListener);
        setCornerButtons();
    }

    public void setDegreeListener(DegreeListener listener) {
        degreeListener = listener;
    }

    public void setResizeListener(OnContentResizeListener listener) {
        resizeListener = listener;
    }

    public void setCornerButtonClickListener(OnCornerButtonClickListener listener) {
        buttonListener = listener;
    }

    public void setContent(Drawable bd) {
        contentRVImage.setImageDrawable(bd);
    }

    public void showContentAt(float centerX, float centerY, float width, float height, float degree) {
        PointF toMargin = toMarginXY(width, height);

        viewer.set(centerX + toMargin.x, centerY + toMargin.y);
        contentSize.set(width, height);
        contentDegree = degree;
        controller.setRotation(degree);
        resizeView();
    }

    private void setCornerButtons() {
        for (int i = 0; i < CORNER_IDS.length; i++) {
            corners[i] = findViewById(CORNER_IDS[i]);
        }
    }

    private boolean isNoContent() {
        return controller == null || contentRV == null;
    }

    private View findCornerButton(float x, float y) {
        for (View v : corners) {
            RectF rect = getRectOnScreen(v);
            if (rect.contains(x, y)) {
                return v;
            }
        }
        return null;
    }

    /** Returns the view's rectangle on the screen, regardless of rotation 90
     * */
    private RectF getRectOnScreen(View v) {
        if (v == null) return new RectF();

        // Parameter for rotation, this is to fix the rect that view has ever rotated
        float degree = controller.getRotation();

        Rect r = new Rect();
        int[] xy = new int[2];

        // 1. Fetching the left-top point as xy, and its width height
        v.getHitRect(r);
        v.getLocationOnScreen(xy);

        RectF original = new RectF(xy[0], xy[1], xy[0] + r.width(), xy[1] + r.height());

        // 2. Prepare rotation matrix of rotate degree counter-clockwise
        // Note : If degree = 0, then directly return original is a good solution
        // Prerequisites:
        //              [ x ]               [ Scale_X,  Skew_Y, Persp_0]
        // If point p = [ y ],  Matrix is = [  Skew_X, Scale_Y, Persp_1]
        //              [ 1 ]               [ Trans_X, Trans_Y, Persp_2]
        //
        //             [ x ]                       [ 1, 0, a]                 [ 1, 0, a]   [ x+a ]
        // So move p = [ y ] by (a, b) to q -> T = [ 0, 1, b], so q = T * p = [ 0, 1, b] = [ y+b ]
        //             [ 1 ]                       [ 0, 0, 1]                 [ 0, 0, 1]   [  1  ]
        // ---
        // Let d = rotation degree
        //
        //         [ x ]       [  1,  0,  0]         [  cos(d), -sin(d),  0]
        // view =  [ y ],  I = [  0,  1,  0], so m = [  sin(d),  cos(d),  0]
        //         [ z ]       [  0,  0,  1]         [       0,       0,  1]
        Matrix m = new Matrix(); // m <- I
        m.postRotate(degree); // m <- R(degrees) * m

        // 3. Rotate the original rect on pivot of left-top (xy)
        RectF result = new RectF(original);
        result.offset(-xy[0], -xy[1]);
        m.mapRect(result);
        result.offset(xy[0], xy[1]);

        return result;
    }

    private OnTouchListener touchListener = new OnTouchListener() {
        private final PointF lastTouch = new PointF(0, 0);
        private final PointF rotateAnchor = new PointF(0, 0);
        private final PointF downRawTouch = new PointF(0, 0);
        private final PointF downContentSize = new PointF(0, 0);
        private float downRotation;
        private View cornerButton;

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            if (isNoContent()) return false;

            scaleDetector.onTouchEvent(e);

            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    // 1. Records last touch
                    lastTouch.set(e.getX(), e.getY());
                    downRawTouch.set(e.getRawX(), e.getRawY());
                    LogF("_ touch  (x, y) = (%s, %s)", lastTouch.x, lastTouch.y);
                    activePointerId = e.getPointerId(0);
                    contentSize.set(contentRV.getWidth(), contentRV.getHeight());
                    downContentSize.set(contentRV.getWidth(), contentRV.getHeight());

                    cornerButton = findCornerButton(e.getRawX(), e.getRawY());
                    if (cornerButton != null) {
                        cornerButton.setPressed(true);
                    }

                    RectF rect = getRectOnScreen(contentRV);
                    rotateAnchor.set(rect.centerX(), rect.centerY());
                    LogF("_ r anchor = %s, %s", rotateAnchor.x, rotateAnchor.y);
                    downRotation = controller.getRotation();

                    performCornerClick = true;
                }   break;
                case MotionEvent.ACTION_MOVE: {
                    // 1. Prepare the max offset we can move // TODO : Fix me
                    boolean onePointer = e.getPointerCount() == 1;

                    // 2. Evaluate the offset x, y user moved
                    int pointerIndex = e.findPointerIndex(activePointerId);
                    PointF pointerRaw = new PointF(e.getRawX(), e.getRawY());
                    PointF pointer = new PointF(e.getX(pointerIndex), e.getY(pointerIndex));
                    float offset_X = pointer.x - lastTouch.x;
                    float offset_Y = pointer.y - lastTouch.y;

                    // 3. Constraint user's move x & y into viewer's valid range
                    // TODO : Remove me
                    //LogF("~ # bound dx in (%12s, %12s), dx = %s, max.x = %s", -maxOffset.x - viewer.x, maxOffset.x - viewer.x, offset_X, maxOffset.x);
                    //offset_X = MathUtils.fallInRange(offset_X, -maxOffset.x - viewer.x, maxOffset.x - viewer.x);
                    // TODO : Remove me
                    //LogF("~ # bound dy in (%12s, %12s), dy = %s, max.y = %s", -maxOffset.y - viewer.y, maxOffset.y - viewer.y, offset_Y, maxOffset.y);
                    //offset_Y = MathUtils.fallInRange(offset_Y, -maxOffset.y - viewer.y, maxOffset.y - viewer.y);

                    LogF("~  viewer (x, y) = (%12s, %12s), (dx, dy) = (%12s, %12s)", viewer.x, viewer.y, offset_X, offset_Y);

                    // 3. Move the resizer's content
                    if (scaleDetector.isInProgress()) { // User is scaling
                        // onScale has handled for us
                    } else if (cornerButton != null) { // User clicks on corner button
                        // 3-1. Evaluate degree of user's point
                        float move_X = pointerRaw.x - rotateAnchor.x;
                        float move_Y = pointerRaw.y - rotateAnchor.y;
                        PointF move = new PointF(move_X, move_Y);
                        double moveDegree = evaluateDegree(move);

                        // 3-2. Evaluate degree of down point
                        float down_X = downRawTouch.x - rotateAnchor.x;
                        float down_Y = downRawTouch.y - rotateAnchor.y;
                        PointF down = new PointF(down_X, down_Y);
                        double downDegree = evaluateDegree(down);

                        // 3-3. Evaluate the new degree for user
                        contentDegree = downRotation + (float)(moveDegree - downDegree);
                        contentDegree = normalizeDegreeTo0_360(contentDegree);
                        if (degreeListener != null) {
                            contentDegree = degreeListener.onRotating(contentDegree);
                        }
                        controller.setRotation(contentDegree);

                        // 3-4. Evaluate new scale
                        float scale = move.length() / down.length();
                        float newW = scale * downContentSize.x;
                        float newH = scale * downContentSize.y;

                        setResizerContentSize(newW, newH);
                        resizeView();
                    } else if (onePointer) { // User move with single finger
                        // 3-2. Move viewer to (viewer.x + offset_X, viewer.y + offset_Y)
                        viewer.offset(offset_X, offset_Y);
                        LogF("  ~ move to (x, y) = (%12s, %12s)", viewer.x, viewer.y);

                        resizeView();
                    }

                    // 4. Records last touch
                    lastTouch.set(pointer.x, pointer.y);

                    if (performCornerClick) {
                        performCornerClick = isInClickRange(downRawTouch, pointerRaw);
                    }

                    // 5. Reports to listener
//                    if (roiListener != null) {// TODO : Remove me
//                        roiListener.onMoveROI(evaluateROI());
//                    }
                }   break;
                case MotionEvent.ACTION_UP: {
                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                    if (performCornerClick && cornerButton != null) {
                        if (buttonListener != null) {
                            buttonListener.onClick(cornerButton);
                        }
                    }
                    if (cornerButton != null) {
                        cornerButton.setPressed(false);
                    }
//                    // 1. Evaluates ROI// TODO : Remove me
//                    RectF oldRoi = evaluateROI();
//                    RectF fixedRoi = boundIfUnderZoomed(oldRoi);
//
//                    LogF("^ (x, y) = (%12s, %12s), same = %s, roi = %s, old = %s", e.getX(), e.getY(), oldRoi.equals(fixedRoi) ? "o" : "x", oldRoi, fixedRoi);
//
//                    // 2-1. If user has under-scaled (so outside's black strips appears)
//                    if (!oldRoi.equals(fixedRoi)) {
//                        // 2-2. Move viewer back to valid range, (viewer.x + back_X, viewer.y + back_Y)
//                        float back_X = (oldRoi.left - fixedRoi.left) * mROIUnit.getMovieViewWidth();
//                        float back_Y = (oldRoi.top - fixedRoi.top) * mROIUnit.getMovieViewHeight();
//                        viewer.offset(back_X, back_Y);
//
//                        LogF("  ^ move to (%12s, %12s), shift (%12s, %12s)", viewer.x, viewer.y, back_X, back_Y);
//                        mROIUnit.moveMovieView(Math.round(viewer.x), Math.round(viewer.y));
//                    }
//
                    // 3. Report to listener
                    if (resizeListener != null) {
                        PointF toCenter = toCenterXY(contentSize.x, contentSize.y);
                        resizeListener.onNewPosition(viewer.x + toCenter.x, viewer.y + toCenter.y, contentSize.x, contentSize.y, contentDegree);
                    }
                }   break;
                case MotionEvent.ACTION_CANCEL: {
                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                }   break;
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
                }   break;
                default: {
                    LogF("Omit e = %s", e);
                }
            }

            return true;
        }

        private boolean isInClickRange(PointF pivot, PointF pointer) {
            float left = pivot.x - MIN_CLICK_WIDTH / 2;
            float top = pivot.y - MIN_CLICK_HEIGHT / 2;

            RectF rect = new RectF(left, top, left + MIN_CLICK_WIDTH, top + MIN_CLICK_HEIGHT);
            return rect.contains(pointer.x, pointer.y);

        }

        private float normalizeDegreeTo0_360(float degree) {
            if (degree < 0) {
                return degree + 360;
            } else if (degree >= 360) {
                return degree - 360;
            } else {
                return degree;
            }
        }
    };

    /** Returns the point's degree with positive X-Axis, in [-180, 180) */
    private double evaluateDegree(PointF point) {
        return Math.toDegrees(Math.signum(point.y) * Math.acos(point.x / point.length()));
    }

    private PointF toCenterXY(float width, float height) {
        PointF point = toMarginXY(width, height);
        point.negate();
        return point;
    }

    private PointF toMarginXY(float width, float height) {
        int outerW = controller.getWidth() - contentRV.getWidth();
        int outerH = controller.getHeight() - contentRV.getHeight();
        float toX = - (width + outerW) * 0.5f;
        float toY = - (height + outerH) * 0.5f;
        return new PointF(toX, toY);
    }

    private void resizeView() {
        resizerMovingView(controller, viewer);
        resizerResizeView(controller, contentRV, contentSize); // Keep the size so we will not be shrunk when 1st time resize view
    }

    private void resizerMovingView(View view, PointF marginPoint) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof MarginLayoutParams) {
            MarginLayoutParams mlp = (MarginLayoutParams) lp;
            mlp.leftMargin = Math.round(marginPoint.x);
            mlp.topMargin = Math.round(marginPoint.y);
            view.setLayoutParams(mlp);
        }
    }

    private void resizerResizeView(View view, View inner, PointF size) {
        int outerW = view.getWidth() - inner.getWidth();
        int outerH = view.getHeight() - inner.getHeight();

        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.width = Math.round(size.x + outerW);
        lp.height = Math.round(size.y + outerH);
        view.setLayoutParams(lp);
    }

    private void setResizerContentSize(float width, float height) {
        float fixedW = Math.max(MIN_CONTENT_WIDTH, width);
        float fixedH = Math.max(MIN_CONTENT_HEIGHT, height);
        float dx = (contentSize.x - fixedW) * 0.5f;
        float dy = (contentSize.y - fixedH) * 0.5f;
        contentSize.set(fixedW, fixedH);
        viewer.offset(dx, dy);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (isNoContent()) return false;

            float f = detector.getScaleFactor();
            float w = f * contentRV.getWidth();
            float h = f * contentRV.getHeight();

            setResizerContentSize(w, h);
            resizeView();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (isNoContent()) return;

//            if (scale == SCALE_MIN) {
//                mROIUnit.moveMovieView(0, 0);
//
//                LogF("scale min");
//                // 5. Reports to listener
//                if (roiListener != null) {
//                    roiListener.onMoveROI(new RectF(0, 0, 1, 1));
//                }
//            }
            LogF("scale end = %s", "OK : " + contentRV);
        }
    }

    private static void LogF(String format, Object... args) {
        if (DEBUG_TOUCH) {
            Log.e("ResizerView", String.format(format, args));
        }
    }
}
