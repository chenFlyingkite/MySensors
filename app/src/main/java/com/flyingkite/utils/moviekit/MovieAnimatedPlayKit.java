package com.cyberlink.actiondirector.page.editor.moviekit;

import android.view.View;
import android.widget.ViewSwitcher;

import com.cyberlink.actiondirector.page.editor.PlaybackProgressListener;

public class MovieAnimatedPlayKit extends MoviePlayKit {
    private AnimatedPlayUnit mInfo;

    public interface AnimatedPlayUnit extends PlayUnit {
         PlaybackProgressListener getPlaybackListener();
    }

    public MovieAnimatedPlayKit(ViewSwitcher playPause, AnimatedPlayUnit info
            , int playChildIndex, int pauseChildIndex) {
        super(playPause, info, playChildIndex, pauseChildIndex);
        mInfo = info;
    }

    @Override
    protected void onUpdatePlayPauseBtn() {
        if (mPlayUnit.isPaused()) {
            if (switcherView().getCurrentView() == pauseView())
                switcherView().showNext();
            pauseView().clearAnimation();
            playView().clearAnimation();
        } else {
            displayPause();
        }
    }

    @Override
    protected void onClickSwitcher(View v) {
        switcherView().getCurrentView().setVisibility(View.VISIBLE);
        switcherView().getCurrentView().callOnClick();
    }

    @Override
    protected void onClickPlay(View v) {
        PlaybackProgressListener listener = mInfo.getPlaybackListener();
        if (listener == null || !listener.onPlayPressed()) {
            // Listener didn't accept play behavior.
            return;
        }

        mPlayUnit.play();

        playView().setClickable(false);
        pauseView().setClickable(true);
        pauseView().setPressed(false);

        // Clear previous animation out first.
        pauseView().clearAnimation();
        displayPause();
        // We don't want to show pause button with animation in.
        pauseView().clearAnimation();
        // We don't want to show pause button at final status.
        pauseView().setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onClickPause(View v) {
        PlaybackProgressListener listener = mInfo.getPlaybackListener();
        if (listener == null || !listener.onPausePressed()) {
            // Listener didn't accept play behavior.
            return;
        }
        mPlayUnit.pause();

        playView().setClickable(true);
        playView().setPressed(false);
        pauseView().setClickable(false);

        // Clear previous animation out first.
        playView().clearAnimation();
        displayPlay();
    }
}
