package com.papinhaehistoria.app

import android.app.Application
import com.google.android.gms.ads.MobileAds

class MeuBebeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Inicializa o SDK de anúncios do Google em segundo plano
        MobileAds.initialize(this) {}
    }
}
