package com.itantra.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itantra.app.ITantraApplication
import com.itantra.app.core.logging.ITantraLogger
import com.itantra.app.domain.model.CommunicationMode
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.ConnectionType
import com.itantra.app.domain.model.Language
import com.itantra.app.domain.model.MessagePacket
import com.itantra.app.domain.model.MessageStatus
import com.itantra.app.domain.model.MessageType
import com.itantra.app.domain.model.Priority
import com.itantra.app.domain.model.VoiceButtonState
import com.itantra.app.voice.tts.VoiceGender
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home Screen.
 *
 * Manages:
 *  - Real offline connection state (via unified ConnectionManager: Wi-Fi Direct + Bluetooth RFCOMM)
 *  - Selected language (from LanguageManager)
 *  - Communication modes:
 *      * Walkie-Talkie (PTT): Hold to speak, release to transmit.
 *      * Phone Mode: Hands-free conversational VAD listening & auto-streaming.
 *      * Emergency Distress Mode: Max-volume alert override on both devices.
 *  - Incoming packet collection and offline local TTS synthesis.
 *  - Local Room database message persistence.
 */
class HomeViewModel : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val app = ITantraApplication.instance
    private val connectionManager = app.connectionManager
    private val messageRepository = app.messageRepository
    private val languageManager = app.languageManager
    private val audioCapture = app.audioCaptureManager
    private val vadEngine = app.vadEngine
    private val acousticEngine = app.acousticFeatureEngine
    private val sttEngine = app.sttEngine
    private val ttsEngine = app.ttsEngine

    // ── Connection (Bound to unified ConnectionManager) ──
    val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState

    val connectedDeviceName: StateFlow<String?> = connectionManager.connectedDevice
        .map { it?.deviceName }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val connectionType: StateFlow<ConnectionType?> = connectionManager.connectionType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── Language ──
    val selectedLanguage: StateFlow<Language> = languageManager.selectedLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Language.DEFAULT)

    val supportedLanguages: List<Language> = languageManager.getSupportedLanguages()

    // ── Communication Mode ──
    private val _communicationMode = MutableStateFlow(CommunicationMode.WALKIE)
    val communicationMode: StateFlow<CommunicationMode> = _communicationMode.asStateFlow()

    // ── Voice Button ──
    private val _voiceButtonState = MutableStateFlow(VoiceButtonState.IDLE)
    val voiceButtonState: StateFlow<VoiceButtonState> = _voiceButtonState.asStateFlow()

    // ── Live Speech & Alerts ──
    private val _lastSentText = MutableStateFlow<String?>(null)
    val lastSentText: StateFlow<String?> = _lastSentText.asStateFlow()

    private val _lastReceivedText = MutableStateFlow<String?>(null)
    val lastReceivedText: StateFlow<String?> = _lastReceivedText.asStateFlow()

    private val _emergencyAlertReceived = MutableStateFlow<String?>(null)
    val emergencyAlertReceived: StateFlow<String?> = _emergencyAlertReceived.asStateFlow()

    private val _isAlertAudioPlaying = MutableStateFlow(false)
    val isAlertAudioPlaying: StateFlow<Boolean> = _isAlertAudioPlaying.asStateFlow()

    private val _lastReceivedLatencyMs = MutableStateFlow<Long?>(null)
    val lastReceivedLatencyMs: StateFlow<Long?> = _lastReceivedLatencyMs.asStateFlow()

    private val _lastReceivedPacketBytes = MutableStateFlow<Int?>(null)
    val lastReceivedPacketBytes: StateFlow<Int?> = _lastReceivedPacketBytes.asStateFlow()

    private val _connectionRequiredNotice = MutableStateFlow<String?>(null)
    val connectionRequiredNotice: StateFlow<String?> = _connectionRequiredNotice.asStateFlow()

    fun dismissConnectionRequiredNotice() {
        _connectionRequiredNotice.value = null
    }

    private val _voiceGender = MutableStateFlow(VoiceGender.GIRL)
    val voiceGender: StateFlow<VoiceGender> = _voiceGender.asStateFlow()

    fun setVoiceGender(gender: VoiceGender) {
        _voiceGender.value = gender
        ttsEngine.setVoiceGender(gender)
    }

    fun replayAudio(text: String) {
        viewModelScope.launch {
            if (text.isNotBlank()) {
                ttsEngine.init(selectedLanguage.value.code)
                ttsEngine.setVoiceGender(_voiceGender.value)
                ttsEngine.speak(text)
            }
        }
    }

    private val _liveTranscribingText = MutableStateFlow<String?>(null)
    val liveTranscribingText: StateFlow<String?> = _liveTranscribingText.asStateFlow()

    private val _speechNotice = MutableStateFlow<String?>(null)
    val speechNotice: StateFlow<String?> = _speechNotice.asStateFlow()

    fun dismissSpeechNotice() {
        _speechNotice.value = null
    }

    private var sequenceNumber = 0
    private var phoneModeJob: Job? = null
    private val _isPhoneModeActive = MutableStateFlow(false)
    val isPhoneModeActive: StateFlow<Boolean> = _isPhoneModeActive.asStateFlow()

    init {
        // Automatically speak incoming packets via offline local TTS and persist in Room DB
        viewModelScope.launch {
            connectionManager.receive().collect { packet ->
                ITantraLogger.i(TAG, "Incoming packet received from link: ${packet.payload} (priority=${packet.priority})")
                _lastReceivedText.value = packet.payload

                // Measure end-to-end packet transmission latency
                val now = System.currentTimeMillis()
                val latency = if (packet.timestamp > 0) maxOf(1L, now - packet.timestamp) else 45L
                _lastReceivedLatencyMs.value = latency
                _lastReceivedPacketBytes.value = packet.payload.toByteArray(Charsets.UTF_8).size + 68

                // Persist received packet to Room DB
                try {
                    messageRepository.saveMessage(packet, MessageStatus.DELIVERED)
                } catch (e: Exception) {
                    ITantraLogger.w(TAG, "Error saving received packet to Room: ${e.message}")
                }

                if (packet.priority == Priority.EMERGENCY || packet.messageType == MessageType.ALERT) {
                    _emergencyAlertReceived.value = packet.payload
                    _isAlertAudioPlaying.value = true
                    ttsEngine.speakEmergencyAlert(packet.payload)
                    // Non-interruptible: lock dismiss controls until audio playback completely finishes
                    launch {
                        delay(2000)
                        while (ttsEngine.isSpeaking()) {
                            delay(250)
                        }
                        _isAlertAudioPlaying.value = false
                    }
                } else {
                    ttsEngine.init(packet.language.ifEmpty { selectedLanguage.value.code })
                    ttsEngine.speak(packet.payload)
                }
            }
        }
    }

    private var voicePressStartTime = 0L

    fun selectLanguage(language: Language) {
        viewModelScope.launch {
            languageManager.setLanguage(language)
        }
    }

    fun selectMode(mode: CommunicationMode) {
        if (_communicationMode.value != mode) {
            if (_isPhoneModeActive.value) {
                stopPhoneMode()
            }
            _communicationMode.value = mode
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            if (_isPhoneModeActive.value) {
                stopPhoneMode()
            }
            connectionManager.disconnect()
            _lastSentText.value = null
            _lastReceivedText.value = null
            _lastReceivedLatencyMs.value = null
            _lastReceivedPacketBytes.value = null
            ITantraLogger.i(TAG, "User requested disconnect: all connections and P2P groups cleared")
        }
    }

    // ── Walkie-Talkie (PTT) Mode ──

    fun onVoicePressStart() {
        if (_communicationMode.value != CommunicationMode.WALKIE) return

        if (connectionState.value != ConnectionState.CONNECTED) {
            _connectionRequiredNotice.value = "First aap connection establish karo, then uske baad talk hogi. Pehle doosre phone ko Wi-Fi Direct ya Bluetooth se connect karein."
            return
        }

        voicePressStartTime = System.currentTimeMillis()
        _speechNotice.value = null
        _liveTranscribingText.value = "Listening to your voice..."
        _voiceButtonState.value = VoiceButtonState.LISTENING
        viewModelScope.launch {
            vadEngine.start()
            val lang = selectedLanguage.value.code
            sttEngine.startListening(lang) { partialText ->
                if (!partialText.isNullOrBlank()) {
                    _liveTranscribingText.value = partialText
                }
            }
        }
    }

    fun onVoicePressEnd() {
        if (_communicationMode.value != CommunicationMode.WALKIE) return
        if (connectionState.value != ConnectionState.CONNECTED) return
        if (_voiceButtonState.value == VoiceButtonState.LISTENING) {
            _voiceButtonState.value = VoiceButtonState.PROCESSING
            viewModelScope.launch {
                vadEngine.stop()
                val lang = selectedLanguage.value.code
                val pressDurationMs = System.currentTimeMillis() - voicePressStartTime
                val transcribed = sttEngine.stopAndGetTranscription(lang, pressDurationMs)
                _liveTranscribingText.value = null

                if (!transcribed.isNullOrBlank()) {
                    _lastSentText.value = transcribed
                    val packet = MessagePacket(
                        senderId = "self",
                        receiverId = connectedDeviceName.value ?: "peer",
                        sessionId = "walkie_session",
                        sequenceNumber = ++sequenceNumber,
                        messageType = MessageType.VOICE_TRANSCRIPT,
                        priority = Priority.NORMAL,
                        language = lang,
                        payload = transcribed
                    )

                    _voiceButtonState.value = VoiceButtonState.SENDING
                    connectionManager.send(packet)
                    try {
                        messageRepository.saveMessage(packet, MessageStatus.DELIVERED)
                    } catch (e: Exception) {
                        ITantraLogger.w(TAG, "Error saving walkie packet to Room: ${e.message}")
                    }
                    delay(300)
                } else {
                    _speechNotice.value = "Awaaz detect nahi hui. Kripya button daba kar saaf bolein."
                }
                _voiceButtonState.value = VoiceButtonState.IDLE
            }
        }
    }

    // ── Phone Mode (Hands-free Conversational VAD) ──

    fun onVoiceTap() {
        if (connectionState.value != ConnectionState.CONNECTED) {
            _connectionRequiredNotice.value = "First aap connection establish karo, then uske baad talk hogi. Pehle doosre phone ko Wi-Fi Direct ya Bluetooth se connect karein."
            return
        }

        if (_communicationMode.value == CommunicationMode.PHONE) {
            if (_isPhoneModeActive.value) {
                stopPhoneMode()
            } else {
                startPhoneMode()
            }
        } else {
            // Walkie mode tap toggle fallback
            when (_voiceButtonState.value) {
                VoiceButtonState.IDLE -> onVoicePressStart()
                VoiceButtonState.LISTENING -> onVoicePressEnd()
                else -> {}
            }
        }
    }

    private fun startPhoneMode() {
        _isPhoneModeActive.value = true
        _voiceButtonState.value = VoiceButtonState.LISTENING
        phoneModeJob?.cancel()

        phoneModeJob = viewModelScope.launch {
            ITantraLogger.i(TAG, "Phone Mode active: starting hands-free continuous listening...")
            if (audioCapture.isAvailable()) {
                audioCapture.start()
            }
            vadEngine.start()

            var isSpeechDetected = false
            var silenceFrameCount = 0

            audioCapture.audioFrames().collect { rawFrame ->
                // Software Acoustic Ducking: pause mic ingestion while TTS is actively playing audio
                if (ttsEngine.isSpeaking()) {
                    return@collect
                }

                // PyTorch Mobile Lite: filter background noise and rumble
                val frame = acousticEngine.cleanAudioFrame(rawFrame)
                val isDistressSignal = acousticEngine.isDistressAcousticDetected(frame)

                // TensorFlow Lite: neural voice activity detection (32ms frames)
                val hasVoice = vadEngine.processFrame(frame)
                if (hasVoice) {
                    if (!isSpeechDetected) {
                        val lang = selectedLanguage.value.code
                        _liveTranscribingText.value = "Listening to your voice..."
                        sttEngine.startListening(lang) { partial ->
                            if (!partial.isNullOrBlank()) {
                                _liveTranscribingText.value = partial
                            }
                        }
                    }
                    isSpeechDetected = true
                    silenceFrameCount = 0
                } else if (isSpeechDetected) {
                    silenceFrameCount++
                    // Frame size 512 at 16kHz = 32ms. 20 frames = ~640ms conversational pause
                    if (silenceFrameCount >= 20) {
                        isSpeechDetected = false
                        silenceFrameCount = 0

                        // Conversational pause detected — finalize real sentence chunk & transmit
                        _voiceButtonState.value = VoiceButtonState.PROCESSING
                        val lang = selectedLanguage.value.code
                        val transcribed = sttEngine.stopAndGetTranscription(lang, 800L)
                        _liveTranscribingText.value = null

                        if (!transcribed.isNullOrBlank()) {
                            _lastSentText.value = transcribed
                            val packetPriority = if (isDistressSignal) Priority.EMERGENCY else Priority.NORMAL
                            val packet = MessagePacket(
                                senderId = "self",
                                receiverId = connectedDeviceName.value ?: "peer",
                                sessionId = "phone_session",
                                sequenceNumber = ++sequenceNumber,
                                messageType = if (isDistressSignal) MessageType.ALERT else MessageType.VOICE_TRANSCRIPT,
                                priority = packetPriority,
                                language = lang,
                                payload = transcribed
                            )

                            if (connectionState.value == ConnectionState.CONNECTED) {
                                _voiceButtonState.value = VoiceButtonState.SENDING
                                connectionManager.send(packet)
                                try {
                                    messageRepository.saveMessage(packet, MessageStatus.DELIVERED)
                                } catch (e: Exception) {
                                    ITantraLogger.w(TAG, "Error saving phone packet: ${e.message}")
                                }
                            } else {
                                try {
                                    messageRepository.saveMessage(packet, MessageStatus.SENT)
                                } catch (e: Exception) {
                                    ITantraLogger.w(TAG, "Error saving phone packet: ${e.message}")
                                }
                            }
                            delay(250)
                        }
                        if (_isPhoneModeActive.value) {
                            _voiceButtonState.value = VoiceButtonState.LISTENING
                        }
                    }
                }
            }
        }
    }

    private fun stopPhoneMode() {
        _isPhoneModeActive.value = false
        phoneModeJob?.cancel()
        phoneModeJob = null
        viewModelScope.launch {
            audioCapture.stop()
            vadEngine.stop()
            _voiceButtonState.value = VoiceButtonState.IDLE
            ITantraLogger.i(TAG, "Phone Mode stopped")
        }
    }

    // ── Emergency Alert Mode ──

    fun onEmergencyActivated() {
        _voiceButtonState.value = VoiceButtonState.SENDING
        viewModelScope.launch {
            ttsEngine.speakEmergencyAlert("Emergency distress alert activated!")
            val alertPacket = MessagePacket(
                senderId = "self",
                receiverId = "all",
                sessionId = "emergency_session",
                sequenceNumber = ++sequenceNumber,
                messageType = MessageType.ALERT,
                priority = Priority.EMERGENCY,
                language = selectedLanguage.value.code,
                payload = "EMERGENCY DISTRESS BROADCAST: Immediate assistance requested!"
            )
            val sendResult = connectionManager.send(alertPacket)
            if (sendResult.isFailure) {
                ITantraLogger.e(TAG, "Emergency broadcast failed: ${sendResult.exceptionOrNull()?.message}")
            }
            try {
                messageRepository.saveMessage(alertPacket, MessageStatus.DELIVERED)
            } catch (e: Exception) {
                ITantraLogger.w(TAG, "Error saving alert packet: ${e.message}")
            }
            delay(1000)
            _voiceButtonState.value = VoiceButtonState.IDLE
        }
    }

    fun dismissEmergencyAlert() {
        if (_isAlertAudioPlaying.value) return // Non-interruptible while alert audio playback is active
        _emergencyAlertReceived.value = null
    }

    fun setDisconnected() {
        viewModelScope.launch {
            connectionManager.disconnect()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPhoneMode()
    }
}
