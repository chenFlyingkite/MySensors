package com.cyberlink.actiondirector.networkmanager.tasks.colorpreset;

import com.cyberlink.actiondirector.networkmanager.NetworkManager;
import com.cyberlink.actiondirector.networkmanager.tasks.Task;

import org.apache.http.HttpEntity;
import org.json.JSONException;

import java.util.Map;

public class PresetPackTask extends Task<PresetPackResponse> {
    public static final String VERSION = "1.0";

    @Override
    protected String getURIString() {
        return NetworkManager.getUri_EffectTree();
        //return "http://apptest.cyberlink.com/service/V2/effect/getTree";
    }

    @Override
    protected Map<String, String> getParameters() {
        Map<String, String> map = super.getParameters();
        map.put("contentVer", VERSION);
        map.put("categoryId", "-1");
        return map;
    }

    @Override
    protected PresetPackResponse getResponse(HttpEntity entity) throws JSONException {
        return new PresetPackResponse(entity);
    }
}
