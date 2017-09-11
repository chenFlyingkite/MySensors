package com.cyberlink.actiondirector.page.preview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.cyberlink.actiondirector.BuildConfig;
import com.cyberlink.actiondirector.R;
import com.cyberlink.actiondirector.libraries.MediaItem;
import com.cyberlink.actiondirector.movie.MovieEdit;
import com.cyberlink.actiondirector.movie.TimelineUnit;
import com.cyberlink.actiondirector.movie.TimelineVideoClip;
import com.cyberlink.actiondirector.page.BaseActivity;
import com.cyberlink.actiondirector.page.editor.MovieController;
import com.cyberlink.actiondirector.page.editor.PlaybackSeekbarAdapter;
import com.cyberlink.actiondirector.page.editor.moviekit.MoviePlayKit;
import com.cyberlink.actiondirector.page.editor.moviekit.MovieSeekBySeekBarKit;
import com.cyberlink.actiondirector.util.UIUtils;
import com.cyberlink.util.StringUtils;

public class PreviewFragment extends Fragment implements
        FullScreenPreviewListener {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final boolean DEBUG_ANIMATION = false;
    private static final boolean DEBUG_PREVIEW_STATUS = false;
    private static final String TAG = "PreviewFragment";

    public static final String BUNDLE_SOURCE_OF_MEDIA_ITEM = "Preview_MediaItem";
    public static final String BUNDLE_HIDE_CONTROLS_WHEN_IDLE = "Hide_Controls_When_Idle";
    public static final String BUNDLE_SWIPE_TO_SEEK_WHEN_IN_FULL_SCREEN = "Swipe_To_Seek_When_In_Full_Screen";
    public static final String BUNDLE_ANCHOR_HEIGHT = "Anchor_Height";
    public static final String BUNDLE_FULLSCREEN_PLAYER = "Full_Screen_Player";

    private int ANCHOR_HEIGHT;
    // Preview requester's source and listeners
    private OnPreviewSizeChange mOnSizeChange;
    private PreviewByMediaItem mSourceOfMediaItem;
    private PreviewByMovieEdit mSourceOfMovieEdit;
    private MovieEdit mMovieEditToPreview = new MovieEdit();
    private MediaItem mPreviewSource;

    // Major components and flags
    private MovieController mMovieController;
    private boolean mIsLandscape = false;
    private boolean mCalledFinish = false;
    private boolean mSeekableWhenSwipe = false;

    // Playback controls
    private View mLeavePreview;
    private View mShellView;
    private View mMovieView;
    private View mControlBar;
    private ViewSwitcher mControlPlayPause;
    private SeekBar mControlMovieSeekBar;
    private TextView mControlMoviePosition;
    private TextView mControlMovieDuration;
    private MoviePlayKit mPlayPauseKit;
    private boolean mHideWhenIdle;
    private PreviewControlsDisplayer mDisplayer;

    private final @IdRes int[] mMovieEditControls = {R.id.screenTitleBack
            , R.id.screenTitleSimpleControl
            , R.id.screenTitleSimplePlayPause
            , R.id.screenTitleSimplePosition
            , R.id.screenTitleSimpleDuration
            , R.id.screenTitleSimpleSeekbar
    };
    private final @IdRes int[] mMediaItemControls = {R.id.screenBack
            , R.id.screenSimpleControl
            , R.id.screenSimplePlayPause
            , R.id.screenSimpleCurrentPosition
            , R.id.screenSimpleSeekbar
    };

    // Playback listeners
    private MovieSeekBySeekBarKit mOnMovieSeekBarChanged;
    private PlaybackSeekbarAdapter mOnPlaybackProgressListener;

    private long mMovieDurationUs;
    private CornerAnimation mMoveToCorner;

    // FLAGS
    private boolean animateCornerBack = true;
    private boolean animateVanish = true;

    private PreviewState mPreviewState = PreviewState.VANISH;

    public enum PreviewState {
        /** Movie is displayed in Full-Screen mode */
        FULL_SCREEN,
        /** Movie is running animation from Corner to Full-Screen */
        BACKING_TO_FULL_SCREEN,
        /** Movie is running animation from Full-Screen to Corner */
        CORNERING,
        /** Movie is displayed at Corner mode */
        CORNER,
        /** Movie is running animation from Corner to Vanish */
        VANISHING,
        /** Movie is vanished and no longer visible on screen */
        VANISH,

        /** As a movie player with fullscreen mode */
        FULL_SCREEN_PLAYER
    }

    public PreviewState getPreviewState() {
        return mPreviewState;
    }

    private boolean isPlayerMode() {
        return mPreviewState == PreviewState.FULL_SCREEN_PLAYER;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public final void onAttach(Context context) {
        super.onAttach(context);
        if (DEBUG) LogWithFragmentState(" > onAttach(Context)");
        // Do nothing here, since we use onAttach(Activity) because both of these methods will be invoked.
        //      Choose one of them is enough.
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        if (DEBUG) LogWithFragmentState(" > onAttach(Activity)");
        super.onAttach(activity);

        // Preview source
        if (activity instanceof PreviewByMediaItem) {
            mSourceOfMediaItem = (PreviewByMediaItem) activity;
        } else if (activity instanceof PreviewByMovieEdit) {
            mSourceOfMovieEdit = (PreviewByMovieEdit) activity;
        } else {}

        if (activity instanceof OnPreviewSizeChange) {
            mOnSizeChange = (OnPreviewSizeChange) activity;
        }
    }

    private boolean hasSource() {
        return useMediaItem() || useMovieEdit();
    }

    private boolean useMediaItem() {
        return mSourceOfMediaItem != null;
    }

    private boolean useMovieEdit() {
        return mSourceOfMovieEdit != null;
    }

    private boolean hasValidMovieEdit() {
        return useMovieEdit() && mSourceOfMovieEdit.getMovieEditForPreview() != null;
    }

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        if (DEBUG) LogWithFragmentState(" > onCreate");
        super.onCreate(savedInstanceState);
    }

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (DEBUG) LogWithFragmentState(" > onCreateView");
        return inflater.inflate(R.layout.fragment_preview, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        if (DEBUG) LogWithFragmentState(" > onViewCreated");
        super.onViewCreated(view, savedInstanceState);

        String msg = parseArgument();

        if (msg != null) {
            Log.w(TAG, msg);
            finish();
            return;
        }

        // Initialize the views and components
        setupViews();
        mDisplayer = new PreviewControlsDisplayer(mLeavePreview, mControlBar, mHideWhenIdle);
        mMovieController = new MovieController((BaseActivity) getActivity()
                , !mIsLandscape
                , R.id.previewContainer
                , R.id.previewMovieView
                , R.id.previewMovieSeekingValueView, -1, -1);
        setupMovieKits();

        // Initialize status, like movie's duration & MovieEdit
        updatePreviewSource(mPreviewSource);

        enterFullScreenPreview();
        setupFullScreenListeners();
    }

    /**
     * @return null if parse successfully, non-null string with message if parse failed.
     */
    private String parseArgument() {
        Bundle arg = getArguments();
        String msg = null;

        if (hasSource()) {
            if (useMediaItem()) {
                if (arg != null) {
                    ANCHOR_HEIGHT = arg.getInt(BUNDLE_ANCHOR_HEIGHT);
                    mPreviewSource = arg.getParcelable(BUNDLE_SOURCE_OF_MEDIA_ITEM);
                }
                boolean existsMediaItem = mPreviewSource != null;

                if (existsMediaItem) {
                    mIsLandscape = !mPreviewSource.isPortrait();
                    mSeekableWhenSwipe = arg.getBoolean(BUNDLE_SWIPE_TO_SEEK_WHEN_IN_FULL_SCREEN, true);
                    mHideWhenIdle = arg.getBoolean(BUNDLE_HIDE_CONTROLS_WHEN_IDLE, true);
                } else {
                    msg = "Preview invalid Media Item, fragment will be detached.";
                }
            } else if (useMovieEdit()) {
                if (hasValidMovieEdit()) {
                    mIsLandscape = mSourceOfMovieEdit.getMovieEditForPreview().isLandscape();
                    mSeekableWhenSwipe = false;
                    if (arg != null && arg.containsKey(BUNDLE_FULLSCREEN_PLAYER)) {
                        if (arg.getBoolean(BUNDLE_FULLSCREEN_PLAYER)) {
                            mPreviewState = PreviewState.FULL_SCREEN_PLAYER;
                        }
                        mHideWhenIdle = true;
                    } else {
                        mHideWhenIdle = false;
                    }
                } else {
                    msg = "Preview invalid Movie Edit, fragment will be detached.";
                }
            } else {
                msg = "Preview with unknown source type, fragment will be detached.";
            }
        } else {
            msg = (arg == null ? "Null" : "Non null")
                + " of Arguments & no source, fragment will be detached.";
        }
        return msg;
    }

    private void setupViews() {
        mShellView = findViewById(R.id.previewContainer);
        mMovieView = findViewById(R.id.previewMovieView);
        if (useMovieEdit() && !isPlayerMode()) {
            mControlBar = findViewById(R.id.screenTitleSimpleControl);
            mLeavePreview = findViewById(R.id.screenTitleBack);
            mControlPlayPause = (ViewSwitcher) findViewById(R.id.screenTitleSimplePlayPause);
            mControlMoviePosition = (TextView) findViewById(R.id.screenTitleSimplePosition);
            mControlMovieDuration = (TextView) findViewById(R.id.screenTitleSimpleDuration);
            mControlMovieSeekBar = (SeekBar) findViewById(R.id.screenTitleSimpleSeekbar);
            setControlsVisible(mMovieEditControls, true);
            setControlsVisible(mMediaItemControls, false);
        } else {
            mControlBar = findViewById(R.id.screenSimpleControl);
            mLeavePreview = findViewById(R.id.screenBack);
            mControlPlayPause = (ViewSwitcher) findViewById(R.id.screenSimplePlayPause);
            mControlMoviePosition = (TextView) findViewById(R.id.screenSimpleCurrentPosition);
            mControlMovieSeekBar = (SeekBar) findViewById(R.id.screenSimpleSeekbar);
            setControlsVisible(mMovieEditControls, false);
            setControlsVisible(mMediaItemControls, true);
        }
    }

    private void setupMovieKits() {
        mPlayPauseKit = new MoviePlayKit(mControlPlayPause, mMovieController.getPlayUnit(), 0, 1);
        mMovieController.setMoviePlayKit(mPlayPauseKit);
    }

    private void setupFullScreenListeners() {
        setPreviewControlsVisible(true);
        notifySizeChange(SizeType.MATCH_SCREEN);

        setupTouchListeners(false, null);

        mDisplayer.show().requestHideWhenIdle();
    }

    /** Prepare touch/click listeners for views
     *  1. show/hide control bar by mDisplayer
     *  2. Swipe to seek's touch listener
     *
     *  @param isCornerMode true to nullify all listeners, cornerTouchListener is ignored
     *                      false to setup basic listeners.
     *  @param cornerTouchListener use listener when preview is at the corner
     * */
    private void setupTouchListeners(boolean isCornerMode, View.OnTouchListener cornerTouchListener) {
        // Prepare touch/click listeners for views
        // 1. show/hide control bar by mDisplayer
        // 2. Swipe to seek's touch listener
        if (mSeekableWhenSwipe) {
            if (isCornerMode) {
                mMovieView.setOnTouchListener(cornerTouchListener);
            } else {
                mMovieController.setSwipeToSeekMovie(mMovieView);
            }
            mMovieView.setOnClickListener(isCornerMode ? null : mDisplayer.mOnMovieViewClickListener);
        } else {
            mControlBar.setOnTouchListener(isCornerMode ? null : mDisplayer.mOnMovieControllerTouchListener);
        }
        mShellView.setOnTouchListener(isCornerMode ? null : mDisplayer.mOnMovieViewTouchListener);
    }

    private void prepareMovieEdit(MediaItem item) {
        if (useMediaItem()) {
            if (item != null) {
                mMovieDurationUs = item.getDuration();
                mPreviewSource = item;
                setupMovieEdit(mPreviewSource);
            }
        } else if (useMovieEdit()) {
            mMovieEditToPreview = mSourceOfMovieEdit.getMovieEditForPreview();
            mMovieDurationUs = mMovieEditToPreview.getMovieDuration();
        } else {
            Log.w(TAG, "No source to prepare movie, nothing is done.");
        }
    }

    public void updatePreviewSource(MediaItem item) {
        prepareMovieEdit(item);

        mMovieController.resetMovie(mMovieEditToPreview, 0, MovieEdit.CompileOptions.ALL);

        // Setup UI display
        mControlMovieSeekBar.setMax((int) (mMovieDurationUs / 1000));
        mControlMovieSeekBar.setProgress(0);
        updateDisplayTexts(0);

        // Set playback listeners according to the media item
        setupPreviewControlListeners();

        // Play the movie
        mPlayPauseKit.playView().performClick();
    }

    private void updateDisplayTexts(long position) {
        if (useMovieEdit() && !isPlayerMode()) {
            mControlMoviePosition.setText(MMSSF(position));
            mControlMovieDuration.setText(MMSSF(mMovieDurationUs));
        } else {
            mControlMoviePosition.setText(MMSSF(position) + "/" + MMSSF(mMovieDurationUs));
        }
    }

    private void setupMovieEdit(@NonNull MediaItem mediaItem) {
        mMovieEditToPreview.removeClips(MovieEdit.TRACK_INDEX_MASTER);

        long duration = mediaItem.getDuration();
        // Create TimelineClip and insert into the 1st track.
        TimelineVideoClip clip = new TimelineVideoClip(mediaItem.getFilePath(), null);
        clip.setInTimeUs(0);
        clip.setOutTimeUs(duration);
        clip.setOriginalDurationUs(duration);
        clip.setMimeType(mediaItem.getMimeType());
        clip.setWidth(mediaItem.getWidth());
        clip.setHeight(mediaItem.getHeight());
        clip.setOrientation(mediaItem.getOrientation());

        TimelineUnit unit = new TimelineUnit();
        unit.setTimelineClip(clip);
        unit.setBeginUs(0);
        unit.setEndUs(duration);

        mMovieEditToPreview.addClip(MovieEdit.TRACK_INDEX_MASTER, 0, unit);
    }

    private void setupPreviewControlListeners() {
        mLeavePreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSourceOfMediaItem != null) {
                    mSourceOfMediaItem.onBackPreview();
                } else if (mSourceOfMovieEdit != null) {
                    mSourceOfMovieEdit.onBackPreview();
                } else {}
            }
        });

        initPlaybackListeners();
    }

    private void initPlaybackListeners() {
        mOnMovieSeekBarChanged = new MovieSeekBySeekBarKit(mMovieController.getSeekUnit(), mMovieDurationUs) {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                super.onProgressChanged(seekBar, progress, fromUser);

                updateDisplayTexts(kitSeekUs);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                super.onStartTrackingTouch(seekBar);
                mDisplayer.show();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                super.onStopTrackingTouch(seekBar);
                mDisplayer.show().requestHideWhenIdle();
            }
        };
        mControlMovieSeekBar.setOnSeekBarChangeListener(mOnMovieSeekBarChanged);

        // Report playback progress to seek bar
        mOnPlaybackProgressListener = new PlaybackSeekbarAdapter(mControlMovieSeekBar) {
            @Override
            public void onComplete() {
                // When playback is finished, playback it again
                mPlayPauseKit.playView().callOnClick();
            }
        };
        mMovieController.setPlaybackProgressListener(mOnPlaybackProgressListener);
    }

    @Override
    public void onStart() {
        if (DEBUG) LogWithFragmentState(" > onStart");
        super.onStart();

        if (mCalledFinish) return;

        mMovieController.resumeResource();
    }

    @Override
    public void onResume() {
        if (DEBUG) LogWithFragmentState(" > onResume");
        super.onResume();

        if (mCalledFinish) return;


        mMovieController.resume();
        if (mPreviewState == PreviewState.FULL_SCREEN || isPlayerMode()) {
            mDisplayer.show().requestHideWhenIdle();
        }
    }

    @Override
    public void onPause() {
        if (DEBUG) LogWithFragmentState(" > onPause");
        super.onPause();

        if (mCalledFinish) return;

        mMovieController.suspend();
    }

    @Override
    public void onStop() {
        if (DEBUG) LogWithFragmentState(" > onStop");
        super.onStop();

        if (mCalledFinish) return;

        mMovieController.suspendResource();
    }

    @Override
    public void onDestroyView() {
        if (DEBUG) LogWithFragmentState(" > onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (DEBUG) LogWithFragmentState(" > onDestroy");
        super.onDestroy();

        if (mCalledFinish) return;

        mMovieController.release();
    }

    @Override
    public void onDetach() {
        if (DEBUG) LogWithFragmentState(" > onDetach");
        super.onDetach();

        mCalledFinish = false;
        mOnSizeChange = null;
        mSourceOfMediaItem = null;
        mSourceOfMovieEdit = null;

    }

    @Override
    public final Context getContext() {
        // getContext() was added at Android 23. But AcD have to support at least 18+.
        return getActivity().getApplicationContext();
    }

    private View findViewById(@IdRes int id) {
        View v = getView();
        View result;
        if (v != null) {
            result = v.findViewById(id);
            if (result != null) {
                return result; // since we found it in fragment's root view
            }
        }
        return getActivity().findViewById(id);
    }

    private void finish() {
        mCalledFinish = true;
        getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
    }

    private String MMSSF(long us) {
        return StringUtils.toTimeStringMMSSF(us / 1000);
    }

    private void setControlsVisible(@IdRes int[] ids, boolean visible) {
        for (int id : ids) {
            View v = findViewById(id);
            v.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private String getFragmentState() {
        return String.format("\n   "
                + "[Added, InLayout] [Resumed, Visible] [Hidden, Removing, Detached] = "
                + "[%s, %s] [%s, %s] [%s, %s, %s]"
                , isAdded() ? "o" : "x"
                , isInLayout() ? "o" : "x"
                , isResumed() ? "o" : "x"
                , isVisible() ? "o" : "x"
                , isHidden() ? "o" : "x"
                , isRemoving() ? "o" : "x"
                , isDetached() ? "o" : "x");
    }

    private void LogWithFragmentState(String s) {
        Log.v(TAG, s + getFragmentState());
    }

    /** Returns the view's rectangle on the screen, regardless of rotation 90
     * */
    private @NonNull Rect getRectOnScreen(View v) {
        if (v == null) return new Rect();

        Rect r = new Rect();
        int[] xy = new int[2];

        v.getHitRect(r);
        v.getLocationOnScreen(xy);

        int rotation = (int) v.getRotation();

        if (rotation == 90) {
            // Correct left position of xy :
            //   Since r.width() & r.height() is in normal Coordinate system
            //     => original = (0, 0)
            //     => x-axis = horizontal, y-axis = vertical
            //   but xy is still in rotated Coordinate system
            //     => original = (Screen Width, 0)
            //     => x-axis = vertical, y-axis = horizontal
            xy[0] -= r.width();
        }
        return new Rect(xy[0], xy[1], xy[0] + r.width(), xy[1] + r.height());
    }

    private void setPreviewControlsVisible(boolean show) {
        final View[] vs = {mLeavePreview, mControlBar, mControlPlayPause, mControlMoviePosition, mControlMovieSeekBar};
        for (View v : vs) {
            v.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    public boolean isXYInPreviewFragment(int x, int y) {
        Rect shellRect = getRectOnScreen(mShellView);

        return shellRect.contains(x, y);
    }

    public void pausePreview() {
        mPlayPauseKit.pauseView().performClick();
    }

    public void togglePlayPause() {
        mPlayPauseKit.switcherView().performClick();
    }

    public interface PreviewByMediaItem {
        void onBackPreview();
    }

    public interface PreviewByMovieEdit {
        void onBackPreview();
        MovieEdit getMovieEditForPreview();
    }

    @Override
    public void enterFullScreenPreview() {
        if (DEBUG_PREVIEW_STATUS) Log.d(TAG, "enter FullScreen at " + mPreviewState);
        switch (mPreviewState) {
            case FULL_SCREEN:
            case CORNER:
                break;
            case VANISH:
                mPreviewState = PreviewState.FULL_SCREEN;
                fullScreenMode();
                break;
        }
    }

    @Override
    public void leaveFullScreenPreview() {
        leaveFullScreenPreview(true);
    }

    public void leaveFullScreenPreview(boolean animationMoveLeft) {
        if (DEBUG_PREVIEW_STATUS) Log.d(TAG, "leave FullScreen at " + mPreviewState);
        switch (mPreviewState) {
            case FULL_SCREEN:
            case CORNER:
                AnimatorListenerAdapter animatorLis = new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mPreviewState = PreviewState.VANISHING;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mPreviewState = PreviewState.VANISH;
                        vanishMode();
                    }
                };

                if (animateVanish) {
                    mShellView.animate()
                            .alpha(0).translationXBy( (animationMoveLeft ? -1 : 1) * UIUtils.getScreenWidth() / 3)
                            .setInterpolator(new LinearInterpolator())
                            .setListener(animatorLis)
                            .setDuration(200 * (DEBUG_ANIMATION ? 10 : 1))
                            .start();
                } else {
                    mShellView.clearAnimation();
                    animatorLis.onAnimationStart(null);
                    animatorLis.onAnimationEnd(null);
                }
                break;
            case VANISH:
                break;
        }
    }

    public void cornerToFullScreen() {
        if (DEBUG_PREVIEW_STATUS) Log.d(TAG, "corner to FullScreen at " + mPreviewState);
        switch (mPreviewState) {
            case FULL_SCREEN:
                break;
            case CORNER: {
                Animation.AnimationListener reverseLis = new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        mPreviewState = PreviewState.BACKING_TO_FULL_SCREEN;
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mPreviewState = PreviewState.FULL_SCREEN;
                        backToFullScreen();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                };
                mMoveToCorner.setReverse(true);
                mMoveToCorner.setReverseListener(reverseLis);
                if (animateCornerBack) {
                    mShellView.startAnimation(mMoveToCorner);
                } else {
                    mShellView.clearAnimation();
                    reverseLis.onAnimationStart(mMoveToCorner);
                    reverseLis.onAnimationEnd(mMoveToCorner);
                }
                break;
            }
            case VANISH:
                break;
        }
    }

    private enum SizeType { MATCH_SCREEN, PARTLY_SHOWN, VANISHED }
    public interface OnPreviewSizeChange {
        void onMatchScreen();
        void onPartlyShown();
        void onVanished();
    }

    private void notifySizeChange(SizeType type) {
        if (mOnSizeChange != null) {
            switch (type) {
                case MATCH_SCREEN: mOnSizeChange.onMatchScreen(); break;
                case PARTLY_SHOWN: mOnSizeChange.onPartlyShown(); break;
                case VANISHED: mOnSizeChange.onVanished(); break;
                default:
            }
        }
    }

    private boolean isActivityGone() {
        return getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed();
    }

    private void vanishMode() {
        if (isActivityGone()) return;

        mMovieController.setFullScreenMode(false);
        setPreviewControlsVisible(false);
        mMovieController.suspend();
        mMovieController.suspendResource();
        notifySizeChange(SizeType.VANISHED);
    }

    private void backToFullScreen() {
        if (isActivityGone()) return;

        mShellView.setLeft(0);
        mShellView.setTop(0);
        mShellView.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        mShellView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        fullScreenMode();
        setupFullScreenListeners();
        mShellView.requestLayout();
    }

    private void fullScreenMode() {
        // Change the MovieView's size if need
        if (useMediaItem()) {
            mMovieController.setFullScreenMode(true);
        } else {}
    }

    public void cornerMode(View.OnTouchListener cornerTouchListener) {
        if (DEBUG_PREVIEW_STATUS) Log.d(TAG, "corner mode at " + mPreviewState);
        // Only FULL_SCREEN can perform action
        if (mPreviewState != PreviewState.FULL_SCREEN) return;

        notifySizeChange(SizeType.PARTLY_SHOWN);

        setupTouchListeners(true, cornerTouchListener);

        mControlPlayPause.setKeepScreenOn(false);

        CornerAnimation ca = new CornerAnimation(mShellView, mMovieView, mIsLandscape);
        ca.setDuration(400 * (DEBUG_ANIMATION ? 10 : 1));
        ca.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                setPreviewControlsVisible(false);
                mPreviewState = PreviewState.CORNERING;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mPreviewState = PreviewState.CORNER;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        mMoveToCorner = ca;

        mShellView.startAnimation(ca);
        // TODO : It is very delicate to apply no animation, need take care 1.back from corner 2. Rotate 90 degree
        // TODO : Not easy to handle corner mode with no animation case
    }

    /** {@link PreviewFragment} animate from full-screen to right-bottom corner with {@link CornerAnimation}
     * */
    private class CornerAnimation extends Animation {
        // Shell's right & bottom gap to screen
        private final int gapW = 20, gapH = 20 + ANCHOR_HEIGHT;

        private final int SW, SH;
        // Use Delta-X, Y instead End-X, Y to handle rotate-90-degree case
        private final float shellStartX, shellDeltaX; // shellEndX = shellStartX + shellDeltaX
        private final float shellStartY, shellDeltaY; // shellEndY = shellStartY + shellDeltaY
        private final int shellStartW, shellDeltaW; // shellEndW = shellStartW + shellEndW
        private final int shellStartH, shellDeltaH; // shellEndH = shellStartH + shellEndH

        private final int movieStartW, movieDeltaW; // movieEndW = movieStartW + movieDeltaW
        private final int movieStartH, movieDeltaH; // movieEndH = movieStartH + movieDeltaH

        // -- Values when animation from time, t = 0 ~ 1
        // Values For Movie View Container (shell)
        private int shell_W_t, shell_H_t;
        private int shell_X_t, shell_Y_t;
        private float shell_Rotate_t;
        // Values for Movie View (movie)
        private int movie_W_t, movie_H_t;

        // -- Values end

        private final View shell, movie;

        private AnimationListener normalListener;
        private boolean reverse;
        private AnimationListener reverseListener;
        private boolean isRotated90;
        private boolean rotateBack;
        // FLAGS
        /**
         * true  : when go to corner mode, rotate the landscape video back to landscape form
         * false : when go to corner mode, make the landscape video still in portrait form
         */
        private final boolean rotateBackForLandscape = true;

        // FLAGS
        /**
         * This flag take on duty only when screen's ratio and movie's ratio are not identical
         * Cases :
         * 1. Preview 16:9 movie in 4:3 screen
         * 2. Preview 4:3 movie in 16:9 screen
         * (ACD always make movie in 16:9)
         *
         * E.g. Preview 16:9 movie in 4:3 screen
         *    => So the portrait 16:9 video  & landscape 9:16 video will have left & right blank strips (since we rotate 90 degree)
         * true  : when go to corner mode, video's size fits movie's ratio
         * false : when go to corner mode, video's size fits device's screen ratio (since we use full screen)
         */
        private final boolean removeBlankStripsWhenAtCorner = true;

        public CornerAnimation(View shellView, View movieView, boolean isRotated90Degree) {
            shell = shellView;
            movie = movieView;
            isRotated90 = isRotated90Degree;
            rotateBack = rotateBackForLandscape && isRotated90Degree;

            Rect shellRect = getRectOnScreen(shell);

            // We Move the full screen view to right-bottom corner
            // with time from 0.0 to 1.0

            // Movie's (width, height) shrinks and scales as Shell change
            // Parametric equation
            // t = t : (movie_W_t, movie_H_t)

            // Shell's (X, Y, width, height) is at
            // t = 0 : (0, 0, Screen Width, Screen Height)
            // t = 1 : (shellDeltaX, shellDeltaY, shellNewW, shellNewH)
            // Parametric equation
            // t = t : (shell_X_t, shell_Y_t, shell_W_t, shell_H_t)

            // Screen Width & Height
            SW = UIUtils.getScreenWidth();
            SH = UIUtils.getScreenHeight();

            // Shell's X & Y at t = 0
            // shell_X(0) = shellStartX, shell_Y(0) = shellStartY
            int shift_XY_R90 = (SH - SW) / 2;
            shellStartX = shell.getX() + (isRotated90 ? + shift_XY_R90 : 0);
            shellStartY = shell.getY() + (isRotated90 ? - shift_XY_R90 : 0);

            // Shell's Width & Height at t = 1
            // shell_W(1) = shellNewW, shell_H(1) = shellNewH
            int shellNewH = Math.round(SH / 4);
            int shellNewW = Math.round(shellNewH * shellRect.width() / shellRect.height()); // Keep ratio
            if (removeBlankStripsWhenAtCorner) {
                shellNewW = Math.round(shellNewH * 9.0f / 16.0f);
            }

            // Shell's Scale, (scaleX, scaleY)
            // t = 0 : (1, 1)
            // t = 1 : (scaleX, scaleY)
            final float scaleX = 1.0f * shellNewW / shellRect.width();
            final float scaleY = 1.0f * shellNewH / shellRect.height();

            // Shell's Translation, (TranslateX, TranslateY)
            // t = 0 : (0, 0)
            // t = 1 : (shellDeltaX, shellDeltaY)
            // The shift for Rotate 90 degree
            int shift_DeltaXY_R90 = ( (SH - SW) - (shellNewH - shellNewW) ) / 2;
            if (rotateBack) {
                shift_DeltaXY_R90 = (SH - SW) / 2;
            }

            // The width & height when at corner
            final int cornerW = (rotateBack ? shellNewH : shellNewW);
            final int cornerH = (rotateBack ? shellNewW : shellNewH);

            //shellDeltaX = shellEndX + (proper shift for Rotated90)
            //shellDeltaY = shellEndY + (proper shift for Rotated90)
            shellDeltaX = SW - gapW - cornerW + (isRotated90 ? + shift_DeltaXY_R90 : 0);
            shellDeltaY = SH - gapH - cornerH + (isRotated90 ? - shift_DeltaXY_R90 : 0);

            // Shell's Width & Height (shell_W, shell_H)
            // t = 0 : (shellStartW, shellStartH)
            // t = 1 : (shellNewW, shellNewH) = (shellStartW + shellDeltaW, shellStartH + shellDeltaH)
            // t = t : (shell_W_t, shell_H_t) = (shellStartW + shellDeltaW * t, shellStartH + shellDeltaH * t)
            shellStartW = shell.getMeasuredWidth();
            shellDeltaW = removeBlankStripsWhenAtCorner
                ? cornerW - shellStartW
                : Math.round(shellStartW * (scaleX - 1.0f));

            shellStartH = shell.getMeasuredHeight();
            shellDeltaH = removeBlankStripsWhenAtCorner
                ? cornerH - shellStartH
                : Math.round(shellStartH * (scaleY - 1.0f));

            // Movie's Width & Height, scales as Shell change
            movieStartW = movie.getMeasuredWidth();
            movieDeltaW = removeBlankStripsWhenAtCorner
                ? cornerW - movieStartW
                : Math.round(movieStartW * (scaleX - 1.0f) );

            movieStartH = movie.getMeasuredHeight();
            movieDeltaH = removeBlankStripsWhenAtCorner
                ? cornerH - movieStartH
                : Math.round(movieStartH * (scaleY - 1.0f) );

            // Keep Shell's layout after animation has ended
            // If we did not set this, Shell's Left/Top/Right/Bottom value will changed after requestLayout()
            setFillEnabled(true);
            setFillAfter(true);

            if (DEBUG_ANIMATION) LogCreate(shift_XY_R90, shift_DeltaXY_R90);
        }

        private void LogCreate(int XYR90, int DeltaXYR90) {
            String s = String.format("Corner : screen W, H = %d, %d, shift XY = %d, DeltaXY = %d\n" +
                            "shell (X, Y), Start = (%f, %f), Delta = (%f, %f)\n" +
                            "shell (W, H), Start = (%4d, %4d), Delta = (%4d, %4d)\n" +
                            "movie (W, H), Start = (%4d, %4d), Delta = (%4d, %4d)\n"
                    , SW, SH, XYR90, DeltaXYR90
                    , shellStartX, shellStartY, shellDeltaX, shellDeltaY
                    , shellStartW, shellStartH, shellDeltaW, shellDeltaH
                    , movieStartW, movieStartH, movieDeltaW, movieDeltaH);

            Log.v(TAG, s);
        }

        @Override
        public void setAnimationListener(AnimationListener listener) {
            normalListener = listener;
            super.setAnimationListener(listener);
        }

        public void setReverseListener(AnimationListener listener) {
            reverseListener = listener;
            setAnimationListener(reverseListener);
        }

        /** Reverse on animation's time,
         *  Time goes from 0.0 to 1.0,
         *  but we applyTransformation from 1.0 to 0.0
         *  The animation listeners will remain unchanged
         * */
        public void setReverse(boolean reverseTime) {
            reverse = reverseTime;
            setAnimationListener(reverse ? reverseListener : normalListener);
        }

        // Compute by Parametric equation, f(time), time is in [0, 1]
        private void prepare_f_t(float time) {
            // Rotation value should satisfy f(0:1)
            // f(0) = 90, f(1) = 0
            float q = 1 - time;
            shell_Rotate_t = 90 * q*q*q*q;

            float dw, dh, shiftX, shiftY;
            // Values for Movie View Container (shell)
            // The values should satisfy f(0:1) :
            // width  : f(0) = shellStartW, f(1) = shellStartW + shellDeltaW
            // X      : f(0) = shellStartX, f(1) = shellStartX + shellDeltaX
            // height : f(0) = shellStartH, f(1) = shellStartH + shellDeltaH
            // Y      : f(0) = shellStartY, f(1) = shellStartY + shellDeltaY
            dw = shellDeltaW * time;
            shell_W_t = Math.round(shellStartW + dw);

            shiftX = shellDeltaX * time;
            shell_X_t = Math.round(shellStartX + shiftX);

            dh = shellDeltaH * time;
            shell_H_t = Math.round(shellStartH + dh);

            shiftY = shellDeltaY * time;
            shell_Y_t = Math.round(shellStartY + shiftY);

            // Values for Movie View (movie)
            // The values should satisfy f(0:1) :
            // width  : f(0) = movieStartW, f(1) = movieStartW + movieDeltaW
            // height : f(0) = movieStartH, f(1) = movieStartH + movieDeltaH
            dw = movieDeltaW * time;
            movie_W_t = Math.round(movieStartW + dw);

            dh = movieDeltaH * time;
            movie_H_t = Math.round(movieStartH + dh);
        }

        @Override
        protected void applyTransformation(float time, Transformation trans) {
            prepare_f_t(reverse ? 1 - time : time);
            if (rotateBack) {
                shell.setRotation(shell_Rotate_t);
            }

            // Values For Movie View Container (shell)
            // Set layout values
            shell.getLayoutParams().width = shell_W_t;
            shell.getLayoutParams().height = shell_H_t;

            // Set Position values
            shell.setLeft(shell_X_t);
            shell.setTop(shell_Y_t);
            shell.setRight(shell_X_t + shell_W_t);
            shell.setBottom(shell_Y_t + shell_H_t);

            // Values for Movie View (movie)
            movie.getLayoutParams().width = movie_W_t;
            movie.getLayoutParams().height = movie_H_t;

            // Only requestLayout() when animation is not yet ended
            // We will get infinite looping if still requestLayout() when hasEnded()
            // Loop by :
            // (Android system side) requestLayout() => layout => draw() => getTransformation()
            // (Our Animations side) => applyTransformation() => requestLayout()
            if (!hasEnded()) {
                shell.requestLayout();
            }

            if (DEBUG_ANIMATION) LogAnimate(time);
        }

        private void LogAnimate(float time) {
            String s = String.format("t = %f" +
                            ", shell (X, Y, W, H) = (%4d, %4d, %4d, %4d), movie (W, H) = (%4d, %4d)" +
                            ", reverse = %s, ended = %s, %s"
                    , time
                    , shell_X_t, shell_Y_t, shell_W_t, shell_H_t, movie_W_t, movie_H_t
                    , (reverse ? "o" : "x"), hasEnded() ? "o" : "x", mPreviewState);
            Log.v(TAG, s);
        }
    }
}
