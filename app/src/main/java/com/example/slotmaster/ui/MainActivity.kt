package com.example.slotmaster.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.room.Room
import com.example.slotmaster.R
import com.example.slotmaster.database.AppDatabase
import com.example.slotmaster.ui.fragments.WelcomeFragment

lateinit var database: AppDatabase

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar base de datos SQLite (Room)
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "slotmaster-db"
        ).build()

        setContentView(R.layout.activity_main)

        // Cargar pantalla de bienvenida
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, WelcomeFragment())
                .commit()
        }
    }
}