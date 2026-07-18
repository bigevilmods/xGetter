package com.htetznaing.xplayer

import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import com.htetznaing.xplayer.databinding.ActivityXplayerBinding

class XPlayer : AppCompatActivity() {
    companion object {
        @JvmField val XPLAYER_URL = "xPlayer.URL"
        val XPLAYER_POSITION = "xPlayer.POSITION"
        @JvmField val XPLAYER_COOKIE = "xPlayer.COOKIE"
    }

    private lateinit var binding: ActivityXplayerBinding
    private lateinit var mUrl: String
    private var mCookie: String = "null"
    private lateinit var player: ExoPlayer
    private var videoPosition: Long = 0L
    private var mediaSession: MediaSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityXplayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.extras == null || !intent.hasExtra(XPLAYER_URL)) {
            finish()
            return
        }

        mUrl = intent.getStringExtra(XPLAYER_URL)!!
        intent.getStringExtra(XPLAYER_COOKIE)?.let { mCookie = it }
        savedInstanceState?.let { videoPosition = it.getLong(XPLAYER_POSITION) }
    }

    override fun onStart() {
        super.onStart()

        player = ExoPlayer.Builder(this)
            .setTrackSelector(DefaultTrackSelector(this))
            .build()

        binding.playerView.player = player

        val userAgent = Util.getUserAgent(this, applicationInfo.loadLabel(packageManager).toString())
        var dataSourceFactory: DefaultDataSource.Factory

        if (mCookie != "null") {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setDefaultRequestProperties(mapOf("Cookie" to mCookie))
            dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)
        } else {
            dataSourceFactory = DefaultDataSource.Factory(this)
        }

        val mediaItem = MediaItem.fromUri(Uri.parse(mUrl))

        when (Util.inferContentType(Uri.parse(mUrl))) {
            C.CONTENT_TYPE_HLS -> {
                val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
                player.setMediaSource(mediaSource)
            }

            C.CONTENT_TYPE_OTHER -> {
                val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
                player.setMediaSource(mediaSource)
            }

            else -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
                return
            }
        }

        binding.progresbarVideoPlay.visibility = View.VISIBLE

        var returnResultOnce = true
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && returnResultOnce) {
                    setResult(Activity.RESULT_OK)
                    binding.progresbarVideoPlay.visibility = View.GONE
                    returnResultOnce = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                setResult(Activity.RESULT_CANCELED)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    finishAndRemoveTask()
                }
            }
        })

        player.prepare()
        player.playWhenReady = true

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onPause() {
        videoPosition = player.currentPosition
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (videoPosition > 0L) {
            player.seekTo(videoPosition)
        }
        player.playWhenReady = true
        binding.playerView.useController = true
    }

    override fun onStop() {
        videoPosition = player.currentPosition
        player.playWhenReady = false
        super.onStop()
    }

    override fun onDestroy() {
        binding.playerView.player = null
        player.release()
        mediaSession?.release()
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            }
        }
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(XPLAYER_POSITION, player.currentPosition)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        videoPosition = savedInstanceState.getLong(XPLAYER_POSITION)
    }
}