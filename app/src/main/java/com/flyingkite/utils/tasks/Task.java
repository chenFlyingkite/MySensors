package com.cyberlink.actiondirector.networkmanager.tasks;

import android.util.Log;

import com.cyberlink.actiondirector.BuildConfig;
import com.cyberlink.actiondirector.networkmanager.Key;
import com.cyberlink.actiondirector.util.NetworkUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Task<T extends Response> implements Runnable {
    private Response.Listener<T> listener;
    @Deprecated
    public static boolean asPDR = true;

    private AtomicBoolean isCancelled = new AtomicBoolean(false);

    public <Subclass extends Task> Subclass listener(Response.Listener<T> listener) {
        this.listener = listener;
        return (Subclass) this;
    }

    protected String getURIString() {
        return null;
    }

    protected Map<String, String> getParameters() {
        Map<String, String> map = new HashMap<>();
        // App name & version
        if (asPDR) {
            map.put(Key.PRODUCT, "PowerDirector Mobile for Android");
            map.put(Key.VERSION_TYPE, "DE");
        } else {
            map.put(Key.PRODUCT, BuildConfig.NOTICE_PRODUCT_NAME);
            map.put(Key.VERSION_TYPE, BuildConfig.NOTICE_VERSION_TYPE);
        }

        map.put(Key.PLATFORM, "Android");
        map.put(Key.VERSION, BuildConfig.NOTICE_VERSION);

        map.put(Key.Init.Parameter.LANGUAGE, Locale.getDefault().toString());
        return map;
    }

    protected abstract T getResponse(HttpEntity entity) throws JSONException;

    @Override
    public void run() {
        if (checkCancel()) return;

        final HttpPost request = new HttpPost(getURIString());

        final List<NameValuePair> params = new ArrayList<>();
        Map<String, String> map = getParameters();
        for (String key : map.keySet()) {
            params.add(new BasicNameValuePair(key, map.get(key)));
        }

        if (checkCancel()) return;

        try {
            request.setEntity(new UrlEncodedFormEntity(params, "utf-8"));

            if (checkCancel()) return;

            long tic, tac;
            tic = System.currentTimeMillis();
            final HttpResponse response = NetworkUtils.getClient().execute(request);
            tac = System.currentTimeMillis();
            Log.i("Task", "request exec " + (tac - tic) + "ms, " + this.getClass().getSimpleName());

            if (checkCancel()) return;

            final HttpEntity entity = response.getEntity();
            if (checkCancel()) return;

            T tResponse = getResponse(entity);
            if (listener != null) {
                if (tResponse.isSuccess()) {
                    listener.onComplete(tResponse);
                } else {
                    listener.onError(tResponse.getError());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (listener != null) {
                listener.onError(e);
            }
        }
    }

    public void cancel() {
        isCancelled.set(true);
    }

    private boolean checkCancel() {
        if (isCancelled.get()) {
            if (listener != null) {
                listener.onCancelled();
            }
            return true;
        }
        return false;
    }
}
