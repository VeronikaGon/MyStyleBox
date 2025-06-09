package com.hfad.mystylebox.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.hfad.mystylebox.database.entity.Category;

import java.util.List;

@Dao
public interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Category category);

    @Query("SELECT * FROM categories")
    List<Category> getAllCategories();

    @Query("DELETE FROM categories")
    void deleteAllCategories();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Category> list);
}