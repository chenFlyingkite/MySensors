package com.cyberlink.actiondirector.networkmanager.tasks.colorpreset;

import com.cyberlink.actiondirector.networkmanager.tasks.Response;
import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PresetPackResponse extends Response {
    private PresetPack pack;

    public PresetPackResponse(HttpEntity _entity) throws JSONException {
        super(_entity);
        if (isSuccess()) {
            JSONArray root = json.getJSONArray("effectTree");
            if (root != null) {
                JSONObject tree = root.getJSONObject(0);
                pack = new Gson().fromJson(tree.toString(), PresetPack.class);
            }
        }
    }

    public PresetPack getPack() {
        return pack;
    }
}
