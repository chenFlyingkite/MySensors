package com.flyingkite.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class StringUtils {

    private static final String TAG = "StringUtils";
    private static final boolean DEBUG = true;

    private StringUtils() { }

    public static String formatByte(long bytes) {
        if (bytes < 1024) {
            return String.format(Locale.US, "%dB", bytes);
        } else if (bytes < 1048576) {
            return String.format(Locale.US, "%.2fKB", bytes / 1024F);
        } else {
            return String.format(Locale.US, "%.2fMB", bytes / 1048576F);
        }
    }

    /**
     * Get memory usage from {@link Runtime} instance.
     * <p/>
     * NOTE: Don't try to parse return String, because it could be meaningless.
     *
     * @return Memory usage.
     */
    public static String getMemoryUsage() {
        Runtime rt = Runtime.getRuntime();
        // https://www.fabric.io/cyberlink/android/apps/com.cyberlink.powerdirector.dra140225_01/issues/56d1bd6ef5d3a7f76b26d4ec
        if (rt == null) return "Runtime instance is unavailable";

        long max = rt.maxMemory();
        long free = rt.freeMemory();
        long used = rt.totalMemory() - free;
        return String.format(Locale.US, "Max[%s] Free[%s] Used[%s]", StringUtils.formatByte(max), StringUtils.formatByte(free), StringUtils.formatByte(used));
    }

    public static String toTimeStringMMSS(long ms) {
        if (ms < 0) return "-" + toTimeStringMMSS(-ms);

        ms = ms + 500; // Round on second
        final long s = ms / 1000;
        final long sec = s % 60;
        final long min = s / 60;
        // XXX: Use String concat instead of format because format need more resource or execution time.
        // return String.format(Locale.US, "%02d:%02d", min, sec);
        return (min < 10 ? "0" : "") + min + ":" + (sec < 10 ? "0" : "") + sec;
    }

    public static String toTimeStringMMSSF(long ms) {
        if (ms < 0) return "-" + toTimeStringMMSSF(-ms);

        ms = (ms + 50); // Round on frame
        final long s = ms / 1000;
        final long sec = s % 60;
        final long min = s / 60;
        final long f = ms / 100 % 10;
        // XXX: Use String concat instead of format because format need more resource or execution time.
        // return String.format(Locale.US, "%02d:%02d.%01d", min, sec, f);
        return (min < 10 ? "0" : "") + min + ":" + (sec < 10 ? "0" : "") + sec + "." + f;
    }

    /**
     * Convert milliseconds to simple time string hh:mm:ss, such as 01:23:56
     * @param ms
     * @return
     */
    public static String toTimeStringHHMMSS(long ms) {
        if (ms < 0) return "-" + toTimeStringHHMMSS(-ms);

        ms = ms + 500; // Round on second
        final long hours = ms / 3600000;
        ms %= 3600000;
        final long mins = ms / 60000;
        ms %= 60000;
        final long sec = ms / 1000;

        return String.format(Locale.US, "%02d:%02d:%02d", hours, mins, sec);
    }

    /**
     * Convert milliseconds to simple time string hh:mm, such as 01:23
     * @param ms
     * @return
     */
    public static String toTimeStringHHMM(long ms) {
        if (ms < 0) return "-" + toTimeStringHHMM(-ms);

        ms = ms + 500; // Round on second
        final long hours = ms / 3600000;
        ms %= 3600000;
        final long mins = ms / 60000;


        return String.format(Locale.US, "%02d:%02d", hours, mins);
    }

    /**
     * Return string format datetime with underline.
     *
     * @param ms Milliseconds of the datetime.
     * @return String format with underline.
     */
    public static String toDateTimeString(long ms) {
        if (ms <= 0) return null;
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date(ms));
    }

    public static String toDateString(long ms) {
        return new SimpleDateFormat("yyyy/MM/dd", Locale.US).format(new Date(ms));
    }

    private final static char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    // http://stackoverflow.com/a/9855338/509029
    public static String toHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int i = 0;
        for (byte b : bytes) {
            int v = b & 0xFF;
            hexChars[i++] = HEX_DIGITS[v >>> 4];
            hexChars[i++] = HEX_DIGITS[v & 0x0F];
        }
        return new String(hexChars);
    }

    // http://stackoverflow.com/a/140861/3901422
    public static byte[] toByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    /**
     * Return GUID-like string which is UUID-based string.
     * @param args
     * @return
     */
    public static String guidHash(String... args) {
        return UUID.nameUUIDFromBytes(toBytes(args)).toString().toUpperCase(Locale.US);
    }

    private static byte[] toBytes(String... args) {
        return TextUtils.join("", args).getBytes();
    }

    public static String md5Hash(String... args) {
        return hashAll("MD5", args);
    }

    public static String sha1Hash(String... args) {
        return hashAll("SHA-1", args);
    }

    public static String hashAll(String algo, String... args) {
        return hashToString(algo, TextUtils.join("", args));
    }

    public static String hashToString(String algo, String src) {
        return toHexString(hash(algo, src));
    }

    public static byte[] hash(String algo, String src) {
        byte[] hash = null;

        try {
            MessageDigest digest = MessageDigest.getInstance(algo);
            digest.update(src.getBytes("UTF-8"));
            hash = digest.digest();
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "", e);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "", e);
        }

        return hash;
    }

    public static boolean isEmpty(CharSequence str) {
        return TextUtils.isEmpty(str);
    }

    public static boolean equals(CharSequence a, CharSequence b) {
        return TextUtils.equals(a, b);
    }

    public static boolean equalsIgnoreCase(CharSequence a, CharSequence b) {
        if (a == b) return true;
        int length;
        if (a != null && b != null && (length = a.length()) == b.length()) {
            if (a instanceof String && b instanceof String) {
                return ((String) a).equalsIgnoreCase((String) b);
            } else {
                for (int i = 0; i < length; i++) {
                    if (Character.toLowerCase(a.charAt(i)) != Character.toLowerCase(b.charAt(i))) return false;
                }
                return true;
            }
        }
        return false;
    }

    public static String replaceCharsToUnderline(String transformStr) {
        // Reserved words: http://en.wikipedia.org/wiki/Filename
        return transformStr.replaceAll("[\\s'\"\\\\?*\\/:\"<>|%^]+", "_");
    }

    public static boolean isLegalChars(String examStr) {
        return examStr.matches("[^\\/:*?\"<>|^]*");
    }

    public static String getCertificateSHA1Fingerprint(Context context) {
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();
        int flags = PackageManager.GET_SIGNATURES;
        PackageInfo packageInfo;
        try {
            packageInfo = pm.getPackageInfo(packageName, flags);
        } catch (PackageManager.NameNotFoundException e) {
            if (DEBUG) Log.e(TAG, "Cannot get package info.", e);
            return null;
        }

        Signature[] signatures = packageInfo.signatures;
        CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X509");
        } catch (CertificateException e) {
            if (DEBUG) Log.e(TAG, "Cannot get X509 certification factory.", e);
            return null;
        }

        byte[] cert = signatures[0].toByteArray();
        InputStream input = new ByteArrayInputStream(cert);
        X509Certificate c = null;
        try {
            c = (X509Certificate) cf.generateCertificate(input);
        } catch (CertificateException e) {
            if (DEBUG) Log.e(TAG, "Cannot print certification.", e);
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException ig) { }
        }

        String hexString = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] publicKey = md.digest(c.getEncoded());
            hexString = toHexString(publicKey);
        } catch (NoSuchAlgorithmException e) {
            if (DEBUG) Log.e(TAG, "No SHA1 algorithm.", e);
        } catch (CertificateEncodingException e) {
            if (DEBUG) Log.e(TAG, "SHA1 encoding exception.", e);
        }

        return hexString;
    }

    /**
     * Encode String with salt by XOR algorithm.
     *
     * @param s String to be encoded.
     * @param salt Specific salt.
     * @return Encoded string.
     */
    public static String encodeXOR(String s, byte[] salt) {
        return Base64.encodeToString(xorWithKey(s.getBytes(), salt), Base64.NO_WRAP);
    }

    /**
     * Decode String with salt by XOR algorithm.
     *
     * @param s String to be decoded.
     * @param salt Salt that used when encoded.
     * @return Source string.
     */
    public static String decodeXOR(String s, byte[] salt) {
        return new String(xorWithKey(Base64.decode(s, Base64.NO_WRAP), salt));
    }

    private static byte[] xorWithKey(byte[] a, byte[] key) {
        byte[] out = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = (byte) (a[i] ^ key[i%key.length]);
        }
        return out;
    }
}
