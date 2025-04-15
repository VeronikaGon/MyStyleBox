package com.hfad.mystylebox.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

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

    @Query("SELECT * FROM daily_plan ORDER BY plan_date ASC")
    List<DailyPlan> getAllDailyPlansSorted();
}