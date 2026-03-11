package com.example.slotmaster.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.slotmaster.data.dao.PartidaDao
import com.example.slotmaster.data.entity.PartidaEntity

@Database(entities = [PartidaEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun partidaDao(): PartidaDao

}