package com.example.slotmaster.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.slotmaster.R
import com.example.slotmaster.audio.MusicService
import com.example.slotmaster.ui.fragments.*

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // SharedPreferences
        prefs = getSharedPreferences("settings", MODE_PRIVATE)

        // 🔝 Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "SLOT MASTER"

        // Pantalla inicial
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, WelcomeFragment())
                .commit()
        }
    }

    // Menú
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // Navegación
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.menu_home -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, WelcomeFragment())
                    .commit()
                true
            }

            R.id.menu_play -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, GameFragment())
                    .commit()
                true
            }

            R.id.menu_history -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, HistoryFragment())
                    .commit()
                true
            }

            R.id.menu_exit -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // APP A SEGUNDO PLANO
    override fun onPause() {
        super.onPause()

        stopService(Intent(this, MusicService::class.java))
    }

    // APP VUELVE
    override fun onResume() {
        super.onResume()

        val musicOn = prefs.getBoolean("music_on", false)

        if (musicOn) {
            startService(Intent(this, MusicService::class.java))
        }
    }
}