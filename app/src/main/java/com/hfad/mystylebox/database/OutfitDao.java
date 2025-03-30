package com.hfad.mystylebox.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;
@Dao
public interface OutfitDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Outfit outfit);

    @Query("SELECT * FROM outfits")
    List<Outfit> getAllOutfits();
}
