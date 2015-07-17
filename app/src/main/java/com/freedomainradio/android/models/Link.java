package com.freedomainradio.android.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "URLs")
public class Link {
    // These values are mapped to the ids present in the URLTypes table
    public static final int TYPE_AUDIO = 1;
    public static final int TYPE_YOUTUBE = 2;
    public static final int TYPE_VIMEO = 3;
    public static final int TYPE_DAILYMOTION = 4;

    @DatabaseField(columnName = "_id", generatedId = true)
    private int id;

    @DatabaseField(columnName = "value")
    private String value;

    @DatabaseField(columnName = "typeId")
    private int type;

    @DatabaseField(columnName = "podcastId", foreign = true)
    private Podcast podcast;

    public int getType() {
        return type;
    }

    @Override
    public String toString() {
        return value;
    }
}
