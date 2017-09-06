package com.cyberlink.actiondirector.networkmanager.tasks.colorpreset;

import com.google.gson.annotations.SerializedName;

public class PresetItem {
    @SerializedName("tid")
    public long tid;

    @SerializedName("guid")
    public String guid;

    @SerializedName("type")
    public String type;

    @SerializedName("name")
    public String name;

    @SerializedName("thumbnail")
    public String thumbnail;

    @SerializedName("downloadurl")
    public String downloadurl;

    @SerializedName("downloadchecksum")
    public String downloadchecksum;

    @SerializedName("publishdate")
    public String publishdate; // yyyy/MM/dd

    @SerializedName("collagetype")
    public String collagetype;

    @SerializedName("collagelayout")
    public String collagelayout;

    @SerializedName("expireddate")
    public String expireddate; // yyyy/MM/dd

    @SerializedName("downloadFileSize")
    public long downloadFileSize;

    @SerializedName("promotionEndDate")
    public String promotionEndDate;

    @SerializedName("support_type")
    public String[] support_type;

    @SerializedName("usage_type")
    public String usage_type;

    @SerializedName("items")
    public Item[] items;

    @SerializedName("purchaseId")
    public String purchaseId;

    @SerializedName("promotion")
    public Promotion promotion;

    public static class Item {
        @SerializedName("itemGUID")
        public String itemGUID;

        @SerializedName("title")
        public String title;

        @SerializedName("description")
        public String description;

        @SerializedName("note")
        public String note;

        @SerializedName("thumbnailURL")
        public String thumbnailURL;

        @Override
        public String toString() {
            return "("
                    + "guid = " + itemGUID
                    + " ,title = " + title
                    + " ,note = " + note
                    + " ,desc = " + description
                    + " ,thumb = " + thumbnailURL
                    + " )";
        }
    }
    public static class Promotion {
        @SerializedName("iconURL")
        public String iconURL;
    }
}
