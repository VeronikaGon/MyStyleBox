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

                // Вставляем категории и сохраняем их ID
                long topId = categoryDao.insert(new Category("Верх"));
                long bottomId = categoryDao.insert(new Category("Низ"));
                long dressId = categoryDao.insert(new Category("Платья"));
                long shoesId = categoryDao.insert(new Category("Обувь"));
                long accessoriesId = categoryDao.insert(new Category("Аксессуары"));
                long costumesId = categoryDao.insert(new Category("Костюмы"));
                long overallsId = categoryDao.insert(new Category("Комбинезоны"));
                long bagsId = categoryDao.insert(new Category("Сумки"));
                long outerwearId = categoryDao.insert(new Category("Верхняя одежда"));
                long headwearId = categoryDao.insert(new Category("Головные уборы"));
                long sportswearId = categoryDao.insert(new Category("Спортивная одежда"));
                long beachwearId = categoryDao.insert(new Category("Одежда для пляжа"));


                // Вставляем подкатегории, используя правильные ID категорий
                subcategoryDao.insert(new Subcategory((int)topId, "Футболка"));
                subcategoryDao.insert(new Subcategory((int)topId, "Блузка"));
                subcategoryDao.insert(new Subcategory((int)topId, "Рубашка"));
                subcategoryDao.insert(new Subcategory((int)topId, "Топ"));
                subcategoryDao.insert(new Subcategory((int)topId, "Свитер"));
                subcategoryDao.insert(new Subcategory((int)topId, "Джемпер"));
                subcategoryDao.insert(new Subcategory((int)topId, "Толстовка"));
                subcategoryDao.insert(new Subcategory((int)topId, "Худи"));

                subcategoryDao.insert(new Subcategory((int)bottomId, "Брюки"));
                subcategoryDao.insert(new Subcategory((int)bottomId, "Джинсы"));
                subcategoryDao.insert(new Subcategory((int)bottomId, "Леггинсы"));
                subcategoryDao.insert(new Subcategory((int)bottomId, "Юбка"));
                subcategoryDao.insert(new Subcategory((int)bottomId, "Шорты"));
                subcategoryDao.insert(new Subcategory((int)bottomId, "Классические штаны"));
                subcategoryDao.insert(new Subcategory((int)bottomId, "Спортивные штаны"));

                subcategoryDao.insert(new Subcategory((int)dressId, "Повседневное платье"));
                subcategoryDao.insert(new Subcategory((int)dressId, "Вечернее платье"));
                subcategoryDao.insert(new Subcategory((int)dressId, "Коктейльное платье"));
                subcategoryDao.insert(new Subcategory((int)dressId, "Летнее платье"));
                subcategoryDao.insert(new Subcategory((int)dressId, "Трикотажное платье"));

                subcategoryDao.insert(new Subcategory((int)shoesId, "Кроссовки"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Туфли"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Очки"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Перчатки"));
            });
        }
    };
}