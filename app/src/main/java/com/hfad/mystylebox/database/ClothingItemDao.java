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

    // Получение элементов с тегами (если это необходимо)
    @Transaction
    @Query("SELECT * FROM clothing_item")
    List<ClothingItemWithTags> getClothingItemsWithTags();
}

