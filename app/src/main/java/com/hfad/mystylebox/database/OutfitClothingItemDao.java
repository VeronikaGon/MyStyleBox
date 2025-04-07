package com.hfad.mystylebox.database;

import androidx.room.Dao;
import androidx.room.Insert;

@Dao
public interface OutfitClothingItemDao {
    @Insert
    long insert(OutfitClothingItem outfitClothingItem);
}
