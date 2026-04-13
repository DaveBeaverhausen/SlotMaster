package com.example.slotmaster.ui.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.slotmaster.R
import com.example.slotmaster.database.DatabaseProvider
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment(R.layout.fragment_history) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val txtHistory = view.findViewById<TextView>(R.id.txtHistory)
        val btnBack = view.findViewById<Button>(R.id.btnBack)

        val db = DatabaseProvider.getDatabase(requireContext())

        db.partidaDao().getAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ partidas ->

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val historyText = StringBuilder()

                partidas.forEach { partida ->

                    val fechaFormateada = sdf.format(Date(partida.fecha))

                    historyText.append(
                        "📅 $fechaFormateada\n" +
                                "🎰 ${partida.resultado}\n" +
                                "💰 ${partida.monedasFinales} monedas\n\n"
                    )
                }

                txtHistory.text = historyText.toString()

            }, { error ->
                error.printStackTrace()
            })

        btnBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MenuFragment())
                .commit()
        }
    }
}