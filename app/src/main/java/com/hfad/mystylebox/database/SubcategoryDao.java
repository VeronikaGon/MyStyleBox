package com.hfad.mystylebox.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SubcategoryDao {
    @Insert
    void insert(Subcategory subcategory);

    @Query("SELECT * FROM subcategories WHERE id = :id LIMIT 1")
    Subcategory getSubcategoryById(int id);

    @Query("SELECT * FROM subcategories WHERE category_id = :categoryId")
    List<Subcategory> getSubcategoriesForCategory(int categoryId);

    @Query("SELECT * FROM subcategories")
    List<Subcategory> getAllSubcategories();
}