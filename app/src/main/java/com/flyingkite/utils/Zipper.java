package com.cyberlink.util;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class Zipper {
    private static final String TAG = Zipper.class.getSimpleName();
    private Zipper() {}

    public static File unzip(@NonNull File dstFolder, @NonNull File srcFile) {
        try {
            return _unzip(dstFolder, new FileInputStream(srcFile));
        } catch (IOException e) { // or FileNotFoundException
            Log.e(TAG, e.toString());
            return null;
        }
    }

    public static File unzip(@NonNull File dstFolder, @NonNull final InputStream fis) {
        try {
            return _unzip(dstFolder, fis);
        } catch (IOException e) { // or FileNotFoundException
            Log.e(TAG, e.toString());
            return null;
        }
    }

    private static ZipInputStream makeZis(File dstFolder, InputStream fis) throws FileNotFoundException {
        dstFolder.mkdirs();
        FileUtils.ensureNoMediaFile(dstFolder); // -> make ".nomedia" in dstFolder

        return new ZipInputStream(new BufferedInputStream(fis));
    }

    private static File _unzip(File dstFolder, InputStream fis) throws IOException {
        ZipInputStream zis = makeZis(dstFolder, fis);

        ZipEntry ze;
        while ((ze = zis.getNextEntry()) != null) {
            String filename = ze.getName();

            File dstFile = new File(dstFolder, filename);
            Log.i(TAG, "Extracting file: " + dstFile.getAbsolutePath());

            if (ze.isDirectory()) {
                continue;
            }

            // Handle for cases that ZipInputStream's entry reaches file and then file's parent folder
            // Like "/a/b" and then next is "/a"
            dstFile.getParentFile().mkdirs();

            // Read from zis and flush to dstFile
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dstFile));

            byte[] buf = new byte[8192]; // 8192 = default buffer size of BufferedOutputStream & BufferedInputStream
            int read;

            while ((read = zis.read(buf)) != -1) {
                bos.write(buf, 0, read);
            }
            bos.flush();
            bos.close();
            zis.closeEntry();
        }

        return dstFolder;
    }
}
