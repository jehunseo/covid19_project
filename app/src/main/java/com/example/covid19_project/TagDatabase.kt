package com.example.covid19_project

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TagEntity::class], version = 1)
abstract class TagDatabase : RoomDatabase(){
    abstract fun getTagDao() : TagDao
    companion object{

        private var INSTANCE : TagDatabase? = null

        fun getInstance(context : Context) : TagDatabase?{
            if(INSTANCE == null){
                synchronized(TagDatabase::class){
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        TagDatabase::class.java,
                        "Tag.db")
                        .build()
                }
            }
            return INSTANCE
        }
    }
}