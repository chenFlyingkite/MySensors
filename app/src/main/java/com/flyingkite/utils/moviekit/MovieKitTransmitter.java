package com.cyberlink.actiondirector.page.editor.moviekit;

public interface MovieKitTransmitter {
    /**
     * Panels may want to use its MoviePlayKit, so just pass MovieKit's
     * requested parameters and send the MovieKit to MovieController
     */
    PlayUnit getPlayUnit();
    MoviePlayKit getMoviePlayKit();
    void setMoviePlayKit(MoviePlayKit kit);

    MovieROIKit getROIKit();
    MovieTipTextKit getTipKit();
}
