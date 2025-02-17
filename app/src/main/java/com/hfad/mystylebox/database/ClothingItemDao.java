package com.hfad.mystylebox.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface ClothingItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ClothingItem item);

    @Query("SELECT * FROM clothing_item")
    List<ClothingItem> getAllItems();

    @Transaction
    @Query("SELECT * FROM clothing_item")
    List<ClothingItemWithTags> getClothingItemsWithTags();
}
