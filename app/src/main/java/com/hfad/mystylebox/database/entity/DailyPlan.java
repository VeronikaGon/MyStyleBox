package com.hfad.mystylebox.database.entity;

import static androidx.room.ForeignKey.CASCADE;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "daily_plan",
        foreignKeys = @ForeignKey(entity = Outfit.class,
                parentColumns = "id",
                childColumns = "outfitId",
                onDelete = CASCADE),
        indices = {@Index("outfitId")})
public class DailyPlan {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "plan_date")
    private String planDate;

    @ColumnInfo(name = "outfitId")
    private int outfitId;

    public DailyPlan(String planDate, int outfitId) {
        this.planDate = planDate;
        this.outfitId = outfitId;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getPlanDate() {
        return planDate;
    }
    public void setPlanDate(String planDate) {
        this.planDate = planDate;
    }
    public int getOutfitId() {
        return outfitId;
    }
    public void setOutfitId(int outfitId) {
        this.outfitId = outfitId;
    }
}