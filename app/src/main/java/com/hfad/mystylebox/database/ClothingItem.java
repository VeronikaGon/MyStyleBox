package com.hfad.mystylebox.database;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "clothing_item")
public class ClothingItem {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String gender; // Пол
    public String category; // Категория одежды
    public String name; // Название вещи
}
