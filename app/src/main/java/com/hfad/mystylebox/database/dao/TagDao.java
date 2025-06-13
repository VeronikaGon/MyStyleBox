package com.hfad.mystylebox.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.hfad.mystylebox.database.entity.Subcategory;
import com.hfad.mystylebox.database.entity.Tag;

import java.util.List;

@Dao
public interface TagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Tag> list);

    @Query("SELECT id FROM tag")
    List<Long> getAllIds();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Tag tag);

    @Query("SELECT * FROM tag")
    List<Tag> getAllTags();

    @Delete
    void delete(Tag tag);

    @Query("DELETE FROM tag")
    void deleteAll();

    @Query("SELECT * FROM tag WHERE name = :name LIMIT 1")
    Tag getTagByName(String name);
}