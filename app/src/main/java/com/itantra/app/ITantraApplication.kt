package com.itantra.app

import android.app.Application
import com.itantra.app.core.localization.LanguageManager
import com.itantra.app.core.logging.ITantraLogger

/**
 * iTantra Application class.
 *
 * Initializes core singletons that survive configuration changes:
 *  - LanguageManager (language preference persistence)
 *  - Logging configuration
 *  - Offline Speech & Voice engines (TTS, VAD, STT)
 *  - Offline Local Transports (Wi-Fi Direct TCP socket + Bluetooth Classic RFCOMM fallback)
 *  - Unified ConnectionManager
 *  - Local Room Database & MessageRepository for complete conversation & alert history
 */
class ITantraApplication : Application() {

    lateinit var languageManager: LanguageManager
        private set

    val ttsEngine: com.itantra.app.voice.tts.AndroidTTSEngine by lazy {
        com.itantra.app.voice.tts.AndroidTTSEngine(this)
    }

    val audioCaptureManager: com.itantra.app.voice.audio.AndroidAudioCaptureManager by lazy {
        com.itantra.app.voice.audio.AndroidAudioCaptureManager(this)
    }

    val vadEngine: com.itantra.app.voice.vad.TFLiteNeuralVADEngine by lazy {
        com.itantra.app.voice.vad.TFLiteNeuralVADEngine(this)
    }

    val acousticFeatureEngine: com.itantra.app.voice.stt.PyTorchAcousticFeatureEngine by lazy {
        com.itantra.app.voice.stt.PyTorchAcousticFeatureEngine(this)
    }

    val sttEngine: com.itantra.app.voice.stt.OfflineSTTEngineImpl by lazy {
        com.itantra.app.voice.stt.OfflineSTTEngineImpl(this)
    }

    val wifiTransport: com.itantra.app.communication.wifi.WifiTransport by lazy {
        com.itantra.app.communication.wifi.WifiTransport(this)
    }

    val bluetoothTransport: com.itantra.app.communication.bluetooth.BluetoothTransport by lazy {
        com.itantra.app.communication.bluetooth.BluetoothTransport(this)
    }

    val connectionManager: com.itantra.app.communication.transport.ConnectionManager by lazy {
        com.itantra.app.communication.transport.ConnectionManager(
            wifiTransport = wifiTransport,
            bluetoothTransport = bluetoothTransport
        )
    }

    val database: com.itantra.app.data.local.ITantraDatabase by lazy {
        com.itantra.app.data.local.ITantraDatabase.getInstance(this)
    }

    val messageRepository: com.itantra.app.domain.repository.MessageRepository by lazy {
        com.itantra.app.data.repository.MessageRepositoryImpl(database.messageDao())
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize core managers
        languageManager = LanguageManager(this)

        // Configure logging for build type
        ITantraLogger.isDebugEnabled = BuildConfig.DEBUG
        ITantraLogger.i("App", "iTantra initialized | debug=${BuildConfig.DEBUG}")
    }

    companion object {
        lateinit var instance: ITantraApplication
            private set
    }
}
