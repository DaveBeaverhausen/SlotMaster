package com.example.slotmaster.ui.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Vibrator
import android.provider.MediaStore
import android.content.ContentValues
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.slotmaster.R
import com.example.slotmaster.domain.GameEngine
import com.example.slotmaster.data.entity.PartidaEntity
import com.example.slotmaster.database.DatabaseProvider
import com.example.slotmaster.ui.notifications.NotificationHelper
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
        val btnReset = view.findViewById<Button>(R.id.btnResetCoins)

        txtCoins.text = "Coins: $coins"

        // 🔥 RECARGAR MONEDAS
        btnReset.setOnClickListener {
            coins = 100
            txtCoins.text = "Coins: $coins"
        }

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
                    activity?.runOnUiThread {
                        reel1.setImageResource(getImage(symbols.random()))
                        reel2.setImageResource(getImage(symbols.random()))
                        reel3.setImageResource(getImage(symbols.random()))
                    }
                    Thread.sleep(80)
                }

                val reward = gameEngine.calculateReward(finalResult, bet)
                coins += reward

                activity?.runOnUiThread {

                    reel1.setImageResource(getImage(finalResult[0]))
                    reel2.setImageResource(getImage(finalResult[1]))
                    reel3.setImageResource(getImage(finalResult[2]))

                    txtCoins.text = "Coins: $coins"

                    if (reward >= 50) {

                        txtResult.text = "💰 JACKPOT +$reward"
                        txtResult.setTextColor(Color.YELLOW)

                        playSound(R.raw.jackpot)
                        spawnJackpot()

                        // 🔔 NOTIFICACIÓN
                        NotificationHelper.showWinNotification(requireContext(), reward)

                        AlertDialog.Builder(requireContext())
                            .setTitle("¡Victoria!")
                            .setMessage("¿Quieres guardar una captura?")
                            .setPositiveButton("Sí") { _, _ ->
                                captureScreenAndSave()
                            }
                            .setNegativeButton("No", null)
                            .show()

                    } else if (reward > 0) {

                        txtResult.text = "💥 WIN +$reward"
                        txtResult.setTextColor(Color.YELLOW)

                        playSound(R.raw.win)
                        spawnCelebration()

                        // 🔔 NOTIFICACIÓN
                        NotificationHelper.showWinNotification(requireContext(), reward)

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

    private fun playSound(sound: Int) {
        MediaPlayer.create(requireContext(), sound).start()
    }

    private fun spawnCelebration() {
        val container = requireActivity().window.decorView as ViewGroup
        repeat(20) { spawnCoin(container) }
    }

    private fun spawnJackpot() {
        val container = requireActivity().window.decorView as ViewGroup
        repeat(60) { spawnCoin(container) }
    }

    private fun spawnCoin(container: ViewGroup) {
        val coin = ImageView(requireContext())
        coin.setImageResource(R.drawable.coin)

        val size = (40..100).random()
        coin.layoutParams = ViewGroup.LayoutParams(size, size)

        coin.x = (0..container.width).random().toFloat()
        coin.y = -100f

        container.addView(coin)

        coin.animate()
            .translationY(container.height.toFloat())
            .rotation((0..720).random().toFloat())
            .setDuration((600..1500).random().toLong())
            .withEndAction { container.removeView(coin) }
            .start()
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

    private fun captureScreenAndSave() {

        val view = requireActivity().window.decorView.rootView

        view.post {

            val bitmap = Bitmap.createBitmap(
                view.width,
                view.height,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(bitmap)
            view.draw(canvas)

            saveImageToGallery(bitmap)
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap) {

        val filename = "SlotMaster_${System.currentTimeMillis()}.png"

        val resolver = requireContext().contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/SlotMaster")
        }

        val imageUri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        if (imageUri != null) {
            val stream = resolver.openOutputStream(imageUri)

            if (stream != null) {
                stream.use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposables.clear()
    }
}