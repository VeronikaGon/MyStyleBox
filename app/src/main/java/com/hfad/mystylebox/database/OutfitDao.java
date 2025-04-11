package com.hfad.mystylebox.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;
@Dao
public interface OutfitDao {
    @Insert
    long insertOutfit(Outfit outfit);

    // Вставляем связь комплекта с предметом одежды
    @Insert
    void insertOutfitClothingItem(OutfitClothingItem outfitClothingItem);

    @Update
    void update(Outfit outfit);

    // Вставка комплекта вместе со списком связанных предметов
    @Transaction
    default void insertOutfitWithItems(Outfit outfit, List<OutfitClothingItem> items) {
        long outfitId = insertOutfit(outfit);
        // Обновляем поле outfitId в каждой связи
        for (OutfitClothingItem item : items) {
            item.outfitId = (int) outfitId;
            insertOutfitClothingItem(item);
        }
    }
    @Query("SELECT * FROM outfits")
    List<Outfit> getAllOutfits();

    @Query("SELECT c.* FROM clothing_item c " +
            "INNER JOIN outfit_clothing_item oci ON c.id = oci.clothingItemId " +
            "WHERE oci.outfitId = :outfitId")
    List<ClothingItem> getClothingItemsForOutfit(int outfitId);
    @Delete
    void delete(Outfit outfit);
    @Query("SELECT o.* FROM outfits o INNER JOIN outfit_clothing_item oci ON o.id = oci.outfitId WHERE oci.clothingItemId = :clothingItemId")
    List<Outfit> getOutfitsForClothingItem(int clothingItemId);
}