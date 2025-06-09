package com.hfad.mystylebox.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.hfad.mystylebox.database.entity.ClothingItem;
import com.hfad.mystylebox.database.entity.Subcategory;

import java.util.List;

    @Dao
    public interface SubcategoryDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insertAll(List<Subcategory> list);

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        void insert(Subcategory subcategory);

        @Query("SELECT * FROM subcategories WHERE id = :id LIMIT 1")
        Subcategory getSubcategoryById(int id);

        @Query("SELECT * FROM subcategories WHERE category_id = :categoryId")
        List<Subcategory> getSubcategoriesForCategory(int categoryId);

        @Query("SELECT name FROM subcategories WHERE id = :id")
        String getSubcategoryNameById(int id);

        @Query("SELECT * FROM subcategories")
        List<Subcategory> getAllSubcategories();

        @Query("DELETE FROM subcategories")
        void deleteAllSubcategories();
    }