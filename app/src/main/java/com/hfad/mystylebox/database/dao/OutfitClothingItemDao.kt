package com.hfad.mystylebox.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hfad.mystylebox.database.entity.OutfitClothingItem

@Dao
interface OutfitClothingItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(list: List<OutfitClothingItem>)

    @Query("SELECT * FROM outfit_clothing_item")
    fun getAllOutfitClothingItems(): List<OutfitClothingItem>

    @Insert
    fun insert(outfitClothingItem: OutfitClothingItem): Long

    @Query("DELETE FROM outfit_clothing_item WHERE outfitId = :outfitId")
    fun deleteForOutfit(outfitId: Int)

    @Query("DELETE FROM daily_plan")
    fun deleteAll();

    @Query("SELECT * FROM outfit_clothing_item WHERE outfitId = :outfitId")
    fun getItemsForOutfit(outfitId: Int): List<OutfitClothingItem>
}