package com.example.slotmaster.ui.fragments

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.slotmaster.R

class HelpFragment : Fragment(R.layout.fragment_help) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // WEBVIEW
        val webView = view.findViewById<WebView>(R.id.webViewHelp)
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = false
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        val prefs = requireContext().getSharedPreferences("settings", 0)
        val lang = prefs.getString("lang", "es") ?: "es"

        val fileName = when (lang) {
            "en" -> "help_en.html"
            "fr" -> "help_fr.html"
            else -> "help_es.html"
        }

        webView.loadUrl("file:///android_asset/$fileName")

        // BOTÓN VOLVER
        val btnBack = view.findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MenuFragment())
                .commit()
        }
    }
}
