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
public interface ClothingItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ClothingItem item);

    @Update
    void update(ClothingItem item);

    @Delete
    void delete(ClothingItem item);

    // Получение элементов с тегами
    @Transaction
    @Query("SELECT * FROM clothing_item")
    List<ClothingItemWithTags> getClothingItemsWithTags();

    // Получение всех элементов с категориями
    @Transaction
    @Query("SELECT clothing_item.*, " +
            "categories.name AS category_name, " +
            "subcategories.name AS subcategory_name " +
            "FROM clothing_item " +
            "INNER JOIN subcategories ON clothing_item.subcategory_id = subcategories.id " +
            "INNER JOIN categories ON subcategories.category_id = categories.id")
    List<ClothingItemWithCategory> getAllItemsWithCategories();

    @Transaction
    @Query("SELECT * FROM clothing_item WHERE name LIKE :query")
    List<ClothingItemWithTags> searchByNameWithTags(String query);

    @Transaction
    @Query("SELECT * FROM clothing_item WHERE notes LIKE :query")
    List<ClothingItemWithTags> searchByDescriptionWithTags(String query);

    @Transaction
    @Query("SELECT clothing_item.*, categories.name AS category_name, subcategories.name AS subcategory_name " +
            "FROM clothing_item " +
            "INNER JOIN subcategories ON clothing_item.subcategory_id = subcategories.id " +
            "INNER JOIN categories ON subcategories.category_id = categories.id")
    List<ClothingItemFull> getAllItemsFull();

    @Transaction
    @Query("SELECT clothing_item.*, categories.name AS category_name, subcategories.name AS subcategory_name " +
            "FROM clothing_item " +
            "INNER JOIN subcategories ON clothing_item.subcategory_id = subcategories.id " +
            "INNER JOIN categories ON subcategories.category_id = categories.id " +
            "WHERE clothing_item.name LIKE :query")
    List<ClothingItemFull> searchByNameWithFull(String query);

    @Transaction
    @Query("SELECT clothing_item.*, categories.name AS category_name, subcategories.name AS subcategory_name " +
            "FROM clothing_item " +
            "INNER JOIN subcategories ON clothing_item.subcategory_id = subcategories.id " +
            "INNER JOIN categories ON subcategories.category_id = categories.id " +
            "WHERE clothing_item.notes LIKE :query")
    List<ClothingItemFull> searchByDescriptionWithFull(String query);

}