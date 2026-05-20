package com.example.slotmaster.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.slotmaster.R
import com.example.slotmaster.database.GameRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private val gameRepository = GameRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val txtHistory = view.findViewById<TextView>(R.id.txtHistory)
        val btnBack = view.findViewById<Button>(R.id.btnBack)

        txtHistory.text = "Cargando historial global..."

        loadGlobalHistory(txtHistory)

        btnBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MenuFragment())
                .commit()
        }
    }

    private fun loadGlobalHistory(txtHistory: TextView) {
        gameRepository.getGlobalHistory(
            onSuccess = { games ->

                if (games.isEmpty()) {
                    txtHistory.text = "No hay partidas globales todavía."
                    return@getGlobalHistory
                }

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val historyText = StringBuilder()

                games.forEach { game ->

                    val fechaFormateada = if (game.createdAt != null) {
                        sdf.format(game.createdAt.toDate())
                    } else {
                        "Fecha no disponible"
                    }

                    historyText.append(
                        "Fecha: $fechaFormateada\n" +
                                "Usuario: ${game.username}\n" +
                                "Resultado: ${game.result}\n" +
                                "Tirada: ${game.symbols}\n" +
                                "Recompensa: ${game.score}\n" +
                                "Monedas finales: ${game.coinsAfter}\n" +
                                "Apuesta: ${game.bet}\n\n"
                    )
                }

                txtHistory.text = historyText.toString()
            },
            onError = { error ->
                Log.e("HISTORY", "Error cargando historial global: $error")

                txtHistory.text = "No se pudo cargar el historial global."

                Toast.makeText(
                    requireContext(),
                    error,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
}