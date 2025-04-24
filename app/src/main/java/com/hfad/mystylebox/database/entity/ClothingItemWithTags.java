package com.hfad.mystylebox.database.entity;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

public class ClothingItemWithTags {
    @Embedded
    public ClothingItem clothingItem;

    @Relation(
            parentColumn = "id",
            entity = Tag.class,
            entityColumn = "id",
            associateBy = @Junction(
                    value = ClothingItemTag.class,
                    parentColumn = "clothingItemId",
                    entityColumn = "tagId"
            )
    )
    public List<Tag> tags;
}
