package com.hfad.mystylebox.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.concurrent.Executors;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;


@Database(entities = {Category.class, Subcategory.class, ClothingItem.class, ClothingItemTag.class, Tag.class}, version = 2)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    public abstract CategoryDao categoryDao();
    public abstract SubcategoryDao subcategoryDao();
    public abstract ClothingItemDao clothingItemDao();
    public abstract TagDao tagDao();
    public abstract ClothingItemTagDao clothingItemTagDao();
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "wardrobe_db")
                            .addCallback(prepopulateCallback)
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static RoomDatabase.Callback prepopulateCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            AppDatabase database = INSTANCE;

            Executors.newSingleThreadExecutor().execute(() -> {
                CategoryDao categoryDao = database.categoryDao();
                SubcategoryDao subcategoryDao = database.subcategoryDao();

                // Вставляем категории
                Category topCategory = new Category("Верх");
                Category bottomCategory = new Category("Низ");
                categoryDao.insert(topCategory);
                categoryDao.insert(bottomCategory);

                // Вставляем подкатегории
                subcategoryDao.insert(new Subcategory(1, "Футболки"));
                subcategoryDao.insert(new Subcategory(1, "Блузки"));
                subcategoryDao.insert(new Subcategory(1, "Рубашки"));
                subcategoryDao.insert(new Subcategory(2, "Брюки"));
                subcategoryDao.insert(new Subcategory(2, "Юбки"));
                subcategoryDao.insert(new Subcategory(2, "Шорты"));
            });
        }
    };
}