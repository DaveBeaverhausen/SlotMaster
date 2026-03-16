package com.example.slotmaster.ui.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.slotmaster.R
import com.example.slotmaster.domain.GameEngine
import com.example.slotmaster.data.entity.PartidaEntity
import com.example.slotmaster.database.DatabaseProvider
import kotlin.concurrent.thread

class GameFragment : Fragment(R.layout.fragment_game) {

    private val gameEngine = GameEngine()
    private var coins = 100

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val reel1 = view.findViewById<TextView>(R.id.reel1)
        val reel2 = view.findViewById<TextView>(R.id.reel2)
        val reel3 = view.findViewById<TextView>(R.id.reel3)

        val txtCoins = view.findViewById<TextView>(R.id.txtCoins)
        val btnSpin = view.findViewById<Button>(R.id.btnSpin)

        txtCoins.text = "Coins: $coins"

        btnSpin.setOnClickListener {

            val bet = 10

            if (coins < bet) return@setOnClickListener

            coins -= bet

            val result = gameEngine.spin()

            reel1.text = result[0]
            reel2.text = result[1]
            reel3.text = result[2]

            val reward = gameEngine.calculateReward(result, bet)

            coins += reward

            txtCoins.text = "Coins: $coins"

            saveGame(result)
        }
    }

    private fun saveGame(result: List<String>) {

        val resultadoTexto = result.joinToString(" ")

        val partida = PartidaEntity(
            fecha = System.currentTimeMillis(),
            monedasFinales = coins,
            resultado = resultadoTexto
        )

        thread {

            val db = DatabaseProvider.getDatabase(requireContext())

            db.partidaDao().insert(partida)

        }
    }
}