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

        @Database(entities = {Category.class, Subcategory.class, ClothingItem.class, ClothingItemTag.class, Tag.class, Outfit.class, OutfitClothingItem.class,OutfitTag.class}, version = 5, exportSchema = true)
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
            private static volatile AppDatabase INSTANCE;
            private static Context appContext;

            static final Migration MIGRATION_2_3 = new Migration(2, 3) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase database) {

                }
            };

            public static AppDatabase getInstance(Context context) {
                if (INSTANCE == null) {
                    synchronized (AppDatabase.class) {
                        if (INSTANCE == null) {
                            appContext = context.getApplicationContext();
                            INSTANCE = Room.databaseBuilder(appContext, AppDatabase.class, "wardrobe_db")
                                    .addMigrations(MIGRATION_2_3)
                                    .allowMainThreadQueries()
                                    .fallbackToDestructiveMigration()
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