package com.hfad.mystylebox.database;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity(tableName = "clothing_item")
public class ClothingItem {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String gender; // Пол
    public String category; // Категория одежды
    public String name; // Название вещи
    public String imagePath; // Путь к файлу изображения
    public List<String> seasons; //сезоны

    public ClothingItem(String name, String category, String gender, String imagePath, List<String> seasons) {
        this.name = name;
        this.category = category;
        this.gender = gender;
        this.imagePath = imagePath;
        this.seasons = seasons;
    }
}
