package com.hfad.mystylebox.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.hfad.mystylebox.database.entity.ClothingItemTag;
import com.hfad.mystylebox.database.entity.Outfit;
import com.hfad.mystylebox.database.entity.OutfitTag;
import com.hfad.mystylebox.database.entity.Tag;

import java.util.List;
@Dao
public interface OutfitTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<OutfitTag> list);

    @Query("SELECT * FROM outfit_tag")
    List<OutfitTag> getAllOutfitTags();

    @Query("DELETE FROM outfit_tag")
    void deleteAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(OutfitTag outfitTag);

    @Query("SELECT Tag.id, Tag.name FROM Tag " +
            "INNER JOIN outfit_tag ON Tag.id = outfit_tag.tagId " +
            "WHERE outfit_tag.outfitId = :outfitId")
    List<Tag> getTagsForOutfit(int outfitId);

    @Query("SELECT COUNT(DISTINCT outfitId) FROM outfit_tag WHERE tagId = :tagId")
    int getCountForTag(int tagId);

    @Query("DELETE FROM outfit_tag WHERE outfitId = :outfitId")
    void deleteTagsForOutfit(int outfitId);
}