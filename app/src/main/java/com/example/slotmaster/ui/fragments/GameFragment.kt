package com.example.slotmaster.ui.fragments

import android.graphics.Color
import android.media.MediaPlayer
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

    private var mediaPlayer: MediaPlayer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val txtCoins = view.findViewById<TextView>(R.id.txtCoins)
        val txtResult = view.findViewById<TextView>(R.id.txtResult)

        val btnSpin = view.findViewById<Button>(R.id.btnSpin)
        val btnExit = view.findViewById<Button>(R.id.btnExit)

        txtCoins.text = "Coins: $coins"

        // BOTÓN GIRAR
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

            // SECUENCIA: coin  spin  animación
            playSound(R.raw.coin) {


                playSound(R.raw.spin)


                startSpinAnimation(view, finalResult, bet)
            }

        }

        // SALIR
        btnExit.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MenuFragment())
                .commit()
        }
    }

    // SISTEMA DE SONIDO PRO
    private fun playSound(soundResId: Int, onComplete: (() -> Unit)? = null) {

        mediaPlayer?.stop()
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer.create(requireContext(), soundResId)
        mediaPlayer?.start()

        mediaPlayer?.setOnCompletionListener {
            it.release()
            mediaPlayer = null
            onComplete?.invoke()
        }
    }

    // ANIMACIÓN DE REELS
    private fun startSpinAnimation(view: View, finalResult: List<String>, bet: Int) {

        val reel1 = view.findViewById<TextView>(R.id.reel1)
        val reel2 = view.findViewById<TextView>(R.id.reel2)
        val reel3 = view.findViewById<TextView>(R.id.reel3)

        val txtCoins = view.findViewById<TextView>(R.id.txtCoins)
        val txtResult = view.findViewById<TextView>(R.id.txtResult)
        val btnSpin = view.findViewById<Button>(R.id.btnSpin)

        val symbols = listOf("🍒", "⭐", "💎", "7")

        thread {

            // animación rápida
            repeat(20) {

                val temp1 = symbols.random()
                val temp2 = symbols.random()
                val temp3 = symbols.random()

                activity?.runOnUiThread {
                    reel1.text = temp1
                    reel2.text = temp2
                    reel3.text = temp3
                }

                Thread.sleep(50)
            }

            // parada progresiva
            activity?.runOnUiThread { reel1.text = finalResult[0] }
            Thread.sleep(200)

            activity?.runOnUiThread { reel2.text = finalResult[1] }
            Thread.sleep(200)

            activity?.runOnUiThread { reel3.text = finalResult[2] }

            val reward = gameEngine.calculateReward(finalResult, bet)
            coins += reward

            activity?.runOnUiThread {

                txtCoins.text = "Coins: $coins"

                if (reward > 0) {
                    txtResult.text = "🎉 WIN +$reward"
                    txtResult.setTextColor(Color.GREEN)
                    playSound(R.raw.win)
                } else {
                    txtResult.text = "❌ LOSE"
                    txtResult.setTextColor(Color.RED)
                    playSound(R.raw.lose)
                }

                // animación resultado
                txtResult.alpha = 0f
                txtResult.translationY = -200f

                txtResult.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .start()

                btnSpin.isEnabled = true
                isSpinning = false
            }

            // guardar partida
            saveGame(finalResult)
        }
    }

    // GUARDAR PARTIDA
    private fun saveGame(result: List<String>) {

        val resultadoTexto = result.joinToString(" ")

        val partida = PartidaEntity(
            fecha = System.currentTimeMillis(),
            monedasFinales = coins,
            resultado = resultadoTexto
        )

        val db = DatabaseProvider.getDatabase(requireContext())

        db.partidaDao().insert(partida)
            .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
            .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
            .subscribe(
                {
                    println("PARTIDA GUARDADA RX")
                },
                {
                    it.printStackTrace()
                }
            )
    }
}
