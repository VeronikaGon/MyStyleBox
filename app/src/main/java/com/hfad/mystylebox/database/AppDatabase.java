package com.hfad.mystylebox.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.concurrent.Executors;
import androidx.room.migration.Migration;

@Database(entities = {Category.class, Subcategory.class, ClothingItem.class, ClothingItemTag.class, Tag.class, Outfit.class, OutfitClothingItem.class, OutfitTag.class, DailyPlan.class}, version = 6, exportSchema = true)
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
    private static volatile AppDatabase INSTANCE;
    private static Context appContext;

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
        }
    };
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_plan` (" +
                            " `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            " `plan_date` TEXT, " +
                            " `outfitId` INTEGER NOT NULL, " +
                            " FOREIGN KEY(`outfitId`) REFERENCES `outfits`(`id`) ON DELETE CASCADE" +      ")"
            );
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_daily_plan_outfitId` ON `daily_plan` (`outfitId`)"
            );
        }
    };
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    appContext = context.getApplicationContext();
                    INSTANCE = Room.databaseBuilder(appContext, AppDatabase.class, "wardrobe_db")
                            .addMigrations(MIGRATION_5_6)
                            .allowMainThreadQueries()
                            .addCallback(prepopulateCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final Callback prepopulateCallback = new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            Executors.newSingleThreadExecutor().execute(() -> {
                final Context context = appContext;
                if (context == null) {
                    Log.e("AppDatabase", "Context is null!");
                    return;
                }
            });
        }
    };
}