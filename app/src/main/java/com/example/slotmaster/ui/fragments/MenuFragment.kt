package com.example.slotmaster.ui.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.slotmaster.R
import com.example.slotmaster.audio.MusicService

class MenuFragment : Fragment(R.layout.fragment_menu) {

    private lateinit var prefs: SharedPreferences
    private var musicOn = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnPlay = view.findViewById<Button>(R.id.btnPlay)
        val btnHistory = view.findViewById<Button>(R.id.btnHistory)
        val btnExit = view.findViewById<Button>(R.id.btnExit)
        val btnMusic = view.findViewById<Button>(R.id.btnMusic)

        // SharedPreferences
        prefs = requireContext().getSharedPreferences("settings", 0)

        // Cargar estado guardado
        musicOn = prefs.getBoolean("music_on", false)

        // Ajustar texto del botón según estado
        btnMusic.text = if (musicOn) "Música OFF" else "Música ON"

        // JUGAR
        btnPlay.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, GameFragment())
                .commit()
        }

        // HISTORIAL
        btnHistory.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HistoryFragment())
                .commit()
        }

        // SALIR
        btnExit.setOnClickListener {
            requireActivity().finish()
        }

        // MÚSICA ON/OFF
        btnMusic.setOnClickListener {

            if (!musicOn) {
                requireContext().startService(
                    Intent(requireContext(), MusicService::class.java)
                )

                prefs.edit().putBoolean("music_on", true).apply()

                btnMusic.text = "Música OFF"
                musicOn = true

            } else {
                requireContext().stopService(
                    Intent(requireContext(), MusicService::class.java)
                )

                prefs.edit().putBoolean("music_on", false).apply()

                btnMusic.text = "Música ON"
                musicOn = false
            }
        }
    }
}