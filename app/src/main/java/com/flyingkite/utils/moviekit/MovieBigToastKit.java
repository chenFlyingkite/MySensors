package com.cyberlink.actiondirector.page.editor.moviekit;


import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.cyberlink.actiondirector.App;
import com.cyberlink.actiondirector.R;
import com.cyberlink.actiondirector.page.preview.ViewDisplayer;

public class MovieBigToastKit {
    private final TextView toastView;
    private boolean isEnabled;
    private final ToastDisplayer mDisplayer = new ToastDisplayer();

    public MovieBigToastKit(TextView view) {
        toastView = view;
        isEnabled = view != null;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void showToast(String msg) {
        if (!isEnabled()) return;

        toastView.setText(msg);
        mDisplayer.show().requestHideWhenIdle();
    }

    private class ToastDisplayer extends ViewDisplayer {
        private final int SHOWING_DURATION = 1000;

        @Override
        public ToastDisplayer show() {
            sDisplayerHandler.removeCallbacks(animateHide);

            toastView.clearAnimation();
            toastView.setVisibility(View.VISIBLE);
            toastView.startAnimation(AnimationUtils.loadAnimation(App.getContext(), R.anim.fadein));
            return this;
        }


        public void requestHideWhenIdle() {
            sDisplayerHandler.removeCallbacks(animateHide);
            sDisplayerHandler.postDelayed(animateHide, SHOWING_DURATION);
        }

        @Override
        public void hide() {
            sDisplayerHandler.removeCallbacks(animateHide);
            toastView.setVisibility(View.GONE);
            toastView.startAnimation(AnimationUtils.loadAnimation(App.getContext(), R.anim.fadeout));
        }
    }
}
