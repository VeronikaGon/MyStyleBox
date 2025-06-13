package com.hfad.mystylebox.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.hfad.mystylebox.database.entity.ClothingItem;
import com.hfad.mystylebox.database.entity.MonthCount;
import com.hfad.mystylebox.database.entity.Outfit;
import com.hfad.mystylebox.database.entity.OutfitClothingItem;
import com.hfad.mystylebox.database.entity.OutfitUsage;
import com.hfad.mystylebox.database.entity.OutfitWithTags;
import com.hfad.mystylebox.database.entity.Tag;
import com.hfad.mystylebox.database.entity.WeekdayCount;

import java.util.List;
@Dao
public interface OutfitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Outfit> list);
    @Insert
    long insertOutfit(Outfit outfit);

    // Вставляем связь комплекта с предметом одежды
    @Insert
    void insertOutfitClothingItem(OutfitClothingItem outfitClothingItem);

    @Query("DELETE FROM outfits")
    void deleteAll();

    @Query("SELECT id FROM outfits")
    List<Long> getAllIds();

    @Update
    void update(Outfit outfit);

    @Transaction
    @Query("SELECT * FROM outfits")
    List<OutfitWithTags> getAllOutfitsWithTags();

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

    @Query("SELECT * FROM outfits WHERE name LIKE :query")
    List<Outfit> searchByName(String query);

    @Query("SELECT * FROM outfits WHERE description LIKE :query")
    List<Outfit> searchByDescription(String query);

    @Query("SELECT * FROM outfits WHERE id IN (:outfitIds)")
    List<Outfit> getOutfitsByIds(List<Integer> outfitIds);

    @Query("SELECT * FROM outfits  WHERE id NOT IN (SELECT DISTINCT outfitId FROM daily_plan WHERE plan_date >= :date )")
    List<Outfit> getStaleOutfitsSince(String date);

    @Query("SELECT COUNT(*) FROM outfits")
    int getCount();
   }