package com.cyberlink.actiondirector.page.editor.moviekit;

import android.widget.SeekBar;

public class MovieSeekBySeekBarKit implements SeekBar.OnSeekBarChangeListener {
    private boolean isEnabled;
    private SeekUnit mSeekUnit;

    private long kitMovieDurationUs;
    protected long kitSeekUs; // used for source's fast seeking

    public MovieSeekBySeekBarKit(SeekUnit seekUnit, long duration) {
        mSeekUnit = seekUnit;
        kitMovieDurationUs = duration;
        isEnabled = seekUnit != null && duration > 0;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        kitSeekUs = Math.round(1.0 * kitMovieDurationUs * progress / seekBar.getMax());
        if (fromUser) {
            mSeekUnit.doUserFastSeeking(kitSeekUs);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mSeekUnit.startUserFastSeeking();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mSeekUnit.stopUserFastSeeking(kitSeekUs);
    }
}
