package com.example.slotmaster.ui.fragments

import android.app.*
import android.content.*
import android.graphics.*
import android.media.MediaPlayer
import android.os.*
import android.provider.MediaStore
import android.view.*
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.example.slotmaster.R
import com.example.slotmaster.domain.GameEngine
import com.example.slotmaster.data.entity.PartidaEntity
import com.example.slotmaster.database.DatabaseProvider
import com.example.slotmaster.ui.MainActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlin.concurrent.thread

class GameFragment : Fragment(R.layout.fragment_game) {

    private val disposables = CompositeDisposable()
    private val gameEngine = GameEngine()
    private var coins = 100
    private var isSpinning = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createNotificationChannel()

        val reel1 = view.findViewById<ImageView>(R.id.reel1)
        val reel2 = view.findViewById<ImageView>(R.id.reel2)
        val reel3 = view.findViewById<ImageView>(R.id.reel3)

        val txtCoins = view.findViewById<TextView>(R.id.txtCoins)
        val txtResult = view.findViewById<TextView>(R.id.txtResult)

        val btnSpin = view.findViewById<Button>(R.id.btnSpin)
        val btnExit = view.findViewById<Button>(R.id.btnExit)

        val db = DatabaseProvider.getDatabase(requireContext())

        val disposable = db.partidaDao().getLast()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ partida ->
                coins = partida.monedasFinales
                if (coins <= 0) coins = 100
                txtCoins.text = "Coins: $coins"
            }, {})

        disposables.add(disposable)

        btnSpin.setOnClickListener {

            if (isSpinning) return@setOnClickListener

            val bet = 10
            if (coins < bet) {
                coins = 100
                txtCoins.text = "Coins: $coins"
                return@setOnClickListener
            }

            isSpinning = true
            btnSpin.isEnabled = false
            txtResult.text = ""

            coins -= bet
            txtCoins.text = "Coins: $coins"

            val finalResult = gameEngine.spin()
            //val symbols = listOf("🍒", "🍋", "💎", "7","⭐","🔔")

            thread {

                repeat(15) {
                    val temp = gameEngine.spin()

                    activity?.runOnUiThread {
                        reel1.setImageResource(getImage(temp[0]))
                        reel2.setImageResource(getImage(temp[1]))
                        reel3.setImageResource(getImage(temp[2]))
                    }

                    Thread.sleep(80)
                }

                activity?.runOnUiThread {
                    reel1.setImageResource(getImage(finalResult[0]))
                    reel2.setImageResource(getImage(finalResult[1]))
                    reel3.setImageResource(getImage(finalResult[2]))
                }

                val reward = gameEngine.calculateReward(finalResult, bet)
                val oldCoins = coins
                coins += reward

                activity?.runOnUiThread {

                    animateCoins(txtCoins, oldCoins, coins)

                    if (reward > 0) {

                        showWinNotification(reward)

                        txtResult.text = "💥 WIN +$reward"
                        txtResult.setTextColor(Color.YELLOW)

                        winAnimation(txtResult)
                        vibrateWin()
                        playWinSound()
                        spawnCelebration(requireView())

                        AlertDialog.Builder(requireContext())
                            .setTitle("¡Victoria!")
                            .setMessage("¿Quieres guardar una captura?")
                            .setPositiveButton("Sí") { _, _ ->
                                captureScreenAndSave()
                            }
                            .setNegativeButton("No", null)
                            .show()

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

    // ---------------- NOTIFICACIONES ----------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "slot_channel",
                "Wins",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = requireContext().getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showWinNotification(reward: Int) {

        val intent = Intent(requireContext(), MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(requireContext(), "slot_channel")
            .setSmallIcon(R.drawable.coin)
            .setContentTitle("🎰 ¡Victoria!")
            .setContentText("Has ganado $reward monedas")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(requireContext())
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    // ---------------- CAPTURA ----------------

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

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SlotMaster")
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let {
            val stream = resolver.openOutputStream(it)
            if (stream != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()
            }
        }
    }

    // ---------------- EFECTOS ----------------

    private fun playWinSound() {
        MediaPlayer.create(requireContext(), R.raw.win).start()
    }

    private fun vibrateWin() {
        try {
            val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(200)
        } catch (_: Exception) {}
    }

    private fun spawnCelebration(view: View) {
        val container = requireActivity().window.decorView as ViewGroup
        repeat(25) {
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
                .setDuration(1500)
                .withEndAction { container.removeView(coin) }
                .start()
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

    // ---------------- UTIL ----------------

    private fun getImage(symbol: String): Int {
        return when (symbol) {
            "🍒" -> R.drawable.cherry
            "🍋" -> R.drawable.lemon
            "💎" -> R.drawable.diamond
            "7" -> R.drawable.seven
            "⭐" -> R.drawable.star
            "🔔" -> R.drawable.bell
            else -> R.drawable.cherry
        }
    }

    private fun animateCoins(textView: TextView, from: Int, to: Int) {
        textView.text = "Coins: $to"
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