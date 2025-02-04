package com.hfad.mystylebox.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ClothingItemDao {
    @Insert
    void insert(ClothingItem item);

    @Query("SELECT * FROM clothing_item")
    List<ClothingItem> getAllItems();
}
