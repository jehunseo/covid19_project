package com.example.covid19_project

import androidx.room.*

@Dao
interface TagDao{
    @Query("SELECT * from Tag")
    fun getAll() : List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(roomEntity : TagEntity)
}