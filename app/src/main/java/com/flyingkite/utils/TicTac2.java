package com.flyingkite.utils;

import android.util.Log;

import java.util.Stack;

/** The class performing the same intention with {@link TicTac}.
 * Unlike {@link TicTac} provides static method and uses global time stack,
 * {@link TicTac2} provides for creating instance and use its own one time stack. <br/>
 * {@link TicTac2} is specially better usage for tracking performance in different AsyncTasks,
 * by each task create a new object and call its {@link TicTac2#tic()} and {@link TicTac2#tac(String)} in task.
 */
public class TicTac2 {
    private static final String TAG = "TicTac2";
    // A handy tic-tac to track the performance
    private Stack<Long> tictac = new Stack<>();

    public void tic() {
        tictac.push(System.currentTimeMillis());
    }

    public void tacF(String format, Object... params) {
        tac(String.format(format, params));
    }

    public void tac(String msg) {
        long tac = System.currentTimeMillis();
        if (tictac.size() < 1) {
            logError(tac, msg);
            return;
        }

        long tic = tictac.pop();

        StringBuilder s = new StringBuilder();
        // Reveal the tic's depth by adding space " "
        int n = tictac.size();
        for (int i = 0; i < n; i++) {
            s.append(" ");
        }
        // Our message
        s.append("[").append(tac - tic).append("] : ").append(msg);
        logTac(s.toString());
    }

    public void reset() {
        tictac.clear();
    }

    protected void logError(long tac, String msg) {
        Log.e(TAG, "X_X [tic = N/A, tac = " + tac + "] : " + msg);
    }

    protected void logTac(String s) {
        Log.e(TAG, s);
    }

}
