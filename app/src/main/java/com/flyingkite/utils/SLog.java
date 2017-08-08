package com.flyingkite.utils;

@Deprecated
public class SLog {
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

}
