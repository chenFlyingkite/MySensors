package com.cyberlink.actiondirector.networkmanager.tasks.colorpreset;

import com.cyberlink.actiondirector.networkmanager.NetworkManager;
import com.cyberlink.actiondirector.networkmanager.tasks.Task;

import org.apache.http.HttpEntity;
import org.json.JSONException;

import java.util.Map;

public class PresetItemTask extends Task<PresetItemResponse> {
    private long tid;

    public PresetItemTask tid(long tid) {
        this.tid = tid;
        return this;
    }

    @Override
    protected String getURIString() {
        return NetworkManager.getUri_TemplateByIdsWithAPP();
        //return "http://apptest.cyberlink.com/service/V2/effect/getTree";
    }

    @Override
    protected Map<String, String> getParameters() {
        Map<String, String> map = super.getParameters();
        map.put("tids", "" + tid);
        return map;
    }

    @Override
    protected PresetItemResponse getResponse(HttpEntity entity) throws JSONException {
        return new PresetItemResponse(entity);
    }
}
