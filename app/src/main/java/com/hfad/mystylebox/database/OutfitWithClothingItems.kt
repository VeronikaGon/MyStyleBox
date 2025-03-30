package com.hfad.mystylebox.database

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class OutfitWithClothingItems(
    @Embedded val outfit: Outfit,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(OutfitClothingItem::class)
    )
    val clothingItems: List<ClothingItem>
)