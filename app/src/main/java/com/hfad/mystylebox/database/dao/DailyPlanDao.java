package com.hfad.mystylebox.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.hfad.mystylebox.database.entity.ClothingItemTag;
import com.hfad.mystylebox.database.entity.DailyPlan;
import com.hfad.mystylebox.database.entity.MonthCount;
import com.hfad.mystylebox.database.entity.Outfit;
import com.hfad.mystylebox.database.entity.OutfitUsage;
import com.hfad.mystylebox.database.entity.PlanImage;
import com.hfad.mystylebox.database.entity.WeekdayCount;

import java.util.List;

@Dao
public interface DailyPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DailyPlan> list);

    @Query("SELECT id FROM daily_plan")
    List<Long> getAllIds();

    @Insert
    long insert(DailyPlan dailyPlan);

    @Query("DELETE FROM daily_plan")
    void deleteAll();

    @Update
    void update(DailyPlan dailyPlan);

    @Delete
    void delete(DailyPlan dailyPlan);

    @Query("SELECT * FROM daily_plan")
    List<DailyPlan> getAllDailyPlans();

    @Query("SELECT * FROM daily_plan WHERE plan_date = :date")
    List<DailyPlan> getDailyPlansForDate(String date);

    @Query("SELECT o.*   FROM outfits o INNER JOIN daily_plan dp ON dp.outfitId = o.id  WHERE dp.plan_date = :date")
    List<Outfit> getOutfitsByDate(String date);

    @Query(" SELECT dp.plan_date AS plan_date, o.imagePath   AS imagePath FROM daily_plan dp INNER JOIN outfits o ON dp.outfitId = o.id ")
    List<PlanImage> getAllPlanImages();

    @Query("DELETE FROM daily_plan WHERE plan_date = :date AND outfitId = :outfitId")
    void deleteByDateAndOutfitId(String date, Long outfitId);

    @Query("SELECT COUNT(*) FROM daily_plan")
    int getTotalDaysPlanned();

    @Query("SELECT strftime('%w', plan_date) as weekday, COUNT(*) as cnt FROM daily_plan GROUP BY weekday ")
    List<WeekdayCount> getCountByWeekday();

    @Query("SELECT outfitId, COUNT(*) as cnt FROM daily_plan GROUP BY outfitId ORDER BY cnt DESC LIMIT :limit ")
    List<OutfitUsage> getMostFrequent(int limit);

    @Query("SELECT outfitId, COUNT(*) as cnt FROM daily_plan GROUP BY outfitId ORDER BY cnt ASC LIMIT :limit ")
    List<OutfitUsage>  getLeastFrequent(int limit);

    @Query("SELECT strftime('%m', plan_date) as month, COUNT(*) as cnt FROM daily_plan GROUP BY month ORDER BY month ")
    List<MonthCount> getCountByMonth();
}