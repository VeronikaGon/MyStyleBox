package com.hfad.mystylebox.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;

    //конструктор класса
    public Category(String name) {
        this.name = name;
    }
}
