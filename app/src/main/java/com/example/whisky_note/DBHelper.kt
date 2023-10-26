package com.example.whisky_note

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 2
        private const val DATABASE_NAME = "whisky_database.db"
        const val TABLE_NAME = "whisky_table"
        const val COLUMN_ID = "id"
        const val COLUMN_REVIEW = "review"
        const val COLUMN_NAME = "name"
        const val COLUMN_NOSE = "nose"
        const val COLUMN_PALATE = "palate"
        const val COLUMN_FINISH = "finish"
        const val COLUMN_RATING = "rating"
        const val COLUMN_IMAGE_PATH = "image_path"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_TABLE = "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_REVIEW TEXT," +
                "$COLUMN_NAME TEXT," +
                "$COLUMN_NOSE TEXT," +
                "$COLUMN_PALATE TEXT," +
                "$COLUMN_FINISH TEXT," +
                "$COLUMN_RATING REAL," +
                "$COLUMN_IMAGE_PATH TEXT" + ")"
        db.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }
}