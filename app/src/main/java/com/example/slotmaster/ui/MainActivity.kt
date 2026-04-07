package com.example.slotmaster.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.slotmaster.R
import com.example.slotmaster.ui.fragments.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Configurar Toolbar
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

    // Mostrar menú
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // Acciones del menú
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
}