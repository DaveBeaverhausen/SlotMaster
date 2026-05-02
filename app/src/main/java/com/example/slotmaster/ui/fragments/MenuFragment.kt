package com.example.slotmaster.ui.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.slotmaster.R
import com.example.slotmaster.audio.MusicService
import android.app.AlertDialog
import androidx.core.content.edit

class MenuFragment : Fragment(R.layout.fragment_menu) {

    private lateinit var prefs: SharedPreferences
    private var musicOn = false
    private var currentLang = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnPlay = view.findViewById<Button>(R.id.btnPlay)
        val btnHistory = view.findViewById<Button>(R.id.btnHistory)
        val btnExit = view.findViewById<Button>(R.id.btnExit)
        val btnMusic = view.findViewById<Button>(R.id.btnMusic)
        val btnLanguage = view.findViewById<Button>(R.id.btnLanguage)

        // SharedPreferences
        prefs = requireContext().getSharedPreferences("settings", 0)

        // Idioma actual
        currentLang = prefs.getString("lang", "es") ?: "es"

        // Estado música
        musicOn = prefs.getBoolean("music_on", false)

        btnMusic.text = if (musicOn)
            getString(R.string.music_off)
        else
            getString(R.string.music_on)

        // BOTONES

        btnPlay.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, GameFragment())
                .commit()
        }

        btnHistory.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HistoryFragment())
                .commit()
        }

        btnExit.setOnClickListener {
            requireActivity().finish()
        }

        btnMusic.setOnClickListener {
            toggleMusic(btnMusic)
        }

        btnLanguage.setOnClickListener {
            showLanguageDialog()
        }
    }

    // ---------------- MÚSICA ----------------

    private fun toggleMusic(btnMusic: Button) {
        if (!musicOn) {
            requireContext().startService(
                Intent(requireContext(), MusicService::class.java)
            )

            prefs.edit { putBoolean("music_on", true) }

            btnMusic.text = getString(R.string.music_off)
            musicOn = true

        } else {
            requireContext().stopService(
                Intent(requireContext(), MusicService::class.java)
            )

            prefs.edit { putBoolean("music_on", false) }

            btnMusic.text = getString(R.string.music_on)
            musicOn = false
        }
    }

    // ---------------- IDIOMA ----------------

    private fun showLanguageDialog() {

        val options = arrayOf("🇪🇸 Español", "🇬🇧 English", "🇫🇷 Français")

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.select_language))
            .setItems(options) { _, which ->

                val lang = when (which) {
                    0 -> "es"
                    1 -> "en"
                    2 -> "fr"
                    else -> "es"
                }

                if (lang == currentLang) return@setItems

                currentLang = lang
                changeLanguage(lang)
            }
            .show()
    }

    private fun changeLanguage(lang: String) {
        prefs.edit { putString("lang", lang) }
        requireActivity().recreate()
    }
}