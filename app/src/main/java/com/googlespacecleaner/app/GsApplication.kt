package com.googlespacecleaner.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.Coil
import coil.ImageLoader
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Point d'entrée de l'application. Annotée @HiltAndroidApp pour déclencher
 * la génération du graphe de dépendances Hilt.
 *
 * Implémente Configuration.Provider pour que WorkManager puisse injecter
 * les dépendances dans les Workers (ex: DriveScanWorker) via Hilt.
 */
@HiltAndroidApp
class GsApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /**
     * Même client OkHttp que Retrofit (voir NetworkAuthModule) : les URLs de
     * miniatures Drive/Photos Picker exigent un en-tête OAuth valide pour se
     * charger (403 sinon), donc Coil doit utiliser le même intercepteur.
     */
    @Inject
    lateinit var sharedOkHttpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient(sharedOkHttpClient)
                .crossfade(true)
                .build()
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
