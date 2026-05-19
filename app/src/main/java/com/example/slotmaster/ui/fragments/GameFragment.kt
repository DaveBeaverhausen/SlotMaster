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
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.TimeZone

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

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

        val dbLocal = DatabaseProvider.getDatabase(requireContext())

        val disposable = dbLocal.partidaDao().getLast()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ partida ->
                coins = partida.monedasFinales
                if (coins <= 0) coins = 100
                txtCoins.text = getString(R.string.coins_amount, coins)
            }, {})

        disposables.add(disposable)

        btnSpin.setOnClickListener {

            if (isSpinning) return@setOnClickListener

            val bet = 10
            if (coins < bet) {
                coins = 100
                txtCoins.text = getString(R.string.coins_amount, coins)
                return@setOnClickListener
            }

            isSpinning = true
            btnSpin.isEnabled = false
            txtResult.text = ""

            coins -= bet
            txtCoins.text = getString(R.string.coins_amount, coins)

            val finalResult = gameEngine.spin()

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

                        // 🔥 DEBUG
                        Log.d("FIRESTORE", "ENTRA A SAVE")

                        // 🔥 FIREBASE
                        saveWinToFirebase(reward)

                        // 🔥 TOP 10
                        getTopScores()

                        txtResult.text = getString(R.string.win_amount, reward)
                        txtResult.setTextColor(Color.YELLOW)

                        winAnimation(txtResult)
                        vibrateWin()
                        playWinSound()
                        spawnCelebration(requireView())

                        AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.win))
                            .setMessage(getString(R.string.capture))
                            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                                captureScreenAndSave()
                            }
                            .setNegativeButton(getString(R.string.no), null)
                            .show()

                    } else {
                        txtResult.text = getString(R.string.lose)
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

    //  FIREBASE SAVE
    private fun saveWinToFirebase(reward: Int) {

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            Log.e("FIRESTORE", "Usuario NULL")
            return
        }

        val db = FirebaseFirestore.getInstance()

        val data = hashMapOf(
            "userId" to user.uid,
            "username" to (user.displayName ?: "unknown"),
            "score" to reward,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("scores")
            .add(data)
            .addOnSuccessListener {
                Log.d("FIRESTORE", "✅ Guardado OK")
            }
            .addOnFailureListener {
                Log.e("FIRESTORE", "❌ Error", it)
            }
    }
    // 🔥 TOP 10
    private fun getTopScores() {

        val db = FirebaseFirestore.getInstance()

        db.collection("scores")
            .orderBy("score", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { result ->

                val list = mutableListOf<String>()

                for (doc in result) {
                    val user = doc.getString("username") ?: "unknown"
                    val score = doc.getLong("score") ?: 0
                    list.add("$user → $score")
                }

                showTopDialog(list)
            }
            .addOnFailureListener {
                Log.e("TOP10", "Error obteniendo ranking", it)
            }
    }

    // 🔥 UI TOP
    private fun showTopDialog(list: List<String>) {

        val message = if (list.isEmpty()) "Sin datos"
        else list.joinToString("\n")

        AlertDialog.Builder(requireContext())
            .setTitle("🏆 TOP 10 GLOBAL")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
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
        textView.text = getString(R.string.coins_amount, to)
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

    /**
     * Guarda una victoria en el calendario del dispositivo.
     * - Usa ContentProvider (CalendarContract)
     * - Se ejecuta en un hilo secundario
     * - Registra fecha y hora actual
     */
    private fun saveWinToCalendar() {

        Log.d("CALENDAR", "Intentando guardar evento...")

        // 1. Comprobar permisos correctamente
        if (
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("CALENDAR", "Permisos NO concedidos")
            return
        }

        // 2. Ejecutar en hilo
        Thread {

            try {

                val calendarId = getCalendarId()

                Log.d("CALENDAR", "Calendar ID obtenido: $calendarId")

                //val startTime = System.currentTimeMillis()
                //val startTime = System.currentTimeMillis() + 2 * 60 * 1000
                //val endTime = startTime + 60 * 60 * 1000 // 1 hora
                val startTime = System.currentTimeMillis() + 60 * 1000
                val endTime = startTime + 5 * 60 * 1000

                val values = ContentValues().apply {
                    put(CalendarContract.Events.DTSTART, startTime)
                    put(CalendarContract.Events.DTEND, endTime)
                    put(CalendarContract.Events.TITLE, "Victoria SlotMaster 🎰")
                    put(CalendarContract.Events.DESCRIPTION, "Ganaste una partida")
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                    put(CalendarContract.Events.HAS_ALARM, 1)
                }

                val uri = requireContext().contentResolver.insert(
                    CalendarContract.Events.CONTENT_URI,
                    values
                )

                if (uri != null) {

                    Log.d("CALENDAR", "✅ Evento insertado: $uri")

                    val eventId = uri.lastPathSegment?.toLongOrNull()

                    if (eventId != null) {

                        val reminderValues = ContentValues().apply {
                            put(CalendarContract.Reminders.EVENT_ID, eventId)
                            put(CalendarContract.Reminders.MINUTES, 1)
                            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                        }

                        requireContext().contentResolver.insert(
                            CalendarContract.Reminders.CONTENT_URI,
                            reminderValues
                        )

                        Log.d("CALENDAR", "🔔 Recordatorio añadido al evento")
                    }

                } else {
                    Log.e("CALENDAR", "❌ ERROR insert NULL")
                }

            } catch (e: Exception) {
                Log.e("CALENDAR", "❌ EXCEPCIÓN: ${e.message}")
                e.printStackTrace()
            }

        }.start()
    }

    /**
     * Obtiene el ID del calendario disponible en el dispositivo.
     */
    private fun getCalendarId(): Long {

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )

        val cursor = requireContext().contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        cursor?.use {

            Log.d("CALENDAR", "Buscando calendario de Google...")

            while (it.moveToNext()) {
                val id = it.getLong(0)
                val accountName = it.getString(1)
                val accountType = it.getString(2)

                Log.d("CALENDAR", "ID=$id | Account=$accountName | Type=$accountType")

                // 👉 Este es el filtro clave
                if (accountType == "com.google") {
                    Log.d("CALENDAR", "✔ Usando calendario GOOGLE: $id")
                    return id
                }
            }

            // fallback → si no hay Google
            it.moveToFirst()
            return it.getLong(0)
        }

        throw Exception("No hay calendarios disponibles")
    }
}