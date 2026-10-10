package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class GameAudio(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // When an ad, incoming call, or other media takes audio, pause music immediately
                pauseMusicInternal()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // When audio focus is regained and app is in foreground, auto resume music
                if (isAppInForeground && isSoundEnabled) {
                    resumeMusicInternal()
                }
            }
        }
    }

    private val audioScope = CoroutineScope(Dispatchers.Default)

    var isSoundEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                pauseMusic()
            } else {
                resumeMusic()
            }
        }

    var isVibrateEnabled: Boolean = true

    // Background Phonk Music Player
    private var mediaPlayer: MediaPlayer? = null
    private var isAppInForeground: Boolean = true

    // Pre-rendered PCM audio buffers for special game sound effects
    private val sampleRate = 22050
    private var scorePcm: ShortArray? = null
    private var coinPcm: ShortArray? = null
    private var hitPcm: ShortArray? = null

    init {
        audioScope.launch {
            scorePcm = generateScoreSound()
            coinPcm = generateCoinSound()
            hitPcm = generateHitSound()
        }
        initMusicPlayer()
    }

    private fun initMusicPlayer() {
        try {
            mediaPlayer = MediaPlayer.create(context, R.raw.phonk_music)?.apply {
                isLooping = true
                // User requested: "background par mara veja hua gana 80% sound par lagadena"
                setVolume(0.80f, 0.80f)
            }
            if (isSoundEnabled && isAppInForeground) {
                requestAudioFocus()
                mediaPlayer?.start()
            }
        } catch (_: Exception) {
            // Graceful fallback if audio hardware is unavailable
        }
    }

    @Suppress("DEPRECATION")
    private fun requestAudioFocus() {
        try {
            audioManager?.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        } catch (_: Exception) {
        }
    }

    @Suppress("DEPRECATION")
    private fun abandonAudioFocus() {
        try {
            audioManager?.abandonAudioFocus(focusChangeListener)
        } catch (_: Exception) {
        }
    }

    /**
     * Start / resume phonk background music
     */
    fun startMusic() {
        isAppInForeground = true
        if (isSoundEnabled) {
            resumeMusicInternal()
        }
    }

    /**
     * Pause music when app is minimized, ads appear, or user navigates away
     * User requested: "song game minimaiz par nai chalega or koi ads aiga tobi song Ruk mai ga"
     */
    fun pauseMusic() {
        isAppInForeground = false
        pauseMusicInternal()
        abandonAudioFocus()
    }

    private fun pauseMusicInternal() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Resume music when user returns to app
     * User requested: "back ane par automatic song chalu ho jai ga ok"
     */
    fun resumeMusic() {
        isAppInForeground = true
        if (isSoundEnabled) {
            resumeMusicInternal()
        }
    }

    private fun resumeMusicInternal() {
        try {
            requestAudioFocus()
            if (mediaPlayer != null) {
                if (!mediaPlayer!!.isPlaying) {
                    mediaPlayer?.setVolume(0.80f, 0.80f)
                    mediaPlayer?.start()
                }
            } else {
                initMusicPlayer()
            }
        } catch (_: Exception) {
            initMusicPlayer()
        }
    }

    fun releaseMusic() {
        try {
            abandonAudioFocus()
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {
        }
    }

    fun getPlaybackPositionMs(): Int {
        return try {
            mediaPlayer?.currentPosition ?: 0
        } catch (_: Exception) {
            0
        }
    }

    fun isMusicPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * User requested: "Her click par sound ko hatado"
     * Removed the click/flap sound on every click completely!
     * Only subtle haptic vibration is kept.
     */
    fun playFlap() {
        if (isVibrateEnabled) {
            vibrate(12, 45)
        }
    }

    fun playScore() {
        if (isSoundEnabled) {
            scorePcm?.let { playPcm(it) }
        }
        if (isVibrateEnabled) {
            vibrate(30, 140)
        }
    }

    fun playCoin() {
        if (isSoundEnabled) {
            coinPcm?.let { playPcm(it) }
        }
        if (isVibrateEnabled) {
            vibrate(40, 180)
        }
    }

    fun playHit() {
        if (isSoundEnabled) {
            hitPcm?.let { playPcm(it) }
        }
        if (isVibrateEnabled) {
            vibrate(180, 255)
        }
    }

    private fun vibrate(durationMs: Long, amplitude: Int) {
        try {
            vibrator?.let {
                if (it.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val effect = VibrationEffect.createOneShot(
                            durationMs,
                            amplitude.coerceIn(1, 255)
                        )
                        it.vibrate(effect)
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(durationMs)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun playPcm(pcm: ShortArray) {
        audioScope.launch {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                launch {
                    val durationMs = (pcm.size * 1000L) / sampleRate + 50
                    kotlinx.coroutines.delay(durationMs)
                    track.stop()
                    track.release()
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun generateScoreSound(): ShortArray {
        val durationMs = 120
        val numSamples = (sampleRate * durationMs) / 1000
        val pcm = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val envelope = (1.0 - progress) * (1.0 - progress)
            val sample = (sin(2.0 * PI * 1046.5 * t) * 0.45 + sin(2.0 * PI * 2093.0 * t) * 0.18) * envelope
            pcm[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return pcm
    }

    private fun generateCoinSound(): ShortArray {
        val durationMs = 140
        val numSamples = (sampleRate * durationMs) / 1000
        val pcm = ShortArray(numSamples)
        val half = numSamples / 2
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = if (i < half) 987.77 else 1318.51
            val localProgress = if (i < half) (i.toDouble() / half) else ((i - half).toDouble() / half)
            val envelope = 1.0 - localProgress * 0.8
            val sample = sin(2.0 * PI * freq * t) * envelope * 0.50
            pcm[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return pcm
    }

    private fun generateHitSound(): ShortArray {
        val durationMs = 160
        val numSamples = (sampleRate * durationMs) / 1000
        val pcm = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = 180.0 * (1.0 - progress * 0.7)
            val envelope = (1.0 - progress)
            val noise = (Math.random() * 2.0 - 1.0) * 0.15
            val sample = (sin(2.0 * PI * freq * t) + noise) * envelope * 0.65
            pcm[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return pcm
    }
}
