package com.flyingkite.utils;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ThreadUtil {
    private ThreadUtil() {}

    //-------------------------------------------------------------------------
    // The thread pool for classes that have many simple tasks to be run
    public static final ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    //-------------------------------------------------------------------------

    public static boolean isUIThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    public static void logUIThread(String s) {
        Log.e("Hi", "isUIThread = " + (isUIThread() ? "o" : "x") + ", " + s);
    }

    public static void runOnUiThread(Runnable action) {
        new Handler(Looper.getMainLooper()).post(action);
    }

    public static void runOnWorkerThread(final Runnable action) {
        if (isUIThread()) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    action.run();
                    return null;
                }
            }.execute();
        } else {
            action.run();
        }
    }
}
