package com.example.slotmaster.ui.fragments

import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.slotmaster.R
import com.example.slotmaster.domain.GameEngine
import com.example.slotmaster.data.entity.PartidaEntity
import com.example.slotmaster.database.DatabaseProvider
import kotlin.concurrent.thread
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable

class GameFragment : Fragment(R.layout.fragment_game) {

    private val disposables = CompositeDisposable()
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

                // 🎰 GIRO
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

                // 🎯 PARADA + REBOTE
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

                // ⏱ pequeño delay para mejorar impacto
                Thread.sleep(200)

                val reward = gameEngine.calculateReward(finalResult, bet)
                coins += reward

                activity?.runOnUiThread {

                    txtCoins.text = "Coins: $coins"

                    if (reward > 0) {

                        txtResult.text = "💥 WIN +$reward"
                        txtResult.setTextColor(Color.YELLOW)

                        winAnimation(txtResult)
                        vibrateWin()
                        playWinSound()
                        spawnCelebration(requireView())

                    } else {
                        txtResult.text = "❌ LOSE"
                        txtResult.setTextColor(Color.RED)
                    }

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

    // 🎯 símbolo → imagen
    private fun getImage(symbol: String): Int {
        return when (symbol) {
            "🍒" -> R.drawable.cherry
            "🍋" -> R.drawable.lemon
            "💎" -> R.drawable.diamond
            "7" -> R.drawable.seven
            else -> R.drawable.cherry
        }
    }

    // 🔥 rebote reel
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

    // 💥 animación win potente
    private fun winAnimation(view: TextView) {

        view.scaleX = 0.3f
        view.scaleY = 0.3f
        view.alpha = 0f

        view.animate()
            .scaleX(1.5f)
            .scaleY(1.5f)
            .alpha(1f)
            .setDuration(250)
            .withEndAction {

                view.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(120)
                    .withEndAction {

                        view.animate()
                            .scaleX(1.4f)
                            .scaleY(1.4f)
                            .setDuration(120)
                            .start()
                    }
                    .start()
            }
            .start()
    }

    // 📳 vibración
    private fun vibrateWin() {
        val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            vibrator.vibrate(200)
        }
    }

    // 🔊 sonido win
    private fun playWinSound() {
        val mediaPlayer = MediaPlayer.create(requireContext(), R.raw.win)
        mediaPlayer.start()

        mediaPlayer.setOnCompletionListener {
            it.release()
        }
    }

    // celebración visual
    private fun spawnCelebration(view: View) {

        val container = view.rootView as ViewGroup

        // 💥 explosión inicial (impacto inmediato)
        repeat(10) {
            spawnCoin(container, fast = true)
        }

        // 🎈 lluvia de monedas (efecto prolongado)
        repeat(25) {
            spawnCoin(container, fast = false)
        }
    }
    private fun spawnCoin(container: ViewGroup, fast: Boolean) {

        val coin = ImageView(requireContext())
        coin.setImageResource(R.drawable.coin)

        val size = (40..100).random()

        val params = ViewGroup.LayoutParams(size, size)
        coin.layoutParams = params

        coin.x = (0..container.width).random().toFloat()
        coin.y = container.height.toFloat()

        container.addView(coin)

        val duration = if (fast) {
            (500..900).random().toLong()
        } else {
            (1200..2500).random().toLong()
        }

        coin.animate()
            .translationY(-container.height.toFloat())
            .rotation((0..720).random().toFloat())
            .alpha(0.9f)
            .setDuration(duration)
            .withEndAction {
                container.removeView(coin)
            }
            .start()
    }

    // guardar partida
        private fun saveGame(result: List<String>) {

            val resultadoTexto = result.joinToString(" ")

            val partida = PartidaEntity(
                fecha = System.currentTimeMillis(),
                monedasFinales = coins,
                resultado = resultadoTexto
            )

            val db = DatabaseProvider.getDatabase(requireContext())

            val disposable = db.partidaDao().insert(partida)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { /* OK */ },
                    { error -> error.printStackTrace() }
                )

            disposables.add(disposable)
        }

    override fun onDestroyView() {
        super.onDestroyView()
        disposables.clear()
    }
}
