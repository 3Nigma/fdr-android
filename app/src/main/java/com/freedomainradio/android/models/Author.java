package com.freedomainradio.android.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "Authors")
public class Author {
    @DatabaseField(columnName = "_id", generatedId = true)
    private int id;

    @DatabaseField
    private String name;

    @DatabaseField
    private String email;

    @DatabaseField
    private String website;
}
