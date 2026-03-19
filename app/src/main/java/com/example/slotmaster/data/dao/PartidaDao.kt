package com.example.slotmaster.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.slotmaster.data.entity.PartidaEntity
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable

@Dao
interface PartidaDao {

    @Insert
    fun insert(partida: PartidaEntity): Completable

    @Query("SELECT * FROM partidas ORDER BY id DESC")
    fun getAll(): Flowable<List<PartidaEntity>>
}