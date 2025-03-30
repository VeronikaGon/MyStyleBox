package com.hfad.mystylebox.database;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

public class OutfitWithTags {
    @Embedded
    public ClothingItem clothingItem;

    @Relation(
            parentColumn = "id",
            entity = Tag.class,
            entityColumn = "id",
            associateBy = @Junction(
                    value = Outfit.class,
                    parentColumn = "outfitId",
                    entityColumn = "tagId"
            )
    )
    public List<Tag> tags;
}