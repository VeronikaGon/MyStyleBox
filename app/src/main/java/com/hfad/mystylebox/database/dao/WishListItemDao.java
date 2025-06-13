package com.hfad.mystylebox.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.hfad.mystylebox.database.entity.DailyPlan;
import com.hfad.mystylebox.database.entity.WishListItem;

import java.util.List;

@Dao
public interface WishListItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<WishListItem> list);

    @Query("SELECT id FROM wish_list_item")
    List<Long> getAllIds();

    @Query("SELECT * FROM wish_list_item")
    List<WishListItem> getAllWishListItems();

    @Query("DELETE FROM wish_list_item")
    void deleteAll();

    @Insert
    long insert(WishListItem item);

    @Update
    void update(WishListItem item);

    @Delete
    void delete(WishListItem item);

    @Query("SELECT * FROM wish_list_item")
    List<WishListItem> getAll();

    @Query("SELECT c.name AS categoryName, COUNT(*) AS wishCount FROM wish_list_item wi JOIN subcategories sc ON wi.subcategory_id = sc.id JOIN categories c ON sc.category_id    = c.id GROUP BY c.name")
    List<CategoryCount> getCountByCategory();

    class CategoryCount {
        public String categoryName;
        public int    wishCount;
    }
}