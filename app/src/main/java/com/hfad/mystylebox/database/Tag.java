package com.hfad.mystylebox.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tag")
public class Tag {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;

    public Tag(String name) {
        this.name = name;
    }
}
