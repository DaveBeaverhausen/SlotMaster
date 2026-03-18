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
    private var isSpinning = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val reel1 = view.findViewById<TextView>(R.id.reel1)
        val reel2 = view.findViewById<TextView>(R.id.reel2)
        val reel3 = view.findViewById<TextView>(R.id.reel3)

        val txtCoins = view.findViewById<TextView>(R.id.txtCoins)
        val btnSpin = view.findViewById<Button>(R.id.btnSpin)
        val btnExit = view.findViewById<Button>(R.id.btnExit)

        txtCoins.text = "Coins: $coins"

        btnSpin.setOnClickListener {

            if (isSpinning) return@setOnClickListener

            val bet = 10
            if (coins < bet) return@setOnClickListener

            isSpinning = true
            btnSpin.isEnabled = false

            coins -= bet
            txtCoins.text = "Coins: $coins"

            val finalResult = gameEngine.spin()
            val symbols = listOf("🍒", "⭐", "💎", "7")

            thread {

                // Animación rápida
                repeat(10) {

                    val temp1 = symbols.random()
                    val temp2 = symbols.random()
                    val temp3 = symbols.random()

                    activity?.runOnUiThread {
                        reel1.text = temp1
                        reel2.text = temp2
                        reel3.text = temp3
                    }

                    Thread.sleep(100)
                }

                // Resultado progresivo
                activity?.runOnUiThread { reel1.text = finalResult[0] }
                Thread.sleep(300)

                activity?.runOnUiThread { reel2.text = finalResult[1] }
                Thread.sleep(300)

                activity?.runOnUiThread { reel3.text = finalResult[2] }

                val reward = gameEngine.calculateReward(finalResult, bet)
                coins += reward

                activity?.runOnUiThread {
                    txtCoins.text = "Coins: $coins"
                    btnSpin.isEnabled = true
                    isSpinning = false
                }

                // 💾 Guardar partida
                saveGame(finalResult)
            }
        }

        btnExit.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MenuFragment())
                .commit()
        }
    }

    private fun saveGame(result: List<String>) {

        val resultadoTexto = result.joinToString(" ")

        val partida = PartidaEntity(
            fecha = System.currentTimeMillis(),
            monedasFinales = coins,
            resultado = resultadoTexto
        )

        val db = DatabaseProvider.getDatabase(requireContext())

        thread {
            try {
                db.partidaDao().insert(partida)
                println("PARTIDA GUARDADA: $resultadoTexto")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}