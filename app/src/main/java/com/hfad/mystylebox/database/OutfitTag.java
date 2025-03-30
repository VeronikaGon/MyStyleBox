package com.hfad.mystylebox.database;

import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(
        tableName = "outfit_tag",
        primaryKeys = {"outfitId", "tagId"},
        foreignKeys = {
                @ForeignKey(entity = Outfit.class,
                        parentColumns = "id",
                        childColumns = "outfitId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Tag.class,
                        parentColumns = "id",
                        childColumns = "tagId",
                        onDelete = ForeignKey.CASCADE)
        }
)
public class OutfitTag {
    public int outfitId;
    public int tagId;

    public OutfitTag(int outfitId, int tagId) {
        this.outfitId = outfitId;
        this.tagId = tagId;
    }
}
