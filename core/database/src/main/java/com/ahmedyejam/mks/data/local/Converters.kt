package com.ahmedyejam.mks.data.local

import android.util.Log
import androidx.room.TypeConverter
import com.ahmedyejam.mks.data.local.entity.QuestionType
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.ParameterizedType

class Converters {
    private val moshi = Moshi.Builder().build()
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val intListType = Types.newParameterizedType(List::class.java, Int::class.javaObjectType)
    private val longListType = Types.newParameterizedType(List::class.java, Long::class.javaObjectType)

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return try {
            moshi.adapter<List<String>>(stringListType).toJson(value ?: emptyList())
        } catch (e: Exception) {
            "[]"
        }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            if (value.startsWith("[")) {
                moshi.adapter<List<String>>(stringListType).fromJson(value) ?: emptyList()
            } else {
                // Handle legacy comma-separated
                value.split(",").map { it.trim() }.filter { it.isNotBlank() }
            }
        } catch (e: Exception) {
            Log.w("Converters", "Failed to parse string list: $value", e)
            emptyList()
        }
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String {
        return try {
            moshi.adapter<List<Int>>(intListType).toJson(value ?: emptyList())
        } catch (e: Exception) {
            "[]"
        }
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            if (value.startsWith("[")) {
                moshi.adapter<List<Int>>(intListType).fromJson(value) ?: emptyList()
            } else {
                value.split(",").mapNotNull { it.trim().toIntOrNull() }
            }
        } catch (e: Exception) {
            Log.w("Converters", "Failed to parse int list: $value", e)
            emptyList()
        }
    }

    @TypeConverter
    fun fromLongList(value: List<Long>?): String {
        return try {
            moshi.adapter<List<Long>>(longListType).toJson(value ?: emptyList())
        } catch (e: Exception) {
            "[]"
        }
    }

    @TypeConverter
    fun toLongList(value: String?): List<Long> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            if (value.startsWith("[")) {
                moshi.adapter<List<Long>>(longListType).fromJson(value) ?: emptyList()
            } else {
                value.split(",").mapNotNull { it.trim().toLongOrNull() }
            }
        } catch (e: Exception) {
            Log.w("Converters", "Failed to parse long list: $value", e)
            emptyList()
        }
    }

    private val listIntType = Types.newParameterizedType(List::class.java, Int::class.javaObjectType)
    private val mapType = Types.newParameterizedType(Map::class.java, Long::class.javaObjectType, listIntType)
    private val mapNotesType = Types.newParameterizedType(Map::class.java, Long::class.javaObjectType, String::class.java)
    private val mapIntType = Types.newParameterizedType(Map::class.java, Long::class.javaObjectType, Int::class.javaObjectType)
    private val mapIntIntType = Types.newParameterizedType(Map::class.java, Int::class.javaObjectType, Int::class.javaObjectType)
    private val mapIntListIntType = Types.newParameterizedType(Map::class.java, Int::class.javaObjectType, listIntType)
    private val mapIntStringType =
        Types.newParameterizedType(Map::class.java, Int::class.javaObjectType, String::class.java)

    @TypeConverter
    fun fromAnswersMap(value: Map<Long, List<Int>>?): String {
        return try {
            moshi.adapter<Map<Long, List<Int>>>(mapType).toJson(value ?: emptyMap())
        } catch (e: Exception) {
            "{}"
        }
    }

    @TypeConverter
    fun toAnswersMap(value: String?): Map<Long, List<Int>> {
        if (value.isNullOrBlank()) return emptyMap()
        return try {
            moshi.adapter<Map<Long, List<Int>>>(mapType).fromJson(value) ?: emptyMap()
        } catch (e: Exception) {
            Log.w("Converters", "Failed to parse answers map: $value", e)
            emptyMap()
        }
    }

    @TypeConverter
    fun fromNotesMap(value: Map<Long, String>?): String {
        return try {
            moshi.adapter<Map<Long, String>>(mapNotesType).toJson(value ?: emptyMap())
        } catch (e: Exception) {
            "{}"
        }
    }

    @TypeConverter
    fun toNotesMap(value: String?): Map<Long, String> {
        if (value.isNullOrBlank()) return emptyMap()
        return try {
            moshi.adapter<Map<Long, String>>(mapNotesType).fromJson(value) ?: emptyMap()
        } catch (e: Exception) {
            Log.w("Converters", "Failed to parse notes map: $value", e)
            emptyMap()
        }
    }

    @TypeConverter
    fun fromIntMap(value: Map<Long, Int>?): String {
        return try {
            moshi.adapter<Map<Long, Int>>(mapIntType).toJson(value ?: emptyMap())
        } catch (e: Exception) {
            "{}"
        }
    }

    @TypeConverter
    fun toIntMap(value: String?): Map<Long, Int> {
        if (value.isNullOrBlank()) return emptyMap()
        return try {
            moshi.adapter<Map<Long, Int>>(mapIntType).fromJson(value) ?: emptyMap()
        } catch (e: Exception) {
            Log.w("Converters", "Failed to parse int map: $value", e)
            emptyMap()
        }
    }

    @TypeConverter
    fun fromIntIntMap(value: Map<Int, Int>?): String {
        return try {
            moshi.adapter<Map<Int, Int>>(mapIntIntType).toJson(value ?: emptyMap())
        } catch (e: Exception) {
            "{}"
        }
    }

    @TypeConverter
    fun toIntIntMap(value: String?): Map<Int, Int> {
        if (value.isNullOrBlank()) return emptyMap()
        return try {
            moshi.adapter<Map<Int, Int>>(mapIntIntType).fromJson(value) ?: emptyMap()
        } catch (e: Exception) {
            Log.w("Converters", "Failed to parse int-int map: $value", e)
            emptyMap()
        }
    }

    @TypeConverter
    fun fromIntListIntMap(value: Map<Int, List<Int>>?): String {
        return try {
            moshi.adapter<Map<Int, List<Int>>>(mapIntListIntType).toJson(value ?: emptyMap())
        } catch (e: Exception) {
            "{}"
        }
    }

    @TypeConverter
    fun toIntListIntMap(value: String?): Map<Int, List<Int>> {
        if (value.isNullOrBlank()) return emptyMap()
        return try {
            moshi.adapter<Map<Int, List<Int>>>(mapIntListIntType).fromJson(value) ?: emptyMap()
        } catch (e: Exception) {
            Log.w("Converters", "Failed to parse int-list-int map: $value", e)
            emptyMap()
        }
    }

    @TypeConverter
    fun fromIntStringMap(value: Map<Int, String>?): String {
        return try {
            moshi.adapter<Map<Int, String>>(mapIntStringType).toJson(value ?: emptyMap())
        } catch (e: Exception) {
            "{}"
        }
    }

    @TypeConverter
    fun toIntStringMap(value: String?): Map<Int, String> {
        if (value.isNullOrBlank()) return emptyMap()
        return try {
            moshi.adapter<Map<Int, String>>(mapIntStringType).fromJson(value) ?: emptyMap()
        } catch (e: Exception) {
            Log.w("Converters", "Failed to parse int-string map: $value", e)
            emptyMap()
        }
    }

    @TypeConverter
    fun fromStringMap(map: Map<String, Boolean>?): String {
        return try {
            moshi.adapter<Map<String, Boolean>>(mapStringType).toJson(map ?: emptyMap())
        } catch (e: Exception) {
            "{}"
        }
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, Boolean> {
        if (value.isNullOrBlank()) return emptyMap()
        return try {
            moshi.adapter<Map<String, Boolean>>(mapStringType).fromJson(value) ?: emptyMap()
        } catch (e: Exception) {
            Log.w("Converters", "Failed to parse string map: $value", e)
            emptyMap()
        }
    }

    private val mapStringType: ParameterizedType =
        Types.newParameterizedType(Map::class.java, String::class.java, java.lang.Boolean::class.javaObjectType)

    @TypeConverter
    fun fromQuestionType(value: QuestionType): String {
        return value.name
    }

    @TypeConverter
    fun toQuestionType(value: String): QuestionType {
        return runCatching { QuestionType.valueOf(value) }
            .getOrDefault(QuestionType.SINGLE_CHOICE)
    }
}
