package com.cyberlink.util;

/**
 * Borrow some code in core / java / android / util / MathUtils.java
 * No need write again handy methods if {@link Math} can achieve. <br/>
 *
 * See source code
 * https://android.googlesource.com/platform/frameworks/base/+/0e40462/core/java/android/util/MathUtils.java
 * */
public final class MathUtils {
    private MathUtils(){}

    /** see source code's  method in link
     * public static int constrain(int amount, int low, int high)
     */
    public static int fallInRange(int value, int min, int max) {
        return Math.min(Math.max(min, value), max);
    }

    /** see source code's  method in link
     * public static int constrain(long amount, long low, long high)
     */
    public static long fallInRange(long value, long min, long max) {
        return Math.min(Math.max(min, value), max);
    }

    /** see source code's  method in link
     * public static int constrain(float amount, float low, float high)
     */
    public static float fallInRange(float value, float min, float max) {
        return Math.min(Math.max(min, value), max);
    }

    public static boolean isInRange(long target, long min, long max) {
        return min <= target && target < max;
    }
}
