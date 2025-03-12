package com.hfad.mystylebox.database;

import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(
        tableName = "clothing_item_tag",
        primaryKeys = {"clothingItemId", "tagId"},
        foreignKeys = {
                @ForeignKey(entity = ClothingItem.class,
                        parentColumns = "id",
                        childColumns = "clothingItemId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Tag.class,
                        parentColumns = "id",
                        childColumns = "tagId",
                        onDelete = ForeignKey.CASCADE)
        }
)
public class ClothingItemTag {
    public int clothingItemId; // Должно совпадать с childColumns
    public int tagId;          // Должно совпадать с childColumns

    public ClothingItemTag(int clothingItemId, int tagId) {
        this.clothingItemId = clothingItemId;
        this.tagId = tagId;
    }
}