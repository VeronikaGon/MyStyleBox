package com.hfad.mystylebox.database;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity(tableName = "clothing_item",
        foreignKeys = @ForeignKey(entity = Subcategory.class,
                parentColumns = "id",
                childColumns = "subcategory_id",
                onDelete = ForeignKey.CASCADE))
public class ClothingItem {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String gender;
    public String imagePath;
    public String name;
    @ColumnInfo(name = "subcategory_id")
    public int subcategoryId;
    public String brend;
    public List<String> seasons;
    public float cost;
    public String status;
    public String size;
    public String notes;

    public ClothingItem(String name, int subcategoryId, String brend, String gender, String imagePath, List<String> seasons, Float cost,String status,String size, String notes) {
        this.imagePath = imagePath;
        this.name = name;
        this.subcategoryId = subcategoryId;
        this.brend = brend;
        this.gender = gender;
        this.seasons = seasons;
        this.cost = cost;
        this.status = status;
        this.size = size;
        this.notes = notes;
    }
}
