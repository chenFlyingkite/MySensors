package com.flyingkite.utils;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

@Deprecated
public class Log {
    public static void v(String t, String s) {
        log(s);
    }
    public static void d(String t, String s) {
        log(s);
    }
    public static void i(String t, String s) {
        log(s);
    }

    public static void w(String t, String s) {
        log(s);
    }
    public static void e(String t, String s) {
        log(s);
    }
    private static void log(String s) {
        Say.Log(s);
    }

    /*
    //TODO : RD debug tool for auto set texts for checking ui layouts truncations
    if (childPosition == 0) {
        runText.setViews(txtViewMemberCount, txtViewGroupName, txtViewLastMessage, unreadCountView);
        h.removeCallbacks(runText);
        h.post(runText);
    }
    */

    private final Handler h = new Handler(Looper.getMainLooper());
    private final AutoRun runText = new AutoRun();

    private class AutoRun implements Runnable {
        private TextView countView;
        private TextView nameView;
        private TextView lastMessageView;
        private TextView unreadCountView;
        private void setViews(TextView count, TextView name, TextView last, TextView unread) {
            countView = count;
            nameView = name;
            lastMessageView = last;
            unreadCountView = unread;
        }

        private String ruler
                = "123456789-123456789=123456789%123456789#123456789@"
                + "123456789-123456789=123456789%123456789#123456789@"
                + "123456789-123456789=123456789%123456789#123456789@";
        private int max = ruler.length();
        private int countMax = 4;
        private int count = 1;
        private int nameMax = 70;
        private int name = 1;
        private int lmsgMax = max;
        private int lmsg = 1;
        private int urMax = 3;
        private int ur = 1;

        @Override
        public void run() {
            // member count
            count = setText(countView, count, countMax);
            name = setText(nameView, name, nameMax);
            lmsg = setText(lastMessageView, lmsg, lmsgMax);
            ur = setText(unreadCountView, ur, urMax);
            h.postDelayed(this, 100);
        }

        private int setText(TextView v, int n, int max) {
            v.setText(ruler.substring(0, n));
            return next(n, max);
        }

        private int next(int value, int max) {
            return value == max ? 0 : (value + 1);
        }
    }
}
