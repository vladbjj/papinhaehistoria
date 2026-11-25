package com.papinhas.bebeapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var webView: WebView
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    // Bloco oficial AdMob
    private val adUnitId = "ca-app-pub-3016178863457047/8931527209"
    private var appOpenAd: AppOpenAd? = null
    private var isAdShownOnce = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o motor de fala
        tts = TextToSpeech(this, this)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
        }

        webView.addJavascriptInterface(WebAppInterface(), "AndroidTTS")
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        webView.loadUrl("file:///android_asset/index.html")

        // Carrega o anúncio de abertura de forma não invasiva apenas na inicialização
        carregarAnuncioAbertura()

        // Tratamento limpo do botão Voltar (sem disparar anúncios ao sair)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    finish() // Sai do app sem disparar anúncio
                }
            }
        })
    }

    private fun carregarAnuncioAbertura() {
        if (isAdShownOnce) return

        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            this,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    if (!isFinishing && !isAdShownOnce) {
                        exibirAnuncioAbertura()
                    }
                }

                override fun onAdFailedToLoad(loadAdError: com.google.android.gms.ads.LoadAdError) {
                    appOpenAd = null
                }
            }
        )
    }

    private fun exibirAnuncioAbertura() {
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isAdShownOnce = true
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                appOpenAd = null
            }

            override fun onAdShowedFullScreenContent() {
                isAdShownOnce = true
            }
        }
        appOpenAd?.show(this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("pt", "BR"))
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
                tts?.setSpeechRate(0.9f)
                tts?.setPitch(1.05f)
            }
        }
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun falar(texto: String) {
            runOnUiThread {
                if (isTtsReady) {
                    tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "HistoriaId")
                }
            }
        }

        @JavascriptInterface
        fun parar() {
            runOnUiThread {
                tts?.stop()
            }
        }
    }

    override fun onPause() {
        tts?.stop()
        super.onPause()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        webView.destroy()
        super.onDestroy()
    }
}
