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
import io.reactivex.rxjava3.disposables.CompositeDisposable

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private val disposables = CompositeDisposable()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val txtHistory = view.findViewById<TextView>(R.id.txtHistory)
        val btnBack = view.findViewById<Button>(R.id.btnBack)

        // Volver al menú
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val db = DatabaseProvider.getDatabase(requireContext())

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val disposable = db.partidaDao().getAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { partidas ->

                    val historyText = StringBuilder()

                    if (partidas.isEmpty()) {
                        historyText.append("No hay partidas")
                    } else {
                        partidas.forEach {

                            val fechaFormateada = sdf.format(Date(it.fecha))

                            historyText.append(
                                "🗓 $fechaFormateada\n" +
                                        "🎰 Resultado: ${it.resultado}\n" +
                                        "💰 Monedas: ${it.monedasFinales}\n" +
                                        "------------------------\n"
                            )
                        }
                    }

                    txtHistory.text = historyText.toString()
                },
                { error ->
                    error.printStackTrace()
                    txtHistory.text = "Error al cargar historial"
                }
            )
        disposables.add(disposable)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        disposables.clear()
    }
}