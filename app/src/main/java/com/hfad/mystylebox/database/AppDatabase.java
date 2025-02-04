package com.hfad.mystylebox.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {ClothingItem.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ClothingItemDao clothingItemDao();
}