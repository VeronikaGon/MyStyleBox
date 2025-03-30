package com.hfad.mystylebox.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;
@Dao
public interface OutfitTagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(OutfitTag outfitTag);

    @Query("SELECT * FROM outfit_tag WHERE outfitId = :outfitId")
    List<OutfitTag> getTagsForOutfit(int outfitId);
}