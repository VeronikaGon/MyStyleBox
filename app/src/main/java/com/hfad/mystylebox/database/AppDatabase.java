package com.hfad.mystylebox.database;

import android.content.Context;
import android.util.Log;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.migration.Migration;

import com.hfad.mystylebox.database.dao.CategoryDao;
import com.hfad.mystylebox.database.dao.SubcategoryDao;
import com.hfad.mystylebox.database.dao.ClothingItemDao;
import com.hfad.mystylebox.database.dao.TagDao;
import com.hfad.mystylebox.database.dao.OutfitDao;
import com.hfad.mystylebox.database.dao.ClothingItemTagDao;
import com.hfad.mystylebox.database.dao.OutfitTagDao;
import com.hfad.mystylebox.database.dao.OutfitClothingItemDao;
import com.hfad.mystylebox.database.dao.DailyPlanDao;
import com.hfad.mystylebox.database.dao.WishListItemDao;
import com.hfad.mystylebox.database.entity.Category;
import com.hfad.mystylebox.database.entity.Subcategory;
import com.hfad.mystylebox.database.entity.ClothingItem;
import com.hfad.mystylebox.database.entity.ClothingItemTag;
import com.hfad.mystylebox.database.entity.Tag;
import com.hfad.mystylebox.database.entity.Outfit;
import com.hfad.mystylebox.database.entity.OutfitClothingItem;
import com.hfad.mystylebox.database.entity.OutfitTag;
import com.hfad.mystylebox.database.entity.DailyPlan;
import com.hfad.mystylebox.database.entity.WishListItem;

import com.hfad.mystylebox.database.converter.Converters;

@Database(
        entities = {
                Category.class,
                Subcategory.class,
                ClothingItem.class,
                ClothingItemTag.class,
                Tag.class,
                Outfit.class,
                OutfitClothingItem.class,
                OutfitTag.class,
                DailyPlan.class,
                WishListItem.class
        },
        version = 7,
        exportSchema = true
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    public abstract CategoryDao categoryDao();
    public abstract SubcategoryDao subcategoryDao();
    public abstract ClothingItemDao clothingItemDao();
    public abstract TagDao tagDao();
    public abstract OutfitDao outfitDao();
    public abstract ClothingItemTagDao clothingItemTagDao();
    public abstract OutfitTagDao outfitTagDao();
    public abstract OutfitClothingItemDao outfitClothingItemDao();
    public abstract DailyPlanDao dailyPlanDao();

    public abstract WishListItemDao wishListItemDao();

    private static volatile AppDatabase INSTANCE;
    private static Context appContext;

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) { }
    };
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_plan` (" +
                            " `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            " `plan_date` TEXT, " +
                            " `outfitId` INTEGER NOT NULL, " +
                            " FOREIGN KEY(`outfitId`) REFERENCES `outfits`(`id`) ON DELETE CASCADE" +
                            ")"
            );
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_daily_plan_outfitId` ON `daily_plan`(`outfitId`)"
            );
        }
    };

    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Просто создаём новую таблицу по вашей Entity
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `wish_list_item` (" +
                            " `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            " `image_path` TEXT, " +
                            " `name` TEXT, " +
                            " `price` REAL NOT NULL, " +
                            " `notes` TEXT, " +
                            " `size` TEXT, " +
                            " `subcategory_id` INTEGER NOT NULL, " +
                            " `gender` TEXT, " +
                            " FOREIGN KEY(`subcategory_id`) " +
                            "   REFERENCES `subcategories`(`id`) ON DELETE CASCADE" +
                            ")"
            );
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                            "`index_wish_list_item_subcategory_id` " +
                            "ON `wish_list_item`(`subcategory_id`)"
            );
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    appContext = context.getApplicationContext();
                    INSTANCE = Room.databaseBuilder(appContext, AppDatabase.class, "wardrobe_db")
                            .addMigrations(MIGRATION_2_3, MIGRATION_5_6, MIGRATION_6_7)
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}