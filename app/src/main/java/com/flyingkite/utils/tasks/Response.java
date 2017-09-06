package com.cyberlink.actiondirector.networkmanager.tasks;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class Response {

    public interface Listener<Type> {
        void onComplete(Type result);
        void onError(Exception e);
        void onCancelled();
    }

    private final HttpEntity entity;
    protected JSONObject json;
    private Exception error;

    public Response(HttpEntity _entity) {
        entity = _entity;
        String status = null;
        String entityString = null;
        try {
            entityString = EntityUtils.toString(entity);
            json = new JSONObject(entityString);
            Log.i(getClass().getSimpleName(), "json = " + json);
            status = json.getString("status");
        } catch (JSONException|IOException e) {
            json = null;
            error = e;
            e.printStackTrace();
        }

        if (!"ok".equalsIgnoreCase(status)) {
            error = new Exception(entityString != null ? entityString : status);
        }
    }

    public boolean isSuccess() {
        return error == null;
    }

    public Exception getError() {
        return error;
    }
}
