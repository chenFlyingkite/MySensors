package com.cyberlink.actiondirector.page.editor.moviekit;

public interface SeekUnit {
    void startUserFastSeeking();
    void startUserFastSeeking(boolean isPreciseSeeking);
    void doUserFastSeeking(long seekPos);
    void doUserFastSeeking(long seekPos, boolean isPreciseSeeking);
    void stopUserFastSeeking(long seekPos);
    void seekPlaybackTo(long seekUs, boolean isFastSeek);
}
