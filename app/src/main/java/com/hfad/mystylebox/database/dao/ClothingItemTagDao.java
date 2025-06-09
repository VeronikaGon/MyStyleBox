package com.hfad.mystylebox.database.dao;



import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.hfad.mystylebox.database.entity.ClothingItemTag;
import com.hfad.mystylebox.database.entity.Tag;

import java.util.List;


@Dao
public interface ClothingItemTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ClothingItemTag> list);

    @Query("SELECT * FROM clothing_item_tag")
    List<ClothingItemTag> getAllClothingItemTags();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ClothingItemTag clothingItemTag);

    @Query("SELECT tag.* FROM tag INNER JOIN clothing_item_tag ON tag.id = clothing_item_tag.tagId WHERE clothing_item_tag.clothingItemId = :clothingItemId")
    List<Tag> getTagsForClothingItem(int clothingItemId);

    @Query("SELECT COUNT(*) FROM clothing_item_tag WHERE tagId = :tagId")
    int getCountForTag(int tagId);

    @Query("DELETE FROM clothing_item_tag")
    void deleteAll();

    @Query("DELETE FROM clothing_item_tag WHERE clothingItemId = :clothingItemId")
    void deleteTagsForClothingItem(int clothingItemId);
}