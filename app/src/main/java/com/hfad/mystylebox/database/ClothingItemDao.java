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
    // Вставка нового элемента
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ClothingItem item);

    // Обновление существующего элемента
    @Update
    void update(ClothingItem item);

    @Delete
    void delete(ClothingItem item);

    // Удаление элемента по ID
    @Query("DELETE FROM clothing_item WHERE id = :id")
    void deleteById(int id);

    // Получение всех элементов
    @Query("SELECT * FROM clothing_item")
    List<ClothingItem> getAllItems();

    // Получение элементов с тегами
    @Transaction
    @Query("SELECT * FROM clothing_item")
    List<ClothingItemWithTags> getClothingItemsWithTags();

    // Поиск по имени
    @Query("SELECT * FROM clothing_item WHERE name LIKE :query")
    List<ClothingItem> searchByName(String query);

    // Поиск по описанию
    @Query("SELECT * FROM clothing_item WHERE notes LIKE :query")
    List<ClothingItem> searchByDescription(String query);

    // Получение всех элементов с категориями
    @Transaction
    @Query("SELECT clothing_item.*, " +
            "categories.name AS category_name, " +
            "subcategories.name AS subcategory_name " +
            "FROM clothing_item " +
            "INNER JOIN subcategories ON clothing_item.subcategory_id = subcategories.id " +
            "INNER JOIN categories ON subcategories.category_id = categories.id")
    List<ClothingItemWithCategory> getAllItemsWithCategories();

    // Поиск по имени с категориями
    @Transaction
    @Query("SELECT clothing_item.*, " +
            "categories.name AS category_name, " +
            "subcategories.name AS subcategory_name " +
            "FROM clothing_item " +
            "INNER JOIN subcategories ON clothing_item.subcategory_id = subcategories.id " +
            "INNER JOIN categories ON subcategories.category_id = categories.id " +
            "WHERE clothing_item.name LIKE :query")
    List<ClothingItemWithCategory> searchByNameWithCategories(String query);

    // Поиск по описанию с категориями
    @Transaction
    @Query("SELECT clothing_item.*, " +
            "categories.name AS category_name, " +
            "subcategories.name AS subcategory_name " +
            "FROM clothing_item " +
            "INNER JOIN subcategories ON clothing_item.subcategory_id = subcategories.id " +
            "INNER JOIN categories ON subcategories.category_id = categories.id " +
            "WHERE clothing_item.notes LIKE :query")
    List<ClothingItemWithCategory> searchByDescriptionWithCategories(String query);
}

