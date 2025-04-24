package com.hfad.mystylebox.database.converter

import androidx.room.TypeConverter
import java.util.Arrays

object Converters {
    @TypeConverter
    fun fromList(seasons: List<String?>?): String {
        return if (seasons != null) java.lang.String.join(",", seasons) else ""
    }

    @TypeConverter
    fun toList(data: String): List<String>? {
        return if (data.isEmpty()) null else Arrays.asList(
            *data.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray())
    }
}