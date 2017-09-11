package com.cyberlink.actiondirector.page.preview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

public abstract class ViewDisplayer {
    protected enum ViewDisplayerState {VISIBLE, SHOWING, HIDING, INVISIBLE}
    protected ViewDisplayerState state = ViewDisplayerState.INVISIBLE;
    protected static final Handler sDisplayerHandler = new Handler();

    protected final TimeInterpolator slower = new DecelerateInterpolator();
    protected final TimeInterpolator faster = new AccelerateInterpolator();

    protected final Runnable animateHide = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    public final class ShowListener extends AnimatorListenerAdapter {
        private final View viewToShow;
        public ShowListener(View v) {
            viewToShow = v;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            state = ViewDisplayerState.SHOWING;
            if (viewToShow != null) {
                viewToShow.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            state = ViewDisplayerState.VISIBLE;
        }
    }

    public final class HideListener extends AnimatorListenerAdapter {
        private final View viewToHide;
        public HideListener(View v) {
            viewToHide = v;
        }
        @Override
        public void onAnimationStart(Animator animation) {
            state = ViewDisplayerState.HIDING;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            state = ViewDisplayerState.INVISIBLE;
            if (viewToHide != null) {
                viewToHide.setVisibility(View.GONE);
            }
        }
    }

    public abstract ViewDisplayer show();

    public abstract void hide();
}
