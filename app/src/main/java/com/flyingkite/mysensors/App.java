package com.flyingkite.mysensors;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.flyingkite.utils.StringUtils;

/**
 * PowerDirector Application class inherited from Android Application class.
 * This is a handy class to provide a static method to access Application Context,
 * Resources, ... etc.
 * <p/>
 * Internal member variables should always be available because lifecycle of this Application
 * instance should be as long as process running.
 * <p/>
 * We hope that you don't put lots of task except Application or Global visible level task
 * into this class. Here only care about application, service, context deputies.
 * Please move your implementation detail, logic into your classes.
 *
 * @see Application
 */
public class App extends Application {

    private static final String TAG = "App";
    private static final boolean DEBUG = true;

    // Application singleton for convenient access.
    private static App instance;

    private Handler mainHandler = null;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //MultiDex.install(this);
    }

    /**
     * This is the first line when application launched. Doing any initialization, configuration
     * is a good place here.
     * But keep in mind that your task should as simple, fast as possible because heavy task would
     * hang your application launching, even cause ANR in worst case.
     *
     * @see Application#onCreate()
     */
    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate");

        if (DEBUG) {
            // http://developer.android.com/intl/zh-tw/training/articles/perf-anr.html
            // For performance tuning, you must keep your application responsive to avoid ANR.
            // Use StrictMode to check if any rock on the road and need to move it out.
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    //.detectDiskReads()
                    //.detectDiskWrites()
                    .detectNetwork()
                    .detectCustomSlowCalls()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectActivityLeaks()
                    //.detectFileUriExposure()
                    //.detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    //.detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    //.penaltyDeath() // Lots of code violate restriction. Let app die will block developing.
                    .build());
        }
        long start = System.currentTimeMillis();
        super.onCreate();
        instance = this;

        mainHandler = new Handler(getMainLooper());

        Log.v(TAG, "onCreate took " + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * A handy, static method to get Application Context.
     *
     * @return Return the context of the single, global Application object of the current process.
     * @see Application#getApplicationContext()
     */
    public static Context getContext() {
        return instance.getApplicationContext();
    }

    // -------------- SystemServices Utils -------------- Start

    /**
     * A handy, static method to check the active network availability
     * Active network maybe WIFI, LTE(MOBILE:4G), HSPA+(MOBILE:3G), BLUETOOTH, etc.
     *
     * @return whether active network is available
     * @see ConnectivityManager#getActiveNetworkInfo()
     * @see ConnectivityManager#getAllNetworkInfo()
     */
    public static boolean isNetworkAvailable() {
        // We can examine all possible networks, WIFI, LTE(4G), HSPA+(3G), Bluetooth by getAllNetworkInfo()
        // But only check for the Active one.
        NetworkInfo net = getConnectivityManager().getActiveNetworkInfo();
        return  net != null && net.isConnectedOrConnecting() && net.isAvailable();
    }

    private static ConnectivityManager getConnectivityManager() {
        return (ConnectivityManager) instance.getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
    }

    public static LayoutInflater getLayoutInflater() {
        return (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    // See in
    // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/core/res/res/values/dimens.xml
    // ...
    //    <!-- Height of the status bar -->
    //    <dimen name="status_bar_height">24dp</dimen>
    //    <!-- Height of the bottom navigation / system bar. -->
    //    <dimen name="navigation_bar_height">48dp</dimen>
    //    <!-- Height of the bottom navigation bar in portrait; often the same as @dimen/navigation_bar_height -->
    //    <dimen name="navigation_bar_height_landscape">48dp</dimen>
    //    <!-- Width of the navigation bar when it is placed vertically on the screen -->
    //    <dimen name="navigation_bar_width">48dp</dimen>
    // ...
    public static int getNavigationBarHeight() {
        String keyOfNavigationBar = getRes().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
                ? "navigation_bar_height"
                : "navigation_bar_height_landscape";

        int resourceId = getRes().getIdentifier(keyOfNavigationBar, "dimen", "android");
        if (resourceId > 0) {
            return getRes().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private static boolean hasSystemUiFlags(Activity activity, int flags) {
        View decorView = activity.getWindow().getDecorView();
        return flags == (decorView.getSystemUiVisibility() & flags);
    }

    // -------------- SystemServices Utils -------------- End

    /**
     * A handy, static method to get Resources.
     *
     * @return Return a Resources instance for your application's package.
     */
    public static Resources getRes() {
        return getContext().getResources();
    }

    /**
     * A handy, static method to get Boolean value from Resources.

     * @param id The desired resource identifier.
     * @return Returns the boolean value contained in the resource.
     * @see Resources#getBoolean(int)
     */
    public static boolean getResBoolean(int id) {
        return getRes().getBoolean(id);
    }

    /**
     * A handy, static method to get Integer value from Resources.
     *
     * @param id The desired resource identifier.
     * @return Returns the integer value contained in the resource.
     * @see Resources#getInteger(int)
     */
    public static int getResInteger(int id) {
        return getRes().getInteger(id);
    }

    /**
     * A handy, static method to get String value from Resources.
     *
     * @param id The desired resource identifier.
     * @return The string data associated with the resource, stripped of styled text information.
     * @see Resources#getString(int)
     */
    public static String getResString(int id) {
        return getRes().getString(id);
    }

    /**
     * A handy, static method to get String values with arguments from Resources.
     *
     * @param id The desired resource identifier.
     * @param formatArgs The format arguments that will be used for substitution.
     * @return The string data associated with the resource, stripped of styled text information.
     * @see Resources#getString(int, Object...)
     */
    public static String getResString(int id, Object... formatArgs) {
        Resources res = getRes();
        if (res == null) return null;
        return res.getString(id, formatArgs);
    }

    private static boolean isMainThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();
    }

    public static void assertMainThread() {
        if (!isMainThread())
            throw new AssertionError("Must be called from the main thread!");
    }

    /**
     * Dispatch the runnable task to the main thread message queue if current thread is not
     * the main thread. Or execute runnable immediately if on it.
     * <p/>
     * <b>NOTE</b>: This method DOES NOT guarantee to execute right after runnable on main thread
     * because we didn't wait for it. So use this method carefully.
     *
     * @param run Task to be run.
     */
    public static void runOnUiThread(Runnable run) {
        if (isMainThread()) {
            // Current thread is UI thread, run it directly.
            run.run();
        } else {
            instance.mainHandler.post(run);
        }
    }

    public static boolean isDebuggable() {
        // http://stackoverflow.com/questions/4276857/getting-debuggable-value-of-androidmanifest-from-code
        return (getContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    /**
     * A handy, static method to show Toast with given resource identifier.
     *
     * @param id The desired resource identifier
     */
    public static void showToast(int id) {
        showToast(getResString(id));
    }

    /**
     * A handy, static method to show Toast with given resource identifier and its arguments.
     *
     * @param id The desired resource identifier.
     * @param formatArgs The format arguments that will be used for substitution.
     */
    public static void showToast(int id, Object... formatArgs) {
        showToast(getResString(id, formatArgs));
    }

    /**
     * A handy, static method to show Toast with given String.
     *
     * @param msg The given String.
     */
    public static void showToast(final String msg) {
        showToastInternal(msg, Toast.LENGTH_SHORT);
    }

    /**
     * (For R&D used only) To show developing message via Toast, so that user won't have to check logcat message.
     * In release build, it would dump VERBOSE in logcat instead.
     * <p/>
     * <b color=red>NOTE</b>: Don't log message that is important, have security concerns, personal information, etc.
     */
    public static void showToastForDeveloping(String tagName, String msg) {
        if (TextUtils.isEmpty(msg) || "".equals(msg)) return; // http://crashes.to/s/3f2de27dbbc
        if (DEBUG) {
            showToast("R&D@" + tagName + ": " + msg);
            try {
                throw new Exception("code stack trace");
            } catch (Exception e) {
                // Dump stack trace to let R&D find out code position easily.
                // Using verbose logging level means it isn't important and may be ignored.
                Log.v(tagName, msg, e);
            }
        } else {
            Log.v(tagName, msg);
        }
    }
    
    public static void showLongToast(int id) {
        showLongToast(getResString(id));
    }

    public static void showLongToast(int resId, Object... formatArgs) {
        showLongToast(getResString(resId, formatArgs));
    }

    public static void showLongToast(final String msg) {
        showToastInternal(msg, Toast.LENGTH_LONG);
    }

    /**
     * Make a standard toast that just contains a text view.
     *
     * @param text The text to show. Can be formatted text.
     * @param duration How long to display the message. Either {@link Toast#LENGTH_SHORT}
     *                 or {@link Toast#LENGTH_LONG}.
     * @see Toast#makeText(Context, CharSequence, int)
     */
    private static void showToastInternal(final String text, final int duration) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), text, duration).show();
            }
        });
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "onLowMemory: " + StringUtils.getMemoryUsage());
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.w(TAG, "onTrimMemory[" + level + "]: " + StringUtils.getMemoryUsage());
    }
}
