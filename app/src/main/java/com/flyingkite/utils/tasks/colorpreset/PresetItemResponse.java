package com.cyberlink.actiondirector.networkmanager.tasks.colorpreset;

import com.cyberlink.actiondirector.networkmanager.tasks.Response;
import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PresetItemResponse extends Response {
    private PresetItem item;

    public PresetItemResponse(HttpEntity _entity) throws JSONException {
        super(_entity);
        if (isSuccess()) {
            JSONArray root = json.getJSONArray("templates");
            if (root != null) {
                JSONObject tree = root.getJSONObject(0);
                item = new Gson().fromJson(tree.toString(), PresetItem.class);
            }
        }
    }

    public PresetItem getItem() {
        return item;
    }
}
