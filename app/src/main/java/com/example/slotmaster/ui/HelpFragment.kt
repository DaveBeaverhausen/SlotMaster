package com.example.slotmaster.ui

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.example.slotmaster.R

class HelpFragment : Fragment(R.layout.fragment_help) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val webView = view.findViewById<WebView>(R.id.webViewHelp)
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = false
        webView.loadUrl("file:///android_asset/help.html")
    }
}