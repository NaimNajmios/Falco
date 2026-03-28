package com.najmi.falco.data.local

import androidx.room.TypeConverter

class FalcoTypeConverters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString("|||")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split("|||")?.filter { it.isNotEmpty() }
    }
}
