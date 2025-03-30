package com.hfad.mystylebox.database;

import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(
        tableName = "outfit_clothing_item",
        primaryKeys = {"clothingItemId", "outfitId"},
foreignKeys = {
@ForeignKey(entity = ClothingItem.class,
        parentColumns = "id",
        childColumns = "clothingItemId",
        onDelete = ForeignKey.CASCADE),
@ForeignKey(entity = Outfit.class,
        parentColumns = "id",
        childColumns = "outfitId",
        onDelete = ForeignKey.CASCADE)
        }
                )
public class OutfitClothingItem {
    public int outfitId;
    public int clothingItemId;

    public OutfitClothingItem(int clothingItemId, int outfitId) {
        this.clothingItemId = clothingItemId;
        this.outfitId = outfitId;
    }
}
