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

    @Query("SELECT Tag.id, Tag.name FROM Tag " +
            "INNER JOIN outfit_tag ON Tag.id = outfit_tag.tagId " +
            "WHERE outfit_tag.outfitId = :outfitId")
    List<Tag> getTagsForOutfit(int outfitId);

    @Query("DELETE FROM outfit_tag WHERE outfitId = :outfitId")
    void deleteTagsForOutfit(int outfitId);
}