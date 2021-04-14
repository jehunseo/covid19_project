package com.example.covid19_project

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Tag")
class TagEntity (@PrimaryKey val tag_main : String,
                 val tag_sub : String,
                 val time : String)