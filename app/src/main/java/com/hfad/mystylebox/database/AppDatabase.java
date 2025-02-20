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
                subcategoryDao.insert(new Subcategory((int)topId, "Водолазка"));
                subcategoryDao.insert(new Subcategory((int)topId, "Майка"));
                subcategoryDao.insert(new Subcategory((int)topId, "Боди"));
                subcategoryDao.insert(new Subcategory((int)topId, "Лонгслив"));
                subcategoryDao.insert(new Subcategory((int)topId, "Поло"));
                subcategoryDao.insert(new Subcategory((int)topId, "Кофта"));
                subcategoryDao.insert(new Subcategory((int)topId, "Жилет"));
                subcategoryDao.insert(new Subcategory((int)topId, "Пиджак"));
                subcategoryDao.insert(new Subcategory((int)topId, "Жакет"));
                subcategoryDao.insert(new Subcategory((int)topId, "Кардиган"));
                subcategoryDao.insert(new Subcategory((int)topId, "Туника"));
                subcategoryDao.insert(new Subcategory((int)topId, "Пуловер"));
                subcategoryDao.insert(new Subcategory((int)topId, "Свитшот"));
                subcategoryDao.insert(new Subcategory((int)topId, "Блейзер"));

                subcategoryDao.insert(new Subcategory((int)bottomId, "Брюки"));
                subcategoryDao.insert(new Subcategory((int)bottomId, "Джинсы"));
                subcategoryDao.insert(new Subcategory((int)bottomId, "Леггинсы"));
                subcategoryDao.insert(new Subcategory((int)bottomId, "Юбка"));
                subcategoryDao.insert(new Subcategory((int)bottomId, "Шорты"));
                subcategoryDao.insert(new Subcategory((int)bottomId, "Классические штаны"));
                subcategoryDao.insert(new Subcategory((int)bottomId, "Велосипедки"));
                subcategoryDao.insert(new Subcategory((int)bottomId, "Бриджи"));
                subcategoryDao.insert(new Subcategory((int)bottomId, "Слаксы"));
                subcategoryDao.insert(new Subcategory((int)bottomId, "Чинос"));
                subcategoryDao.insert(new Subcategory((int)bottomId, "Бермуды"));
                subcategoryDao.insert(new Subcategory((int)bottomId, "Брюки классические"));
                subcategoryDao.insert(new Subcategory((int)bottomId, "Брюки льняные"));
                subcategoryDao.insert(new Subcategory((int)bottomId, "Брюки укороченные"));
                subcategoryDao.insert(new Subcategory((int)bottomId, "Брюки парадные"));

                subcategoryDao.insert(new Subcategory((int)dressId, "Повседневное платье"));
                subcategoryDao.insert(new Subcategory((int)dressId, "Официальное платье"));
                subcategoryDao.insert(new Subcategory((int)dressId, "Вечернее платье"));
                subcategoryDao.insert(new Subcategory((int)dressId, "Коктейльное платье"));
                subcategoryDao.insert(new Subcategory((int)dressId, "Летнее платье"));
                subcategoryDao.insert(new Subcategory((int)dressId, "Трикотажное платье"));
                subcategoryDao.insert(new Subcategory((int)dressId, "Сарафан"));
                subcategoryDao.insert(new Subcategory((int)dressId, "Платье макси"));
                subcategoryDao.insert(new Subcategory((int)dressId, "Ромпер"));
                subcategoryDao.insert(new Subcategory((int)dressId, "Курта"));
                subcategoryDao.insert(new Subcategory((int)dressId, "Платье-комбинация"));
                subcategoryDao.insert(new Subcategory((int)dressId, "Платье с запахом"));
                subcategoryDao.insert(new Subcategory((int)dressId, "Платье-футляр"));
                subcategoryDao.insert(new Subcategory((int)dressId, "Платье до колена"));
                subcategoryDao.insert(new Subcategory((int)dressId, "Платье джинсовое"));

                subcategoryDao.insert(new Subcategory((int)shoesId, "Кроссовки"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Кеды"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Туфли"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Ботинки"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Босоножки"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Сандалии"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Сапоги"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Лоферы"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Мюли"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Шлёпанцы"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Ботильоны"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Туфли-лодочки"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Балетки"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Сникеры"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Полуботинки"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Полусапоги"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Ботфорты"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Обувь рабочая"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Слиперы"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Мокасины"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Сланцы"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Слипоны"));
                subcategoryDao.insert(new Subcategory((int)shoesId, "Дерби"));

                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Очки"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Перчатки"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Серьги"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Ремень"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Колье"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Браслет"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Шарф"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Кольцо"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Очки солнцезащитные"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Часы"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Цепочка, цепь"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Брошь"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Платок, шаль"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Пояс"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Подвеска"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Кулон"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Чокер"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Повязка, лента"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Галстук"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Бусы"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Заколка"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Ожерелье"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Запонки"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Галстук-бабочка"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Кашне"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Подтяжки"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Портмоне, бумажник"));
                subcategoryDao.insert(new Subcategory((int)accessoriesId, "Другое"));

                subcategoryDao.insert(new Subcategory((int)costumesId, "Костюм брючный"));
                subcategoryDao.insert(new Subcategory((int)costumesId, "Комплект с юбкой"));
                subcategoryDao.insert(new Subcategory((int)costumesId, "Традиционный наряд"));
                subcategoryDao.insert(new Subcategory((int)costumesId, "Комплект из двух вещей"));
                subcategoryDao.insert(new Subcategory((int)costumesId, "Костюм деловой"));
                subcategoryDao.insert(new Subcategory((int)costumesId, "Костюм юбочный"));
                subcategoryDao.insert(new Subcategory((int)costumesId, "Co-ord комплект"));
                subcategoryDao.insert(new Subcategory((int)costumesId, "Костюм вязаный"));
                subcategoryDao.insert(new Subcategory((int)costumesId, "Костюм трикотажный"));
                subcategoryDao.insert(new Subcategory((int)costumesId, "Кэтсьют"));
                subcategoryDao.insert(new Subcategory((int)costumesId, "Форма школьная"));
                subcategoryDao.insert(new Subcategory((int)costumesId, "Твинсет"));
                subcategoryDao.insert(new Subcategory((int)costumesId, "Униформа"));
                subcategoryDao.insert(new Subcategory((int)costumesId, "Костюм – тройка"));
                subcategoryDao.insert(new Subcategory((int)costumesId, "Фрак"));
                subcategoryDao.insert(new Subcategory((int)costumesId, "Костюм торжественный"));
                subcategoryDao.insert(new Subcategory((int)costumesId, "Костюм карнавальный"));
                subcategoryDao.insert(new Subcategory((int)costumesId, "Камербанд"));

                subcategoryDao.insert(new Subcategory((int)overallsId, "Комбинезон"));
                subcategoryDao.insert(new Subcategory((int)overallsId, "Комбинезон летний"));
                subcategoryDao.insert(new Subcategory((int)overallsId, "Комбинезон вечерний"));
                subcategoryDao.insert(new Subcategory((int)overallsId, "Комбинезон рабочий"));
                subcategoryDao.insert(new Subcategory((int)overallsId, "Комбинезон брючный"));
                subcategoryDao.insert(new Subcategory((int)overallsId, "Комбинезон джинсовый"));
                subcategoryDao.insert(new Subcategory((int)overallsId, "Полукомбинезон"));
                subcategoryDao.insert(new Subcategory((int)overallsId, "Комбинезон - юбка"));
                subcategoryDao.insert(new Subcategory((int)overallsId, "Комбинезон без рукавов"));
                subcategoryDao.insert(new Subcategory((int)overallsId, "Комбинезон с открытой спиной"));
                subcategoryDao.insert(new Subcategory((int)overallsId, "Комбинезон кожанный"));
                subcategoryDao.insert(new Subcategory((int)overallsId, "Комбинезон с капюшоном"));

                subcategoryDao.insert(new Subcategory((int)bagsId, "Сумка"));
                subcategoryDao.insert(new Subcategory((int)bagsId, "Рюкзак"));
                subcategoryDao.insert(new Subcategory((int)bagsId, "Сумка кросс боди"));
                subcategoryDao.insert(new Subcategory((int)bagsId, "Сумка-тоут"));
                subcategoryDao.insert(new Subcategory((int)bagsId, "Клатч"));
                subcategoryDao.insert(new Subcategory((int)bagsId, "Борсетка"));
                subcategoryDao.insert(new Subcategory((int)bagsId, "Сумка-багет"));
                subcategoryDao.insert(new Subcategory((int)bagsId, "Сумка хобо"));
                subcategoryDao.insert(new Subcategory((int)bagsId, "Сумка-кошелек"));
                subcategoryDao.insert(new Subcategory((int)bagsId, "Шоппер"));
                subcategoryDao.insert(new Subcategory((int)bagsId, "Поясная сумка"));
                subcategoryDao.insert(new Subcategory((int)bagsId, "Сумка через плечо"));
                subcategoryDao.insert(new Subcategory((int)bagsId, "Дорожная сумка"));
                subcategoryDao.insert(new Subcategory((int)bagsId, "Портфель"));
                subcategoryDao.insert(new Subcategory((int)bagsId, "Рюкзак школьный"));
                subcategoryDao.insert(new Subcategory((int)bagsId, "Сумка для ноута"));

                subcategoryDao.insert(new Subcategory((int)outerwearId, "Пальто"));
                subcategoryDao.insert(new Subcategory((int)outerwearId, "Куртка"));
                subcategoryDao.insert(new Subcategory((int)outerwearId, "Пуховик"));
                subcategoryDao.insert(new Subcategory((int)outerwearId, "Парка"));
                subcategoryDao.insert(new Subcategory((int)outerwearId, "Плащ"));
                subcategoryDao.insert(new Subcategory((int)outerwearId, "Шуба"));
                subcategoryDao.insert(new Subcategory((int)outerwearId, "Ветровка"));
                subcategoryDao.insert(new Subcategory((int)outerwearId, "Бомбер"));
                subcategoryDao.insert(new Subcategory((int)outerwearId, "Куртка кожаная"));
                subcategoryDao.insert(new Subcategory((int)outerwearId, "Тренчкот(Тренч)"));
                subcategoryDao.insert(new Subcategory((int)outerwearId, "Косуха"));
                subcategoryDao.insert(new Subcategory((int)outerwearId, "Накидка"));
                subcategoryDao.insert(new Subcategory((int)outerwearId, "Дождевик"));
                subcategoryDao.insert(new Subcategory((int)outerwearId, "Полупальто"));
                subcategoryDao.insert(new Subcategory((int)outerwearId, "Дубленка"));
                subcategoryDao.insert(new Subcategory((int)outerwearId, "Кейп"));

                subcategoryDao.insert(new Subcategory((int)headwearId, "Шапка"));
                subcategoryDao.insert(new Subcategory((int)headwearId, "Кепка"));
                subcategoryDao.insert(new Subcategory((int)headwearId, "Панама"));
                subcategoryDao.insert(new Subcategory((int)headwearId, "Берет"));
                subcategoryDao.insert(new Subcategory((int)headwearId, "Фетровая шляпа"));
                subcategoryDao.insert(new Subcategory((int)headwearId, "Шляпа"));
                subcategoryDao.insert(new Subcategory((int)headwearId, "Бейсболка"));
                subcategoryDao.insert(new Subcategory((int)headwearId, "Бандана"));
                subcategoryDao.insert(new Subcategory((int)headwearId, "Косынка"));
                subcategoryDao.insert(new Subcategory((int)headwearId, "Хиджаб"));
                subcategoryDao.insert(new Subcategory((int)headwearId, "Маска"));
                subcategoryDao.insert(new Subcategory((int)headwearId, "Наушники теплые"));
                subcategoryDao.insert(new Subcategory((int)headwearId, "Кепи"));
                subcategoryDao.insert(new Subcategory((int)headwearId, "Капор"));
                subcategoryDao.insert(new Subcategory((int)headwearId, "Фуражка"));
                subcategoryDao.insert(new Subcategory((int)headwearId, "Шляпа-котелок"));

                subcategoryDao.insert(new Subcategory((int)sportswearId, "Леггинсы спортивные"));
                subcategoryDao.insert(new Subcategory((int)sportswearId, "Спортивные брюки"));
                subcategoryDao.insert(new Subcategory((int)sportswearId, "Футболка спортивная"));
                subcategoryDao.insert(new Subcategory((int)sportswearId, "Майка спортивная"));
                subcategoryDao.insert(new Subcategory((int)sportswearId, "Олимпийка"));
                subcategoryDao.insert(new Subcategory((int)sportswearId, "Спортивные костюмы"));
                subcategoryDao.insert(new Subcategory((int)sportswearId, "Спортивные штаны"));
                subcategoryDao.insert(new Subcategory((int)sportswearId, "Куртка спортивная"));
                subcategoryDao.insert(new Subcategory((int)sportswearId, "Кроссовки для фитнеса"));
                subcategoryDao.insert(new Subcategory((int)sportswearId, "Ботинки спортивные"));
                subcategoryDao.insert(new Subcategory((int)sportswearId, "Лонгслив тренировочный"));
                subcategoryDao.insert(new Subcategory((int)sportswearId, "Топ спортивный"));
                subcategoryDao.insert(new Subcategory((int)sportswearId, "Юбка теннисная"));
                subcategoryDao.insert(new Subcategory((int)sportswearId, "Кроссовки беговые"));
                subcategoryDao.insert(new Subcategory((int)sportswearId, "Йога-штаны"));
                subcategoryDao.insert(new Subcategory((int)sportswearId, "Костюм для черлидинга"));

                subcategoryDao.insert(new Subcategory((int)beachwearId, "Костюм для черлидинга"));
            });
        }
    };
}