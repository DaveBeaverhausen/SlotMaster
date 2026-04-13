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
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlin.concurrent.thread

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

        val db = DatabaseProvider.getDatabase(requireContext())

        // cargar monedas guardadas
        val disposable = db.partidaDao().getLast()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ partida ->
                coins = partida.monedasFinales
                txtCoins.text = "Coins: $coins"
            }, {})

        disposables.add(disposable)

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

                Thread.sleep(200)

                val reward = gameEngine.calculateReward(finalResult, bet)
                val oldCoins = coins
                coins += reward

                activity?.runOnUiThread {

                    animateCoins(txtCoins, oldCoins, coins)

                    if (reward >= 50) {

                        showJackpot(txtResult, reward)
                        vibrateJackpot()
                        playJackpotSound()
                        spawnJackpot(requireView())

                    } else if (reward > 0) {

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

    private fun getImage(symbol: String): Int {
        return when (symbol) {
            "🍒" -> R.drawable.cherry
            "🍋" -> R.drawable.lemon
            "💎" -> R.drawable.diamond
            "7" -> R.drawable.seven
            else -> R.drawable.cherry
        }
    }

    private fun bounceAnimation(view: View) {
        view.animate().translationY(30f).setDuration(80)
            .withEndAction {
                view.animate().translationY(0f).setDuration(120).start()
            }.start()
    }

    private fun animateCoins(textView: TextView, from: Int, to: Int) {
        val steps = 20
        val diff = to - from
        val increment = if (steps != 0) diff / steps else diff

        thread {
            var current = from
            repeat(steps) {
                current += increment
                activity?.runOnUiThread {
                    textView.text = "Coins: $current"
                }
                Thread.sleep(30)
            }
            activity?.runOnUiThread {
                textView.text = "Coins: $to"
            }
        }
    }

    private fun winAnimation(view: TextView) {
        view.scaleX = 0.3f
        view.scaleY = 0.3f
        view.alpha = 0f

        view.animate().scaleX(1.5f).scaleY(1.5f).alpha(1f).setDuration(250)
            .withEndAction {
                view.animate().scaleX(1.2f).scaleY(1.2f).setDuration(120).start()
            }.start()
    }

    private fun spawnCelebration(view: View) {
        val container = requireActivity().window.decorView as ViewGroup
        repeat(25) { spawnCoin(container, false) }
    }

    private fun spawnCoin(container: ViewGroup, fast: Boolean) {
        val coin = ImageView(requireContext())
        coin.setImageResource(R.drawable.coin)

        val size = (40..100).random()
        coin.layoutParams = ViewGroup.LayoutParams(size, size)

        coin.x = (0..container.width).random().toFloat()
        coin.y = container.height.toFloat()

        container.addView(coin)

        coin.animate()
            .translationY(-container.height.toFloat())
            .rotation((0..720).random().toFloat())
            .setDuration(if (fast) 800 else 1500)
            .withEndAction { container.removeView(coin) }
            .start()
    }

    private fun spawnJackpot(view: View) {
        val container = requireActivity().window.decorView as ViewGroup

        repeat(80) {
            val coin = ImageView(requireContext())
            coin.setImageResource(R.drawable.coin)

            val size = (50..120).random()
            coin.layoutParams = ViewGroup.LayoutParams(size, size)

            coin.x = (0..container.width).random().toFloat()
            coin.y = -100f

            container.addView(coin)

            coin.animate()
                .translationY(container.height.toFloat())
                .rotation((0..1080).random().toFloat())
                .setDuration((600..1200).random().toLong())
                .withEndAction { container.removeView(coin) }
                .start()
        }
    }

    private fun showJackpot(textView: TextView, reward: Int) {
        textView.text = "💰 JACKPOT +$reward"
        textView.setTextColor(Color.YELLOW)

        textView.scaleX = 0.2f
        textView.scaleY = 0.2f

        textView.animate().scaleX(2f).scaleY(2f).setDuration(400)
            .withEndAction {
                textView.animate().scaleX(1.5f).scaleY(1.5f).setDuration(200).start()
            }.start()
    }

    private fun playWinSound() {
        MediaPlayer.create(requireContext(), R.raw.win).start()
    }

    private fun playJackpotSound() {
        MediaPlayer.create(requireContext(), R.raw.jackpot).start()
    }

    private fun vibrateWin() {
        val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(200)
    }

    private fun vibrateJackpot() {
        val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(longArrayOf(0, 100, 50, 200, 50, 300), -1)
    }

    private fun saveGame(result: List<String>) {
        val partida = PartidaEntity(
            fecha = System.currentTimeMillis(),
            monedasFinales = coins,
            resultado = result.joinToString(" ")
        )

        val db = DatabaseProvider.getDatabase(requireContext())

        val disposable = db.partidaDao().insert(partida)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({}, { it.printStackTrace() })

        disposables.add(disposable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposables.clear()
    }
}