package com.cyberlink.actiondirector.page.editor.moviekit;

import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.cyberlink.actiondirector.R;

public class MovieTipTextKit {
    private TextView mMovieTip;
    private boolean isEnabled;

    public MovieTipTextKit(TextView tipTextView) {
        mMovieTip = tipTextView;
        isEnabled = tipTextView != null;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void clearText() {
        if (!isEnabled()) return;

        mMovieTip.setText("");
    }

    public void resetState() {
        if (!isEnabled()) return;

        if (mMovieTip.getVisibility() == View.VISIBLE) {
            mMovieTip.setVisibility(View.GONE);
            mMovieTip.startAnimation(AnimationUtils.loadAnimation(mMovieTip.getContext()
                    , R.anim.fadeout));
        }
    }

    public void showTipsViewValue(String value) {
        if (!isEnabled()) return;

        if (mMovieTip.getVisibility() == View.INVISIBLE || mMovieTip.getVisibility() == View.GONE) {
            mMovieTip.clearAnimation();
            mMovieTip.setVisibility(View.VISIBLE);
            mMovieTip.startAnimation(AnimationUtils.loadAnimation(mMovieTip.getContext()
                    , R.anim.fadein));
        }

        mMovieTip.setText(value);
    }
}
