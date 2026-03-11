package com.example.slotmaster.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "partidas")
data class PartidaEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val fecha: Long,

    val monedasFinales: Int,

    val resultado: String
)