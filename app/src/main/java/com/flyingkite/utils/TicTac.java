package com.flyingkite.utils;

import android.util.Log;

import java.util.Stack;

public class TicTac {
    private static final String TAG = "Hi";
    // A handy tic-tac to track the performance
    private static Stack<Long> tictac = new Stack<>();

    public static void tic() {
        tictac.push(System.currentTimeMillis());
    }

    public static void tacF(String format, Object... params) {
        tac(String.format(format, params));
    }

    public static void tac(String msg) {
        long tac = System.currentTimeMillis();
        if (tictac.size() < 1) {
            Log.e(TAG, "X_X [tic = N/A, tac = " + tac + "] : " + msg);
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
        Log.e(TAG, s.toString());
    }
}
