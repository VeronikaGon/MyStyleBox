package com.hfad.mystylebox.database;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

public class ClothingItemWithTags {
    @Embedded
    public ClothingItem clothingItem;

    @Relation(
            parentColumn = "id", // из ClothingItem
            entity = Tag.class,
            entityColumn = "id", // из Tag
            associateBy = @Junction(
                    value = ClothingItemTag.class,
                    parentColumn = "clothingItemId", // столбец в ClothingItemTag, ссылающийся на ClothingItem
                    entityColumn = "tagId"          // столбец в ClothingItemTag, ссылающийся на Tag
            )
    )
    public List<Tag> tags;
}
