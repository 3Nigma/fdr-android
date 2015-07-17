package com.freedomainradio.android.models;

import com.freedomainradio.android.DatePersister;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;
import java.util.List;

@DatabaseTable(tableName = "Podcasts")
public class Podcast {
    @DatabaseField(columnName = "_id", generatedId = true)
    private int id;

    @DatabaseField
    private String podcastId;

    @DatabaseField
    private String title;

    @DatabaseField
    private String description;

    @DatabaseField(columnName = "authorId", foreign = true)
    private Author author;

    @DatabaseField(persisterClass = DatePersister.class)
    private Date date;

    @DatabaseField
    private int length;

    @ForeignCollectionField(eager = true)
        ForeignCollection<Link> links;

    public String getTitle() {
        return title;
    }
    public Date getDate() {return date;}
    public ForeignCollection<Link> getLinks() {
        return links;
    }
}
