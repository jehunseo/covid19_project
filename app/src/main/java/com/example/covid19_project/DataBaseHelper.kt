package com.example.covid19_project

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DataBaseHelper(context: Context, name: String, version: Int) :

    SQLiteOpenHelper(context, name, null, version){

    override fun onCreate(db: SQLiteDatabase?) {
        val create = "create table memo(" +
                "tag_main string," +
                "tag_sub string," +
                "tag_time string"+
                ")"

        db?.execSQL(create)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }

    fun insertMemo(memo : Memo){
        val values = ContentValues()
        values.put("tag_main", memo.tag_main)
        values.put("tag_sub", memo.tag_sub)
        values.put("tag_time", memo.tag_time)

        val wd = writableDatabase
        wd.insert("memo", null, values)
        wd.close()
    }

    fun selectMemo() : MutableList<Memo>{
        val list = mutableListOf<Memo>()

        val select = "select * from memo"
        val rd = readableDatabase
        val cursor = rd.rawQuery(select, null)

        while(cursor.moveToNext()){
            val tag_main = cursor.getString(cursor.getColumnIndex("tag_main"))
            val tag_sub = cursor.getString(cursor.getColumnIndex("tag_sub"))
            val tag_time = cursor.getString(cursor.getColumnIndex("tag_time"))

            list.add(Memo(tag_main, tag_sub, tag_time))
        }

        cursor.close()
        rd.close()
        return list
    }

    fun updateMemo(memo : Memo){
        val values = ContentValues()
        values.put("tag_main", memo.tag_main)
        values.put("tag_sub", memo.tag_sub)
        values.put("tag_time", memo.tag_time)

        val wd = writableDatabase
        wd.update("memo", values, "tag_main = ${memo.tag_main}", null)
        wd.close()
    }

    fun deleteMemo(memo : Memo){
        val delete = "delete from memo where tag_main = ${memo.tag_main}"
        val db = writableDatabase
        db.execSQL(delete)
        db.close()
    }
}
data class Memo(var tag_main : String, var tag_sub : String, var tag_time : String)
