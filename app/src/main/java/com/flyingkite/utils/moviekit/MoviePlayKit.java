package com.cyberlink.actiondirector.page.editor.moviekit;

import android.view.View;
import android.widget.ViewSwitcher;

public class MoviePlayKit {
    private boolean isEnabled;
    private final ViewSwitcher mSwitcherPlayPause;
    private final int INDEX_PLAY;
    private final int INDEX_PAUSE;
    protected final PlayUnit mPlayUnit;

    public MoviePlayKit(PlayUnit playUnit) {
        this(null, playUnit, -1, -1);
    }

    public MoviePlayKit(ViewSwitcher playPause, PlayUnit playUnit
            , int playChildIndex, int pauseChildIndex) {
        mSwitcherPlayPause = playPause;
        mPlayUnit = playUnit;
        INDEX_PLAY = playChildIndex;
        INDEX_PAUSE = pauseChildIndex;
        isEnabled = playPause != null && playUnit != null;

        setupPlayPauseSwitcher();
    }

    public void setEnabled(boolean enable) {
        isEnabled = enable;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public ViewSwitcher switcherView() {
        return mSwitcherPlayPause;
    }

    public View playView() {
        if (!isEnabled()) return null;

        return mSwitcherPlayPause.getChildAt(INDEX_PLAY);
    }

    public View pauseView() {
        if (!isEnabled()) return null;

        return mSwitcherPlayPause.getChildAt(INDEX_PAUSE);
    }

    private void switcherDisplay(int child) {
        if (!isEnabled()) return;

        mSwitcherPlayPause.setDisplayedChild(child);
    }

    protected void displayPlay() {
        switcherDisplay(INDEX_PLAY);
    }

    protected void displayPause() {
        switcherDisplay(INDEX_PAUSE);
    }

    private void setupPlayPauseSwitcher() {
        if (!isEnabled()) return;

        mSwitcherPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickSwitcher(v);
            }
        });
        playView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickPlay(v);
            }
        });
        pauseView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickPause(v);
            }
        });
    }

    public void updatePlayPauseBtn() {
        if (!isEnabled()) return;

        onUpdatePlayPauseBtn();
    }

    protected void onUpdatePlayPauseBtn() {
        if (mPlayUnit.isPaused()) {
            displayPlay();
        } else {
            displayPause();
        }
    }

    protected void onClickSwitcher(View v) {
        mSwitcherPlayPause.getCurrentView().callOnClick();
    }

    protected void onClickPlay(View v) {
        mPlayUnit.play();
        displayPause();
    }

    protected void onClickPause(View v) {
        mPlayUnit.pause();
        displayPlay();
    }
}
