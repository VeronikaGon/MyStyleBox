package com.hfad.mystylebox.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.hfad.mystylebox.database.entity.DailyPlan;
import com.hfad.mystylebox.database.entity.Outfit;
import com.hfad.mystylebox.database.entity.PlanImage;

import java.util.List;

@Dao
public interface DailyPlanDao {
    @Insert
    long insert(DailyPlan dailyPlan);

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
}