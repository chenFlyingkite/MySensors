package com.cyberlink.actiondirector.page.editor.moviekit;

import android.view.MotionEvent;
import android.view.View;

import com.cyberlink.actiondirector.page.editor.PlaybackProgressListener;
import com.cyberlink.actiondirector.util.UIUtils;
import com.cyberlink.util.MathUtils;

public class MovieSeekByTouchKit implements View.OnTouchListener {
    private SeekByTouchInfo mInfo;

    public interface SeekByTouchInfo {
        SeekUnit getSeekUnit();
        MovieTipTextKit getRespondKit();
        PlaybackProgressListener getPlaybackListener();
        long getMovieBeginUs();
        long getMovieEndUs();
        long getPosition();
    }

    public MovieSeekByTouchKit(SeekByTouchInfo info) {
        mInfo = info;
    }

    /**
     * The threshold of the offset begin to detect gestures.
     */
    private final float START_PRECISE_THRESHOLD = 40;

    /**
     * The threshold of seek time not too close
     */
    private final float PRECISE_SEEK_TIME_THRESHOLD = 50;

    /**
     * For better UX, the padding area should not be count.
     */
    private final float TOP_BOTTOM_PADDING = 0;
    private final float LEFT_RIGHT_PADDING = 60;

    /**
     * The scroll speed. 2400000us per screen width.
     */
    private final float N_TOTAL_US_PER_SCREEN_WITH = 2400000.0f / UIUtils.getScreenWidth();

    private float MAX_VIEW_WIDTH;
    private float MAX_VIEW_HEIGHT;
    private float startX;
    private float startY;
    private float prevX;
    private float prevY;
    private boolean isSeeking;
    private long lastSeekPos = -1;
    private long startSeekPos = -1;
    private long seekOffsetUs = 0;
    private long lastSeekTime = 0;

    private boolean performClick = false;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean isHandled = false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                performClick = true;
                MAX_VIEW_WIDTH = v.getWidth();
                MAX_VIEW_HEIGHT = v.getHeight();

                startX = event.getX();
                startY = event.getY();
                prevX = startX;
                prevY = startY;

                lastSeekPos = mInfo.getPosition();
                lastSeekTime = 0;
                startSeekPos = mInfo.getPosition();
                mInfo.getRespondKit().clearText();
            }   break;
            case MotionEvent.ACTION_MOVE: {
                float curX = event.getX();
                float curY = event.getY();

                if (isInWorkingArea(curX, curY)) {
                    if (Math.abs(curX - startX) > START_PRECISE_THRESHOLD || isSeeking) {
                        if (!isSeeking) {
                            isSeeking = true;
                            startX = curX;
                            startY = curY;
                            prevX = startX;
                            prevY = startY;
                            mInfo.getSeekUnit().startUserFastSeeking(true);
                        }
                        performClick = false;

                        if (System.currentTimeMillis() - lastSeekTime > PRECISE_SEEK_TIME_THRESHOLD) {
                            handleScroll(curX - prevX);
                            prevX = curX;
                            prevY = curY;
                            lastSeekTime = System.currentTimeMillis();
                        }
                    }
                }
            }   break;
            case MotionEvent.ACTION_UP: {
                if (performClick) {
                    isHandled = true;
                    v.performClick();
                } else if (isSeeking) {
                    isHandled = true;
                    mInfo.getSeekUnit().stopUserFastSeeking(lastSeekPos);
                }

                resetState();
            }    break;
            case MotionEvent.ACTION_CANCEL: {
                resetState();
            }   break;
        }
        return isHandled;
    }

    private void resetState() {
        lastSeekPos = -1;
        seekOffsetUs = 0;
        isSeeking = false;
        performClick = false;

        mInfo.getRespondKit().resetState();
    }

    private void handleScroll(float offset) {
        final long offsetSeekTo = (long) (offset * N_TOTAL_US_PER_SCREEN_WITH);
        // Bound playback position in valid range of movie, (0, duration) or (minUs, maxUs)
        final long seekTo = boundValueInRange(mInfo.getMovieBeginUs()
                , lastSeekPos + offsetSeekTo
                , mInfo.getMovieEndUs());

        // Bound seekOffsetUs in valid range
        seekOffsetUs = boundValueInRange(mInfo.getMovieBeginUs() - startSeekPos
                , seekOffsetUs + offsetSeekTo
                , mInfo.getMovieEndUs() - startSeekPos);

        mInfo.getRespondKit().showTipsViewValue(toTipString(seekOffsetUs));

        if (mInfo.getPlaybackListener() != null) {
            mInfo.getPlaybackListener().onTouchPreciseSeek(seekTo, mInfo.getMovieEndUs());
        }

        mInfo.getSeekUnit().doUserFastSeeking(seekTo, true);

        lastSeekPos = seekTo;
    }

    private String toTipString(long seekOffsetUs) {
        // E.g. if seekOffsetUs = 123456789 us
        // seekSeconds = 123.456789 sec
        // floored = 123.4 sec
        double seekSeconds = 1.0 * seekOffsetUs / 1000000;
        double seekSecondsFloor = Math.floor((seekSeconds * 10)) / 10.0;
        return (seekSecondsFloor >= 0 ? "+ " : "- ")
                + Math.abs(seekSecondsFloor);
    }

    /**
     * Returns the bounded value to be within min &le; value &le; max
     * <p>If MovieRangeKit disabled => <b>Bound within (0, getDuration())</b></p>
     * <p>If MovieRangeKit enabled  => <b>Bound within (minUs, maxUs)</b> of {@link com.cyberlink.actiondirector.page.editor.MovieController#mSeekRange}</p>
     *
     * @param min the left bound of value
     * @param value the value to be bound
     * @param max the right bound of value
     * @return bounded value ensures <b>min &le; value &le; max</b>
     */
    private long boundValueInRange(long min, long value, long max) {
        return MathUtils.fallInRange(value, min, max);
    }

    private boolean isInWorkingArea(float x, float y) {
        return (LEFT_RIGHT_PADDING < x && x < MAX_VIEW_WIDTH - LEFT_RIGHT_PADDING) &&
               (TOP_BOTTOM_PADDING < y && y < MAX_VIEW_HEIGHT - TOP_BOTTOM_PADDING);
    }
}
