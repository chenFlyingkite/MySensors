package com.cyberlink.actiondirector.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.cyberlink.actiondirector.R;

import java.util.concurrent.atomic.AtomicBoolean;

public class TrimView extends RelativeLayout {

    private static final String TAG = TrimView.class.getSimpleName();
    private static final boolean DEBUG = false;

    /**
     * Minimum width of trimming operation. That means, each unit can only trim to at least 0.1 second.
     */
    public static final long MINIMUM_TRIM_RESULT_WIDTH = 100 * 1000; // 100ms

    public interface OnValueChangeListener {
        /**
         * Tell the observer that the TrimView will change the indicator position in microseconds(us).
         * @param positionUs Trim indicator on timeline position in microseconds(us).
         */
        void onValueChange(long positionUs);


        /**
         * Tell the observer that the trim behavior is started.
         */
        void onTrimBegin();

        /**
         * Tell the observer that the trim behavior is finished and where its position on timeline in microseconds(us).
         */
        void onTrimEnd(long positionUs);
    }

    public enum IndicatorSide {
        LEFT,
        RIGHT,
        NONE;

        public boolean is(IndicatorSide side) {
            return this == side;
        }

        public boolean isNot(IndicatorSide side) {
            return this != side;
        }
    }

    public static final class Boundary {
        /**
         * Left initial boundary of target view in timeline.
         */
        private final long beginUs;
        /**
         * Right initial boundary of target view in timeline.
         */
        private final long endUs;
        /**
         * Left minimum boundary of target view in timeline.
         */
        private final long minUs;
        /**
         * Right maximum boundary of target view in timeline
         */
        private final long maxUs;
        /**
         * Front TX time
         */
        private final long frontTXUs;
        /**
         * back TX time
         */
        private final long backTXUs;
        /**
         * No maximum limitation for clip which doesn't have duration, such as photo, title
         */
        private final boolean hasLimit;

        /**
         * Trim enabled to show left or right trim indicator. Disable to visible them
         * <p/>
         * This boundary view was over-deputy since version 3.2 could disable trim function on virtual cut,
         * but itemView selected border is not obvious, so we decide to use trim boundary view to be a selected border.
         *
         * FIXME: Need obviously selected border drawables for three tracks.
         */
        private final boolean trimEnabled;

        public Boundary(long beginUs, long endUs, long minUs, long maxUs, long frontTXUs, long backTXUs, boolean hasLimit, boolean trimEnabled) {
            this.beginUs = beginUs;
            this.endUs = endUs;
            this.minUs = minUs;
            this.maxUs = (hasLimit) ? maxUs : -1;
            this.frontTXUs = frontTXUs;
            this.backTXUs = backTXUs;
            this.hasLimit = hasLimit;
            this.trimEnabled = trimEnabled;
        }
    }

    /** isMoving is true if indicators are handling touch event */
    private final AtomicBoolean isMoving = new AtomicBoolean(false);
    /** canMove is true if indicator can receive touch event */
    private final AtomicBoolean canMove = new AtomicBoolean(true);

    /**
     * We only accept single finger operation, so keep one {@link IndicatorSide} for current moving.
     */
    private IndicatorSide mMoveIndicatorSide = IndicatorSide.NONE;

    /**
     * This view is used to accept touch down event.
     */
    private class IndicatorView extends ImageView {

        private final IndicatorSide side;

        public IndicatorView(Context context, IndicatorSide theSide) {
            super(context);
            side = theSide;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (false == canMove.get()) return false;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:

                    if (isMoving.get()) {
                        break;
                    }

                    // Enter trim mode only when user touch trim IndicatorView.
                    IndicatorView.this.bringToFront();
                    isMoving.set(true);
                    mMoveIndicatorSide = side;
                    break;
            }

