package com.example.slotmaster.ui.fragments

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.slotmaster.R

class WelcomeFragment : Fragment(R.layout.fragment_welcome) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnStart = view.findViewById<Button>(R.id.btnStart)
        val title = view.findViewById<TextView>(R.id.txtTitle)

        // animaciones al entrar
        animateTitle(title)
        startCoinRain()
        playIntroSound()

        btnStart.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MenuFragment())
                .commit()
        }
    }

    private fun animateTitle(view: TextView) {

        view.translationY = -300f
        view.alpha = 0f

        view.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(600)
            .withEndAction {

                view.animate()
                    .translationY(-40f)
                    .setDuration(150)
                    .withEndAction {
                        view.animate()
                            .translationY(0f)
                            .setDuration(150)
                            .start()
                    }
                    .start()
            }
            .start()
    }

    private fun startCoinRain() {

        val container = requireActivity().window.decorView as ViewGroup

        repeat(30) {

            val coin = ImageView(requireContext())
            coin.setImageResource(R.drawable.coin)

            val size = (40..80).random()
            val params = ViewGroup.LayoutParams(size, size)
            coin.layoutParams = params

            coin.x = (0..container.width).random().toFloat()
            coin.y = -100f

            container.addView(coin)

            coin.animate()
                .translationY(container.height.toFloat())
                .rotation((0..360).random().toFloat())
                .setDuration((800..1500).random().toLong())
                .withEndAction {
                    container.removeView(coin)
                }
                .start()
        }
    }

    private fun playIntroSound() {
        val mediaPlayer = MediaPlayer.create(requireContext(), R.raw.intro)
        mediaPlayer.start()

        mediaPlayer.setOnCompletionListener {
            it.release()
        }
    }
}