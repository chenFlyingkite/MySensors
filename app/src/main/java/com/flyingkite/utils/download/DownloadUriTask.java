package com.cyberlink.actiondirector.networkmanager.download;

import android.util.Log;

import com.cyberlink.actiondirector.networkmanager.ImmutableFraction;
import com.cyberlink.actiondirector.networkmanager.task.IAsyncTaskResultProgressCallback;
import com.cyberlink.actiondirector.networkmanager.task.InfoTask;
import com.cyberlink.actiondirector.networkmanager.task.ResponseError;
import com.cyberlink.actiondirector.util.NetworkUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadUriTask extends InfoTask {
    private static final String TAG = DownloadUriTask.class.getSimpleName();
    private static final int BUFFER_SIZE = 65536; // 64KB = Max TCP packet size

    protected AtomicBoolean mIsCancelled = new AtomicBoolean(false);

    /** Use {@link Listener} instead*/
    @Deprecated
    public static interface Callback extends IAsyncTaskResultProgressCallback<File, ResponseError, Void, ImmutableFraction> {
    }

    public interface Listener {
        void onPreExecute();
        void onPostExecute();
        void complete(File result);

        void error(Exception error);

        void cancel();

        void progress(long progress, long max);
    }

    protected URI mUri;
    private final File mFile;
    private Callback mCallback;
    private Listener listener;
    private ImmutableFraction mProgressFraction;

    public DownloadUriTask(final URI uri, final File file, final Callback callback) {
        mUri = uri;
        mFile = file;
        mCallback = callback;
        mProgressFraction = null;
    }

    public DownloadUriTask(URI uri, File file, Listener _listener) {
        mUri = uri;
        mFile = file;
        listener = _listener;
    }

    private void begin() {
        if (listener != null) {
            listener.onPreExecute();
        }
    }
    private void end() {
        if (listener != null) {
            listener.onPostExecute();
        }
    }

    @Override
    public void run() {
        Log.d(TAG, "run");
        begin();

        InputStream is = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;

        try {
            final HttpGet request = new HttpGet();
            request.setURI(mUri);
            final HttpResponse response = NetworkUtils.getClient().execute(request);

            final HttpEntity entity = response.getEntity();
            final long length = entity.getContentLength();
            is = entity.getContent();

            final byte buf[] = new byte[BUFFER_SIZE];
            int read;
            int write = 0;
            fos = new FileOutputStream(mFile);
            bos = new BufferedOutputStream(fos);
            do {
                if (mIsCancelled.get()) {
                    Log.d(TAG, "call mCallback.cancel()");
                    if (mCallback != null) {
                        mCallback.cancel(null);
                    }
                    checkCancel();
                    request.abort();
                    return;
                }
                read = is.read(buf);
                if (read < 0) {
                    break;
                }
                bos.write(buf, 0, read);
                write += read;
                mProgressFraction = new ImmutableFraction(write, length); // TODO: too many object created?
                if (mCallback != null) {
                    mCallback.progress(mProgressFraction);
                }
                if (listener != null) {
                    listener.progress(write, length);
                }
            } while (read > 0);

            // It must flush all buffer before invoke complete callback
            bos.flush();
            closeIt(is, bos, fos);

            if (mCallback != null) {
                mCallback.complete(mFile);
            }
            if (listener != null) {
                listener.complete(mFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
            mFile.delete();
            if (mCallback != null) {
                mCallback.error(new ResponseError(null, e));
            }
            if (listener != null) {
                listener.error(e);
            }
        } finally {
            closeIt(is, bos, fos);

            // if task be cancelled, delete file.
            if (mIsCancelled.get()) {
                mFile.delete();
            }
        }
        end();
    }

    private void closeIt(Closeable... c) {
        if (c == null) return;

        for (Closeable aC : c) {
            try {
                if (aC != null) {
                    aC.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkCancel() {
        if (mIsCancelled.get()) {
            if (listener != null) {
                listener.cancel();
            }
            mFile.delete();
            end();
            return true;
        }
        return false;
    }

    public void cancel() {
        mIsCancelled.set(true);
    }

    @Override
    public void callError(final ResponseError error) {
        if (mCallback != null) {
            mCallback.error(error);
        }
    }

    /** Use {@link Listener#progress(long, long)} instead*/
    @Deprecated
    public ImmutableFraction getProgressFraction() {
        return mProgressFraction;
    }

    /** Use {@link Listener} instead*/
    @Deprecated
    public void setCallBack(final Callback callback)
    {
        mCallback = callback;
    }
}
