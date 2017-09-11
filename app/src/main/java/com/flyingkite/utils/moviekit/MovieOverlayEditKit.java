package com.cyberlink.actiondirector.page.editor.moviekit;

import android.view.View;

public class MovieOverlayEditKit {
    private final View mToolsContainerView;
    private boolean isEnabled;

    public MovieOverlayEditKit(View toolsView) {
        mToolsContainerView = toolsView;
        isEnabled = toolsView != null;
    }

    public View getToolsContainerView() {
        return mToolsContainerView;
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}
