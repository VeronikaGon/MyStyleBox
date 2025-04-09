package com.hfad.mystylebox.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface OutfitClothingItemDao {
    @Insert
    fun insert(outfitClothingItem: OutfitClothingItem): Long

    @Query("DELETE FROM outfit_clothing_item WHERE outfitId = :outfitId")
    fun deleteForOutfit(outfitId: Int)
}