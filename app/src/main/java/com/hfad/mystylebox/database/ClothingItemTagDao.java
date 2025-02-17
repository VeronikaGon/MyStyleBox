package com.hfad.mystylebox.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ClothingItemTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ClothingItemTag clothingItemTag);

    @Query("SELECT tag.* FROM tag INNER JOIN clothing_item_tag ON tag.id = clothing_item_tag.tagId WHERE clothing_item_tag.clothingItemId = :clothingItemId")
    List<Tag> getTagsForClothingItem(int clothingItemId);
}