package com.example.slotmaster.ui.fragments

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.slotmaster.R
import com.example.slotmaster.database.DatabaseProvider
import kotlin.concurrent.thread

class HistoryFragment : Fragment(R.layout.fragment_history) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val txtHistory = view.findViewById<TextView>(R.id.txtHistory)

        thread {

            val db = DatabaseProvider.getDatabase(requireContext())
            val partidas = db.partidaDao().getAllList()

            val historyText = StringBuilder()

            if (partidas.isEmpty()) {
                historyText.append("No hay partidas guardadas todavía")
            } else {
                partidas.forEach {
                    historyText.append(
                        "Resultado: ${it.resultado} | Monedas: ${it.monedasFinales}\n"
                    )
                }
            }

            activity?.runOnUiThread {
                txtHistory.text = historyText.toString()
            }
        }
    }
}