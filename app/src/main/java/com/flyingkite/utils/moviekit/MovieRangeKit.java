package com.cyberlink.actiondirector.page.editor.moviekit;

/** MovieRangeKit is bound used for movie's seek & playback range.
 * <p>When {@link #isEnabled} is true, Movie can seek & playback within ({@link #minUs}, {@link #maxUs})</p>
 * <p>When {@link #isEnabled} is false, Movie can seek & playback within (0, Movie's duration)</p>
 * */
public class MovieRangeKit {
    public boolean isEnabled;
    public long minUs;
    public long maxUs;
}
