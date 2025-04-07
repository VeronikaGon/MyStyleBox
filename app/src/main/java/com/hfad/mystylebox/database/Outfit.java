package com.hfad.mystylebox.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity(tableName = "outfits")
public class Outfit {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @NonNull
    public String name;
    public List<String> seasons;
    public String description;
    public int minTemp;
    public int maxTemp;
    public String imagePath;
}