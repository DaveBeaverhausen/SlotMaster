package com.example.slotmaster.domain

import kotlin.random.Random

class GameEngine {

    private val symbols = listOf("🍒","⭐","🔔","💎","🍋")

    fun spin(): List<String> {

        return listOf(
            symbols.random(),
            symbols.random(),
            symbols.random()
        )

    }

    fun calculateReward(result: List<String>, bet: Int): Int {

        if(result[0] == result[1] && result[1] == result[2]) {
            return bet * 5
        }

        if(result[0] == result[1] || result[1] == result[2] || result[0] == result[2]) {
            return bet * 2
        }

        return 0
    }
}