package com.example.slotmaster.domain.model

import com.google.firebase.Timestamp

data class GameHistory(
        val userId: String = "",
        val username: String = "",
        val symbols: String = "",
        val score: Int = 0,
        val result: String = "",
        val coinsAfter: Int = 0,
        val bet: Int = 0,
        val durationSeconds: Long = 0,
        val errorMessage: String? = null,
        val createdAt: Timestamp? = null
)