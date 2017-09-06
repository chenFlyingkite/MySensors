package com.cyberlink.actiondirector.networkmanager.tasks.colorpreset;

import com.google.gson.annotations.SerializedName;

public class PresetPack {
    @SerializedName("categoryId")
    public long categoryId;

    @SerializedName("effects")
    public Effects[] effects;

    @SerializedName("subCategoryList")
    public PresetPack[] subCategoryList;

    @SerializedName("lastModified")
    public long lastModified;

    @Override
    public String toString() {
        return "("
                + "id = " + categoryId
                + ", lastMod = " + lastModified
                + ", effects = " + (effects == null ? 0 : effects.length)
                + ", subCategoryList = " + (subCategoryList == null ? 0 : subCategoryList.length)
                + ")";
    }

    public static class Effects {
        @SerializedName("tid")
        public long tid;

        @SerializedName("purchaseId")
        public String purchaseId;

        @SerializedName("lastModified")
        public long lastModified;
    }
}
