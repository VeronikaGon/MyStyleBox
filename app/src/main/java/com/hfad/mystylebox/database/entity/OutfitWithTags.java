package com.hfad.mystylebox.database.entity;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

public class OutfitWithTags {
    @Embedded
    public Outfit outfit;

    @Relation(
            parentColumn = "id",
            entity = Tag.class,
            entityColumn = "id",
            associateBy = @Junction(
                    value = OutfitTag.class,
                    parentColumn = "outfitId",
                    entityColumn = "tagId"
            )
    )
    public List<Tag> tags;
}