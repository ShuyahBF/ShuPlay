package com.shuyah.shuplay

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Service de lecture : il porte le lecteur (ExoPlayer) pour que le son CONTINUE quand la fenêtre perd le focus, quand on change
 * d'application ou que l'écran s'éteint. Il fournit aussi la notification de contrôle, les boutons du casque Bluetooth
 * et les commandes de l'écran verrouillé (MediaSession).
 */
class ServiceLecture : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val lecteur = ExoPlayer.Builder(this)
            // Focus audio : pause pendant un appel ou quand une autre application prend le son ; reprise ensuite
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
            // Casque débranché : on met en pause au lieu de jouer sur le haut-parleur
            .setHandleAudioBecomingNoisy(true)
            // Garde le processeur et le Wi-Fi éveillés pendant la lecture écran éteint
            .setWakeMode(C.WAKE_MODE_NETWORK)
            // Retour et avance rapides : 10 secondes par appui
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
        session = MediaSession.Builder(this, lecteur).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** L'utilisateur ferme l'application depuis la liste des applications récentes : on s'arrête si rien ne joue. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val lecteur = session?.player
        if (lecteur == null || !lecteur.playWhenReady || lecteur.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run { player.release(); release() }
        session = null
        super.onDestroy()
    }
}
