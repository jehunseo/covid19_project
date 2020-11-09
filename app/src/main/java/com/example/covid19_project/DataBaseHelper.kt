package com.example.covid19_project

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DataBaseHelper(context : Context, name : String, version : Int) :
    SQLiteOpenHelper(context, name, null, version){
    override fun onCreate(db: SQLiteDatabase?) {
        val create = "create table memo(" +
                "name string," +
                "MAC string, " +
                "bluetoothRssi short "+
                ")"

        db?.execSQL(create)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }

    fun insertMemo(memo : Memo){
        val values = ContentValues()
        values.put("name", memo.name)
        values.put("MAC", memo.MAC)
        values.put("bluetoothRssi", memo.bluetoothRssi)

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
            val name = cursor.getString(cursor.getColumnIndex(("name")))
            val MAC = cursor.getString(cursor.getColumnIndex("MAC"))
            val bluetoothRssi = cursor.getShort(cursor.getColumnIndex("bluetoothRssi"))

            list.add(Memo(name, MAC, bluetoothRssi))
        }

        cursor.close()
        rd.close()
        return list
    }

    fun updateMemo(memo : Memo){
        val values = ContentValues()
        values.put("name", memo.name)
        values.put("MAC", memo.MAC)
        values.put("bluetoothRssi", memo.bluetoothRssi)

        val wd = writableDatabase
        wd.update("memo", values, "MAC = ${memo.MAC}", null)
        wd.close()
    }

    fun deleteMemo(memo : Memo){
        val delete = "delete from memo where MAC = ${memo.MAC}"
        val db = writableDatabase
        db.execSQL(delete)
        db.close()
    }
}
data class Memo(var name : String, var MAC : String, var bluetoothRssi : Short)
