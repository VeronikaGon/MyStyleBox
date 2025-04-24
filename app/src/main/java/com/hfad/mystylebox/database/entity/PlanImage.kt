package com.hfad.mystylebox.database.entity

import androidx.room.ColumnInfo

data class PlanImage(
    @ColumnInfo(name = "plan_date") val date: String,
    @ColumnInfo(name = "imagePath") val path: String
)