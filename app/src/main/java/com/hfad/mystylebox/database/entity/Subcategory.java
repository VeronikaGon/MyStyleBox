package com.hfad.mystylebox.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "subcategories",
        foreignKeys = @ForeignKey(entity = Category.class,
                parentColumns = "id",
                childColumns = "category_id",
                onDelete = ForeignKey.CASCADE))
public class Subcategory {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo(name = "category_id")
    public int categoryId;
    public String name;

    public Subcategory(int categoryId, String name) {
        this.categoryId = categoryId;
        this.name = name;
    }
}