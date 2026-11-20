package com.example.streamplayer

import android.os.Bundle
import android.util.Base64
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_USER = "user"
        const val EXTRA_PASS = "pass"
    }

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private var position = 0L
    private var playWhenReady = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        playerView = findViewById(R.id.playerView)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, playerView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        savedInstanceState?.let {
            position = it.getLong("pos", 0L)
            playWhenReady = it.getBoolean("play", true)
        }
    }

    override fun onStart() {
        super.onStart()
        initPlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        player?.let {
            outState.putLong("pos", it.currentPosition)
            outState.putBoolean("play", it.playWhenReady)
        }
    }

    private fun initPlayer() {
        val url = intent.getStringExtra(EXTRA_URL) ?: return finish()
        val user = intent.getStringExtra(EXTRA_USER).orEmpty()
        val pass = intent.getStringExtra(EXTRA_PASS).orEmpty()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)

        if (user.isNotEmpty()) {
            val token = Base64.encodeToString("$user:$pass".toByteArray(), Base64.NO_WRAP)
            httpFactory.setDefaultRequestProperties(mapOf("Authorization" to "Basic $token"))
        }

        val exo = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory)) // MP4, MKV, HLS, DASH
            .build()

        exo.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(
                    this@PlayerActivity,
                    "Gagal memutar: ${error.errorCodeName}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        playerView.player = exo
        exo.setMediaItem(MediaItem.fromUri(url))
        exo.seekTo(position)
        exo.playWhenReady = playWhenReady
        exo.prepare()
        player = exo
    }

    private fun releasePlayer() {
        player?.let {
            position = it.currentPosition
            playWhenReady = it.playWhenReady
            it.release()
        }
        player = null
    }
}
