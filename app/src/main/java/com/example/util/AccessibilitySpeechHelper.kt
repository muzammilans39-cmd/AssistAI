package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.UUID

class AccessibilitySpeechHelper(private val context: Context) {

    val hapticManager = AssistHapticManager(context)
    val sttHandler = SpeechToTextHandler(context, hapticManager)

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var pendingSpeechText: String? = null
    private var speakingTimeoutRunnable: Runnable? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    val isListening: StateFlow<Boolean> = sttHandler.isListening
    val livePartialTranscript: StateFlow<String> = sttHandler.partialTranscript
    val sttStatus: StateFlow<SttStatus> = sttHandler.status
    val rmsDbLevel: StateFlow<Float> = sttHandler.rmsDbLevel
    val lastHapticPattern: StateFlow<VoiceHapticPattern?> = hapticManager.lastTriggeredPattern
    val hapticsEnabled: StateFlow<Boolean> = hapticManager.hapticsEnabled

    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val CHANNEL_ID = "assist_ai_alerts_channel"
        private const val CHANNEL_NAME = "AssistAI Reminders & Alarms"
    }

    init {
        initTextToSpeech()
        createNotificationChannel()
    }

    private fun initTextToSpeech() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.getDefault())
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.US)
                    }
                    tts?.setSpeechRate(0.92f)
                    tts?.setPitch(1.0f)
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            _isSpeaking.value = false
                        }
                    })
                    isTtsReady = true
                    pendingSpeechText?.let { queued ->
                        pendingSpeechText = null
                        speak(queued, flush = true)
                    }
                }
            }
        } catch (_: Exception) {
            isTtsReady = false
        }
    }

    fun speak(text: String, flush: Boolean = true) {
        if (text.isBlank()) return
        speakingTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }

        if (!isTtsReady || tts == null) {
            pendingSpeechText = text
            _isSpeaking.value = true
            val resetRunnable = Runnable { _isSpeaking.value = false }
            speakingTimeoutRunnable = resetRunnable
            mainHandler.postDelayed(resetRunnable, 2200L)
            return
        }
        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val utteranceId = UUID.randomUUID().toString()
        _isSpeaking.value = true
        val result = try {
            tts?.speak(text, queueMode, null, utteranceId) ?: TextToSpeech.ERROR
        } catch (_: Exception) {
            TextToSpeech.ERROR
        }
        val fallbackDurationMs = if (result == TextToSpeech.ERROR) {
            1800L
        } else {
            (text.length * 75L).coerceIn(2500L, 15000L)
        }
        val resetRunnable = Runnable { _isSpeaking.value = false }
        speakingTimeoutRunnable = resetRunnable
        mainHandler.postDelayed(resetRunnable, fallbackDurationMs)
    }

    fun stopSpeaking() {
        speakingTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        speakingTimeoutRunnable = null
        pendingSpeechText = null
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        _isSpeaking.value = false
    }

    fun setSlowSpeechRate(slow: Boolean) {
        tts?.setSpeechRate(if (slow) 0.78f else 0.95f)
    }

    fun startSpeechRecognition(
        onFinalResult: (String) -> Unit,
        onErrorMessage: (String) -> Unit
    ) {
        stopSpeaking()
        sttHandler.startListening(
            onResult = onFinalResult,
            onError = onErrorMessage
        )
    }

    fun stopSpeechRecognition() {
        sttHandler.stopListening()
    }

    fun startAudioFileRecording(): File? {
        return try {
            val outFile = File(context.cacheDir, "call_recording_${System.currentTimeMillis()}.m4a")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(outFile.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            currentRecordingFile = outFile
            outFile
        } catch (_: Exception) {
            null
        }
    }

    fun stopAudioFileRecording(): Pair<String?, String?> {
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        mediaRecorder = null

        val file = currentRecordingFile
        currentRecordingFile = null
        if (file != null && file.exists() && file.length() > 0L && file.length() < 8 * 1024 * 1024L) {
            return try {
                val bytes = file.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                Pair(base64, "audio/mp4")
            } catch (_: Exception) {
                Pair(null, null)
            }
        }
        return Pair(null, null)
    }

    fun readAudioUriAsBase64(uri: Uri): Triple<String?, String?, String> {
        return try {
            val mimeType = context.contentResolver.getType(uri) ?: "audio/mpeg"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "uploaded_call_audio.mp3"
            if (bytes != null && bytes.isNotEmpty() && bytes.size < 10 * 1024 * 1024) {
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                Triple(base64, mimeType, fileName)
            } else {
                Triple(null, mimeType, fileName)
            }
        } catch (_: Exception) {
            Triple(null, null, "uploaded_audio")
        }
    }

    fun readImageUriAsBase64(uri: Uri): Pair<String?, String?> {
        return try {
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null && bytes.isNotEmpty() && bytes.size < 8 * 1024 * 1024) {
                Pair(Base64.encodeToString(bytes, Base64.NO_WRAP), mimeType)
            } else {
                Pair(null, mimeType)
            }
        } catch (_: Exception) {
            Pair(null, null)
        }
    }

    fun bitmapToBase64(bitmap: Bitmap): Pair<String, String> {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
        val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        return Pair(base64, "image/jpeg")
    }

    fun triggerHapticFeedback() {
        hapticManager.triggerTapHaptic()
    }

    fun triggerStartListeningHaptic() {
        hapticManager.triggerStartListeningHaptic()
    }

    fun triggerProcessingInputHaptic() {
        hapticManager.triggerProcessingInputHaptic()
    }

    fun triggerCommandConfirmedHaptic() {
        hapticManager.triggerCommandConfirmedHaptic()
    }

    fun triggerEmergencyAlertHaptic() {
        hapticManager.triggerEmergencyAlertHaptic()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-visibility notifications for AssistAI reminders, alarms, and appointments"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun sendNotificationAlert(title: String, message: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify((System.currentTimeMillis() % 10000).toInt(), notification)
        } catch (_: Exception) {
        }
    }

    fun shutdown() {
        sttHandler.cancelAndDestroy()
        stopAudioFileRecording()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
    }
}
