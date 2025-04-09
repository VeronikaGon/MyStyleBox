package com.hfad.mystylebox.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ClothingItemFull(
    @Embedded val clothingItem: ClothingItem,
    @ColumnInfo(name = "category_name") val categoryName: String = "",
    @ColumnInfo(name = "subcategory_name") val subcategoryName: String = "",
    @Relation(
        parentColumn = "id",
        entity = Tag::class,
        entityColumn = "id",
        associateBy = Junction(
            value = ClothingItemTag::class,
            parentColumn = "clothingItemId",
            entityColumn = "tagId"
        )
    )
    val tags: List<Tag> = emptyList()
)