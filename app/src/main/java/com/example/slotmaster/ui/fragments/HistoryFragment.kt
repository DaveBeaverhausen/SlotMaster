package com.example.slotmaster.ui.fragments

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.slotmaster.R
import com.example.slotmaster.ui.database
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers

class HistoryFragment : Fragment(R.layout.fragment_history) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val txtHistory = view.findViewById<TextView>(R.id.txtHistory)

        database.partidaDao().getAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { partidas ->

                val historyText = StringBuilder()

                partidas.forEach {
                    historyText.append(
                        "Resultado: ${it.resultado} | Monedas: ${it.monedasFinales}\n"
                    )
                }

                txtHistory.text = historyText.toString()
            }
    }
}