            return false;
        }
    }

    private OnValueChangeListener mLeftOnValueChangeListener;
    private OnValueChangeListener mRightOnValueChangeListener;

    private final int mIndicatorPixelWidth;
    private final int mIndicatorPixelHeight;
    private IndicatorView mLeftIndicator;
    private IndicatorView mRightIndicator;
    // Make IndicatorView be handy to touch by extending paddingStart & paddingEnd
    private final int mEasyTouchIndicatorPadding;

    /**
     * Trim boundary explicitly shows the target mark-in/out of the trimming clip.
     */
    private final View mTrimBoundaryView;
    private final View mTrimLeftOutsideView;
    private final View mTrimRightOutsideView;
    private Scaler mScaler = null;
    private Boundary mIndicatorBoundary;
    private Drawable mDrawableBoundary;

    public TrimView(Context context) {
        this(context, null);
    }

    public TrimView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TrimView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final Resources.Theme theme = context.getTheme();
        TypedArray a = theme.obtainStyledAttributes(attrs, R.styleable.TrimView, defStyle, 0);

        mIndicatorPixelWidth = a.getDimensionPixelOffset(R.styleable.TrimView_indicatorWidth, 0);
        mIndicatorPixelHeight = a.getDimensionPixelOffset(R.styleable.TrimView_indicatorHeight, 0);
        mEasyTouchIndicatorPadding = mIndicatorPixelWidth > 0 ? mIndicatorPixelWidth / 2 : 30;

        boolean boundaryAboveThumb = a.getBoolean(R.styleable.TrimView_boundaryAboveThumb, false);

        View[] bounds;
        if (boundaryAboveThumb) {
            constructIndicators(a);
            bounds = constructBoundaryViews(a);
        } else {
            bounds = constructBoundaryViews(a);
            constructIndicators(a);
        }
        mTrimLeftOutsideView = bounds[0];
        mTrimBoundaryView = bounds[1];
        mTrimRightOutsideView = bounds[2];

        a.recycle();
    }

    /**
     * Make {@link IndicatorSide#LEFT} and {@link IndicatorSide#RIGHT} of {@link IndicatorView}
     * have chance to receive the touch event even if indicator draws outside from its parent.
     * We will call <b>parentOfIndicator.setOnTouchListener()</b> internally.<br>
     * If your listener, parentOnTouchListener, is not null, please take care this calling sequence.<br>
     * <p>
     * To ensure indicators can be outside touchable, please call as :<br>
     * <b> [parentOfIndicator].setOnTouchListener([parentOnTouchListener]) </b><br>
     * <b> [TrimView].ensureIndicatorOutsideTouchable([parentOfIndicator], [parentOnTouchListener]) </b>
     * </p>
     * <p>
     * Indicators has no chance to receive the touch event from outside if call as :<br>
     * [TrimView].ensureIndicatorOutsideTouchable([parentOfIndicator], [parentOnTouchListener])<br>
     * [parentOfIndicator].setOnTouchListener([parentOnTouchListener])
     *
     * @param parentOfIndicator the non-null parent view that Left/Right Indicator will completely reside in.
     * @param parentOnTouchListener the parentOfIndicator's onTouchListener
     * @return the listener we set to parentOfIndicator.
     * */
    public OnTouchListener ensureIndicatorOutsideTouchable(@NonNull final View parentOfIndicator, final @Nullable OnTouchListener parentOnTouchListener) {
        final int pid = parentOfIndicator.getId();
        final OnTouchListener changedTouchListener = new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) { // v = parentOfIndicator
                if (e.getAction() == MotionEvent.ACTION_CANCEL) return false; // http://crashes.to/s/244006e9879

                int x = (int)e.getRawX();
                int y = (int)e.getRawY();
                boolean touchInsideParent;
                boolean touchOnLeftIndicator;
                boolean touchOnRightIndicator;
                Rect parentRect, leftRect, rightRect;

                // This line should output, "View id = <your_id>, is parentOfIndicator = <TRUE>"
                if (DEBUG) {
                    Log.i(TAG, "View id = " + v.getId() + ", is parentOfIndicator = " + (v.getId() == pid) );
                    Log.i(TAG, "Touch event at (RawX, RawY) = (" + x + ", " + y + "), Moving = " + isMoving.get() + ", side = " + mMoveIndicatorSide);
                }

                parentRect = getRectOnScreen(v);
                leftRect = getRectOnScreen(mLeftIndicator);
                rightRect = getRectOnScreen(mRightIndicator);

                touchInsideParent = parentRect.contains(x, y);
                touchOnLeftIndicator = leftRect.contains(x, y);
                touchOnRightIndicator = rightRect.contains(x, y);
                if (DEBUG) {
                    Log.i(TAG, "Parent is " + parentRect + ", event inside = " + touchInsideParent);
                    Log.i(TAG, "L Rect is " + leftRect + ", event inside = " + touchOnLeftIndicator);
                    Log.i(TAG, "R Rect is " + rightRect + ", event inside = " + touchOnRightIndicator);
                }

                // We offset the pointer location (X, Y) by the padding of parentOfIndicator to let Trim view receive the correct event
                // This newEvent is used to pass to TrimView.onTouchEvent(MotionEvent)
                MotionEvent newEvent = MotionEvent.obtain(e);
                newEvent.offsetLocation(-v.getPaddingStart(), -v.getPaddingTop());

                if (DEBUG) {
                    Log.i(TAG, "The new event for Indicator = " + newEvent);
                    Log.i(TAG, "The old event for Indicator = " + e);
                }

                boolean wantToMoveLeftIndicator = mLeftIndicator != null && TrimView.this.getVisibility() == VISIBLE && (
                        (touchInsideParent && touchOnLeftIndicator) ||
                        (canMove.get() && isMoving.get() && mMoveIndicatorSide == IndicatorSide.LEFT));

                boolean wantToMoveRightIndicator = mRightIndicator!= null && TrimView.this.getVisibility() == VISIBLE && (
                        (touchInsideParent && touchOnRightIndicator) ||
                        (canMove.get() && isMoving.get() && mMoveIndicatorSide == IndicatorSide.RIGHT));

                if (DEBUG) Log.i(TAG, "want to move left / right = " + wantToMoveLeftIndicator + " / " + wantToMoveRightIndicator);

                if (wantToMoveLeftIndicator) {
                    if (DEBUG) Log.i(TAG, "touch on left");
                    mLeftIndicator.setSelected(true);
                    mRightIndicator.setSelected(false);
                    return mLeftIndicator.dispatchTouchEvent(newEvent) || TrimView.this.onTouchEvent(newEvent);
                } else if (wantToMoveRightIndicator) {
                    if (DEBUG) Log.i(TAG, "touch on right");
                    mRightIndicator.setSelected(true);
                    mLeftIndicator.setSelected(false);
                    return mRightIndicator.dispatchTouchEvent(newEvent) || TrimView.this.onTouchEvent(newEvent);
                } else if (touchInsideParent) {
                    if (DEBUG) Log.i(TAG, "touch on parent, not in left, right indicators");
                    deselectIndicators();
                    return dispatchTouchEvent(newEvent);
                } else {
                    if (DEBUG) Log.i(TAG, "touch outside parent");
                    deselectIndicators();
                    return dispatchTouchEvent(e);
                }
            }

            private @NonNull Rect getRectOnScreen(View v) {
                Rect r = new Rect();
                int[] xy = new int[2];

                if (v == null) return r; // since Indicator maybe null

                v.getHitRect(r);
                v.getLocationOnScreen(xy);
                return new Rect(xy[0], xy[1], xy[0] + r.width(), xy[1] + r.height());
            }
        };
        OnTouchListener parentNewOnTouchListener = new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_CANCEL) return false; // http://crashes.to/s/244006e9879

                boolean result = changedTouchListener.onTouch(v, event);
                if (result) {
                    return true;
                } else if (parentOnTouchListener != null) {
                    return parentOnTouchListener.onTouch(v, event);
                } else {
                    return false;
                }
            }
        };

        parentOfIndicator.setOnTouchListener(parentNewOnTouchListener);
        return parentNewOnTouchListener;
    }

    public void deselectIndicators() {
        mMoveIndicatorSide = IndicatorSide.NONE;
        if (mLeftIndicator != null) mLeftIndicator.setSelected(false);
        if (mRightIndicator != null) mRightIndicator.setSelected(false);
    }


    private void constructIndicators(TypedArray a) {
        Drawable drawableLeft = a.getDrawable(R.styleable.TrimView_drawableLeft);
        mLeftIndicator = constructIndicator(drawableLeft, mIndicatorPixelWidth, mIndicatorPixelHeight ,IndicatorSide.LEFT);

        Drawable drawableRight = a.getDrawable(R.styleable.TrimView_drawableRight);
        mRightIndicator = constructIndicator(drawableRight, mIndicatorPixelWidth, mIndicatorPixelHeight, IndicatorSide.RIGHT);
    }

    private IndicatorView constructIndicator(Drawable drawable, int indicatorWidth, int indicatorHeight, IndicatorSide side) {
        if (drawable == null) return null;

        IndicatorView view = new IndicatorView(getContext(), side);
        //view.setBackgroundColor(Color.argb(64, 255, 0, 0)); // Debug used, to make view more explicit
        view.setImageDrawable(drawable);
        view.setClickable(true);

        int width = (indicatorWidth > 0) ? indicatorWidth : RelativeLayout.LayoutParams.WRAP_CONTENT;
        int height = (indicatorHeight > 0) ? indicatorHeight : RelativeLayout.LayoutParams.WRAP_CONTENT;
        if (width > 0) {
            width += mEasyTouchIndicatorPadding * 2;
        }
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);

        params.addRule(CENTER_VERTICAL);
        params.addRule(ALIGN_PARENT_LEFT);
        view.setLayoutParams(params);

        // Set easy touch padding
        view.setPadding(mEasyTouchIndicatorPadding, 0, mEasyTouchIndicatorPadding, 0);

        this.addView(view);
        return view;
    }

    private View[] constructBoundaryViews(TypedArray a) {
        View[] views = new View[]{null, null, null};
        if (isInEditMode()) return views;

        // Start to left background
        Drawable drawableLeftOutside = a.getDrawable(R.styleable.TrimView_drawableLeftToLeftMost);
        if (drawableLeftOutside != null) {
            // Set background
            final ImageView leftOutside = (ImageView) LayoutInflater.from(getContext()).inflate(R.layout.material_preview_border_view, this, false);
            leftOutside.setBackground(drawableLeftOutside);

            // Align parent left
            RelativeLayout.LayoutParams leftParams = (RelativeLayout.LayoutParams) leftOutside.getLayoutParams();
            leftParams.addRule(ALIGN_PARENT_LEFT);
            leftOutside.setLayoutParams(leftParams);

            addView(leftOutside);
            views[0] = leftOutside;
        }

        // Right to end background
        Drawable drawableRightOutside = a.getDrawable(R.styleable.TrimView_drawableRightToRightMost);
        if (drawableRightOutside != null) {
            // Set background
            final ImageView rightOutside = (ImageView) LayoutInflater.from(getContext()).inflate(R.layout.material_preview_border_view, this, false);
            rightOutside.setBackground(drawableRightOutside);

            // Align parent right
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) rightOutside.getLayoutParams();
            params.addRule(ALIGN_PARENT_RIGHT);
            rightOutside.setLayoutParams(params);

            addView(rightOutside);
            views[2] = rightOutside;
        }

        // Trim boundary
        final ImageView trimBound = (ImageView) LayoutInflater.from(getContext()).inflate(R.layout.material_preview_border_view, this, false);
        Drawable drawableBoundary = a.getDrawable(R.styleable.TrimView_drawableBoundary);
        if (drawableBoundary != null) {
            trimBound.setBackground(drawableBoundary);
        }
        mDrawableBoundary = drawableBoundary;
        addView(trimBound);
        views[1] = trimBound;
        return views;
    }

    public void setLeftIndicator(int drawableId) {
        checkIndicatorIsPresent(mLeftIndicator);
        mLeftIndicator = constructIndicator(ResourcesCompat.getDrawable(getResources(), drawableId, null), mIndicatorPixelWidth, mIndicatorPixelHeight, IndicatorSide.LEFT);
    }

    public void setRightIndicator(int drawableId) {
        checkIndicatorIsPresent(mRightIndicator);
        mRightIndicator = constructIndicator(ResourcesCompat.getDrawable(getResources(), drawableId, null), mIndicatorPixelWidth, mIndicatorPixelHeight, IndicatorSide.RIGHT);
    }

    public void setScaler(Scaler scaler) {
        mScaler = scaler;
    }

    private void checkIndicatorIsPresent(IndicatorView indicator) {
        if (indicator != null) {
            ViewGroup parent = (ViewGroup) indicator.getParent();
            if (parent != null) {
                parent.removeView(indicator);
            }
        }
    }

    private int getIndicatorPadding(IndicatorSide side) {
        int result = 0;
        switch (side) {
            case LEFT:
                if (mLeftIndicator != null) {
                    result = mIndicatorPixelWidth / 2 + mEasyTouchIndicatorPadding;
                }
                break;
            case RIGHT:
                if (mRightIndicator != null) {
                    result = mIndicatorPixelWidth / 2 + mEasyTouchIndicatorPadding;
                }
                break;
        }
        return result;
    }

    public void setLeftOnValueChangeListener(OnValueChangeListener l) {
        mLeftOnValueChangeListener = l;
    }

    public void setRightOnValueChangeListener(OnValueChangeListener l) {
        mRightOnValueChangeListener = l;
    }

    /**
     * Convert pointer axis-x to timeline position in microseconds.
     *
     * @param side Which indicator the user press, left or right.
     * @param pointerX  The x position of the trim handle
     * @return Timeline position in microseconds(us).
     */
    public long convertMovingPointerXToPositionUs(IndicatorSide side, float pointerX) {
        long pointerPositionUs = Math.round(pointerX * mScaler.getUsPerPixel());

        if (IndicatorSide.LEFT == side) { // Press LEFT trim indicator
            if (mIndicatorBoundary.hasLimit && pointerPositionUs < mIndicatorBoundary.minUs) {
                pointerPositionUs = mIndicatorBoundary.minUs;
            } else {
                double rightBoundaryX = (mRightIndicator.getX() + getIndicatorPadding(IndicatorSide.RIGHT)) - MINIMUM_TRIM_RESULT_WIDTH * mScaler.getPixelsPerUs();
                if (pointerX > rightBoundaryX) {
                    pointerPositionUs = Math.round(rightBoundaryX * mScaler.getUsPerPixel());
                }
                long endBoundaryUs = mIndicatorBoundary.endUs - MINIMUM_TRIM_RESULT_WIDTH;
                if (mIndicatorBoundary.backTXUs != 0 || mIndicatorBoundary.frontTXUs != 0) {
                    endBoundaryUs = mIndicatorBoundary.endUs - mIndicatorBoundary.backTXUs - mIndicatorBoundary.frontTXUs;
                }
                if (pointerPositionUs > endBoundaryUs) {
                    pointerPositionUs = endBoundaryUs;
                }
            }
        } else { // Press RIGHT trim indicator
            if (mIndicatorBoundary.hasLimit && pointerPositionUs > mIndicatorBoundary.maxUs) {
                pointerPositionUs = mIndicatorBoundary.maxUs;
            } else {
                double leftBoundaryX = (mLeftIndicator.getX() + getIndicatorPadding(IndicatorSide.LEFT)) + MINIMUM_TRIM_RESULT_WIDTH * mScaler.getPixelsPerUs();
                if (pointerX < leftBoundaryX) {
                    pointerPositionUs = Math.round(leftBoundaryX * mScaler.getUsPerPixel());
                }
                long beginBoundaryUs = mIndicatorBoundary.beginUs + MINIMUM_TRIM_RESULT_WIDTH;
                if (mIndicatorBoundary.frontTXUs != 0 || mIndicatorBoundary.backTXUs != 0) {
                    beginBoundaryUs = mIndicatorBoundary.beginUs + mIndicatorBoundary.frontTXUs + mIndicatorBoundary.backTXUs;
                }
                if (pointerPositionUs < beginBoundaryUs) {
                    pointerPositionUs = beginBoundaryUs;
                }
            }
        }

        return pointerPositionUs;
    }

    private void notifyTriming(IndicatorSide side, long positionUs) {
        if (IndicatorSide.LEFT == side) {
            if (mLeftOnValueChangeListener != null) mLeftOnValueChangeListener.onValueChange(positionUs);
        } else if (IndicatorSide.RIGHT == side) {
            if (mRightOnValueChangeListener != null) mRightOnValueChangeListener.onValueChange(positionUs);
        }
    }

    private void notifyTrimBegin(IndicatorSide side) {
        if (IndicatorSide.LEFT == side) {
            if (mLeftOnValueChangeListener != null) mLeftOnValueChangeListener.onTrimBegin();
        } else if (IndicatorSide.RIGHT == side) {
            if (mRightOnValueChangeListener != null) mRightOnValueChangeListener.onTrimBegin();
        }
    }

    private void notifyTrimEnd(IndicatorSide side, long positionUs) {
        if (IndicatorSide.LEFT == side) {
            if (mLeftOnValueChangeListener != null) mLeftOnValueChangeListener.onTrimEnd(positionUs);
        } else if (IndicatorSide.RIGHT == side) {
            if (mRightOnValueChangeListener != null) mRightOnValueChangeListener.onTrimEnd(positionUs);
        }
    }

    /**
     * Set the referrer that we want to adjust its left or right boundary.
     * @param trimBoundary the boundary
     */
    public void setReferrer(Boundary trimBoundary) {
        if (trimBoundary == null) {
            if (DEBUG) Log.v(TAG, "Unable to know trim boundary.");
            return;
        }

        mIndicatorBoundary = trimBoundary;
        double pixelsPerUs = mScaler.getPixelsPerUs();
        float left = (float) pixelsPerUs * mIndicatorBoundary.beginUs;
        float right = (float) pixelsPerUs * mIndicatorBoundary.endUs;
        if (mLeftIndicator != null) {
            mLeftIndicator.setX(left - getIndicatorPadding(IndicatorSide.LEFT));
            mLeftIndicator.setVisibility(mIndicatorBoundary.trimEnabled ? View.VISIBLE : View.GONE);
        }
        if (mRightIndicator != null) {
            mRightIndicator.setX(right - getIndicatorPadding(IndicatorSide.RIGHT));
            mRightIndicator.setVisibility(mIndicatorBoundary.trimEnabled ? View.VISIBLE : View.GONE);
        }

        // Update Left outside part
        if (mTrimLeftOutsideView != null) {
            ViewGroup.LayoutParams params = mTrimLeftOutsideView.getLayoutParams();
            params.width = Math.round(left);
            mTrimLeftOutsideView.setLayoutParams(params);
        }

        // Update Right outside part
        if (mTrimRightOutsideView != null) {
            ViewGroup.LayoutParams params = mTrimRightOutsideView.getLayoutParams();
            params.width = Math.round(this.getWidth() - right);
            mTrimRightOutsideView.setLayoutParams(params);
        }

        // Update Trim range part
        ViewGroup.LayoutParams params = mTrimBoundaryView.getLayoutParams();
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        params.width = Math.round(right - left);
        mTrimBoundaryView.setX(left);
        mTrimBoundaryView.setLayoutParams(params);
    }


    /**
     * Unset and use the drawable background provided in xml
     * @see #setTrimBoundaryViewBackground(Drawable)
     */
    public void unsetTrimBoundaryViewBackground() {
        mTrimBoundaryView.setBackground(mDrawableBoundary);
    }

    /**
     * Set the Trim Boundary's background. To unset the properly, use {@link #unsetTrimBoundaryViewBackground()}
     */
    public void setTrimBoundaryViewBackground(Drawable background) {
        mTrimBoundaryView.setBackground(background);
    }

    public void setIndicatorVisible(boolean visible) {
        mLeftIndicator.setVisibility(visible ? VISIBLE : GONE);
        mRightIndicator.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setIndicatorMovable(boolean movable) {
        canMove.set(movable);
        if (!movable) isMoving.set(false);
    }

    public void moveIndicator(IndicatorSide side, long acceptPositionUs) {
        float positionPx = (float) (acceptPositionUs * mScaler.getPixelsPerUs());

        if (IndicatorSide.LEFT == side) {
            mLeftIndicator.setX(positionPx - getIndicatorPadding(side));
            mTrimBoundaryView.setX(positionPx);
        } else {
            mRightIndicator.setX(positionPx - getIndicatorPadding(side));
        }

        int leftAt = Math.round(mLeftIndicator.getX() + getIndicatorPadding(side));
        int rightAt = Math.round(mRightIndicator.getX() + getIndicatorPadding(side));
        // Update Left outside part
        if (mTrimLeftOutsideView != null) {
            ViewGroup.LayoutParams params = mTrimLeftOutsideView.getLayoutParams();
            params.width = leftAt;
            mTrimLeftOutsideView.setLayoutParams(params);
        }

        // Update Right outside part
        if (mTrimRightOutsideView != null) {
            ViewGroup.LayoutParams params = mTrimRightOutsideView.getLayoutParams();
            params.width = this.getWidth() - rightAt;
            mTrimRightOutsideView.setLayoutParams(params);
        }

        // Update Trim range
        ViewGroup.LayoutParams params = mTrimBoundaryView.getLayoutParams();
        params.width = Math.round(mRightIndicator.getX() - mLeftIndicator.getX());
        mTrimBoundaryView.setLayoutParams(params);
    }

    private long onTrimBoundary(float pointerX) {
        long acceptPositionUs = convertMovingPointerXToPositionUs(mMoveIndicatorSide, pointerX);
        if (!isMoving.get()) {
            return acceptPositionUs;
        }

        moveIndicator(mMoveIndicatorSide, acceptPositionUs);
        notifyTriming(mMoveIndicatorSide, acceptPositionUs);

        return acceptPositionUs;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Omit event if
        // 1. Did not set the referrer
        // 2. Cannot move indicators or not moving
        if (mIndicatorBoundary == null || !canMove.get() || !isMoving.get()) {
            return false;
        }

        final float pointerX = event.getX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                onTrimBoundary(pointerX);
                break;

            case MotionEvent.ACTION_DOWN:
                // The ScrollView will not get the touch events
                getParent().requestDisallowInterceptTouchEvent(true);
                notifyTrimBegin(mMoveIndicatorSide);
                if (mRightIndicator != null) mRightIndicator.setSelected(mMoveIndicatorSide == IndicatorSide.RIGHT);
                if (mLeftIndicator != null) mLeftIndicator.setSelected(mMoveIndicatorSide == IndicatorSide.LEFT);
                break;

            case MotionEvent.ACTION_CANCEL:
                // https://www.fabric.io/cyberlink/android/apps/com.cyberlink.powerdirector.dra140225_01/issues/565a93e9f5d3a7f76bf2f783
                // JavaDoc: You should treat this as an up event, but not perform any action that you normally would.
                isMoving.set(false);
                break; // We have beginTrim, but endTrim isn't suitable here. Just do nothing.
            case MotionEvent.ACTION_UP:
                isMoving.set(false);
                long positionUs = onTrimBoundary(pointerX);
                getParent().requestDisallowInterceptTouchEvent(false);
                notifyTrimEnd(mMoveIndicatorSide, positionUs);
                break;
        }

        return true;
    }

    public IndicatorSide getSelectedIndicatorSide() {
        if (mRightIndicator.isSelected()) {
            return IndicatorSide.RIGHT;
        } else if (mLeftIndicator.isSelected()){
            return IndicatorSide.LEFT;
        } else {
            return IndicatorSide.NONE;
        }
    }
}