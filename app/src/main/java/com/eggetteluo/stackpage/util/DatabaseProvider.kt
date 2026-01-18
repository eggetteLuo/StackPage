package com.eggetteluo.stackpage.util

import android.content.Context
import androidx.room.Room
import com.eggetteluo.stackpage.data.entity.AppDatabase

object DatabaseProvider {

    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "stackpage_db"
            ).build()
            instance = db
            db
        }
    }

}