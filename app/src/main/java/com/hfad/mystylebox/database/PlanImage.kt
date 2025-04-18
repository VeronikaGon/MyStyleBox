package com.hfad.mystylebox.database

import androidx.room.ColumnInfo

data class PlanImage(
    @ColumnInfo(name = "plan_date") val date: String,
    @ColumnInfo(name = "imagePath") val path: String
)