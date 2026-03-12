package com.example.slotmaster.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.slotmaster.data.entity.PartidaEntity

@Dao
interface PartidaDao {

    @Insert
    fun insert(partida: PartidaEntity)

    @Query("SELECT * FROM partidas ORDER BY id DESC")
    fun getAll(): List<PartidaEntity>
}