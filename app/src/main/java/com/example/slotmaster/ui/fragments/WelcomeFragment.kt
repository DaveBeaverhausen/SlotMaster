package com.example.slotmaster.ui.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.slotmaster.R

class WelcomeFragment : Fragment(R.layout.fragment_welcome) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnStart = view.findViewById<Button>(R.id.btnStart)

        btnStart.setOnClickListener {

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MenuFragment())
                .addToBackStack(null)
                .commit()

        }
    }
}