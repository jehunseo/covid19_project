package com.example.covid19_project

import androidx.room.*
import java.util.*

@Dao
interface TagDao{
    @Query("SELECT * from Tag")
    fun getAll() : List<TagEntity>

    @Query("SELECT * FROM Tag WHERE time BETWEEN :from AND :to")
    fun getFromTwoWeeksBefore(from: Date, to: Date): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(roomEntity : TagEntity)
}