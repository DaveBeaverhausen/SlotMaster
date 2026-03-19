package com.example.slotmaster.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
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

        val reel1 = view.findViewById<ImageView>(R.id.reel1)
        val reel2 = view.findViewById<ImageView>(R.id.reel2)
        val reel3 = view.findViewById<ImageView>(R.id.reel3)

        val txtCoins = view.findViewById<TextView>(R.id.txtCoins)
        val txtResult = view.findViewById<TextView>(R.id.txtResult)

        val btnSpin = view.findViewById<Button>(R.id.btnSpin)
        val btnExit = view.findViewById<Button>(R.id.btnExit)

        txtCoins.text = "Coins: $coins"

        btnSpin.setOnClickListener {

            if (isSpinning) return@setOnClickListener

            val bet = 10
            if (coins < bet) return@setOnClickListener

            isSpinning = true
            btnSpin.isEnabled = false
            txtResult.text = ""

            coins -= bet
            txtCoins.text = "Coins: $coins"

            val finalResult = gameEngine.spin()
            val symbols = listOf("🍒", "🍋", "💎", "7")

            thread {

                // ANIMACIÓN DE GIRO
                repeat(15) {

                    val temp1 = symbols.random()
                    val temp2 = symbols.random()
                    val temp3 = symbols.random()

                    activity?.runOnUiThread {
                        reel1.setImageResource(getImage(temp1))
                        reel2.setImageResource(getImage(temp2))
                        reel3.setImageResource(getImage(temp3))
                    }

                    Thread.sleep(80)
                }

                // PARADA PROGRESIVA
                activity?.runOnUiThread {
                    reel1.setImageResource(getImage(finalResult[0]))
                    bounceAnimation(reel1)
                }
                Thread.sleep(200)

                activity?.runOnUiThread {
                    reel2.setImageResource(getImage(finalResult[1]))
                    bounceAnimation(reel2)
                }
                Thread.sleep(200)

                activity?.runOnUiThread {
                    reel3.setImageResource(getImage(finalResult[2]))
                    bounceAnimation(reel3)
                }

                val reward = gameEngine.calculateReward(finalResult, bet)
                coins += reward

                activity?.runOnUiThread {

                    txtCoins.text = "Coins: $coins"

                    if (reward > 0) {
                        txtResult.text = "🎉 WIN +$reward"
                        txtResult.setTextColor(Color.GREEN)
                    } else {
                        txtResult.text = "❌ LOSE"
                        txtResult.setTextColor(Color.RED)
                    }

                    // 🎯 animación resultado
                    txtResult.alpha = 0f
                    txtResult.translationY = -150f

                    txtResult.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(400)
                        .start()

                    isSpinning = false
                    btnSpin.isEnabled = true

                    saveGame(finalResult)
                }
            }
        }

        btnExit.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MenuFragment())
                .commit()
        }
    }

    // MAPEO
    private fun getImage(symbol: String): Int {
        return when (symbol) {
            "🍒" -> R.drawable.cherry
            "🍋" -> R.drawable.lemon
            "💎" -> R.drawable.diamond
            "7" -> R.drawable.seven
            else -> R.drawable.cherry
        }
    }

    // ANIMACIÓN REBOTE
    private fun bounceAnimation(view: View) {
        view.animate()
            .translationY(30f)
            .setDuration(80)
            .withEndAction {
                view.animate()
                    .translationY(0f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }

    // GUARDADO
    private fun saveGame(result: List<String>) {

        val resultadoTexto = result.joinToString(" ")

        val partida = PartidaEntity(
            fecha = System.currentTimeMillis(),
            monedasFinales = coins,
            resultado = resultadoTexto
        )

        thread {
            try {
                val db = DatabaseProvider.getDatabase(requireContext())
                db.partidaDao().insert(partida)
                println("PARTIDA GUARDADA: $resultadoTexto")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}