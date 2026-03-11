package com.example.slotmaster.ui
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.slotmaster.R
import com.example.slotmaster.ui.fragments.WelcomeFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, WelcomeFragment())
                .commit()
        }
    }
}