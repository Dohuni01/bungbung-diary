package com.bungbung.diary.data.voice

import android.content.Context
import android.util.Log
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType as SttErrorType
import ai.pleos.playground.stt.constant.Mode as SttMode
import ai.pleos.playground.stt.listener.OnInitializeListener as SttInitListener
import ai.pleos.playground.stt.listener.ResultListener
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType as TtsErrorType
import ai.pleos.playground.tts.constant.Mode as TtsMode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener as TtsInitListener
import java.util.Locale

/**
 * Pleos TTS + STT를 하나의 클래스로 묶는다.
 *
 * - Mode.ON_DEVICE: 서버 등록 없이 기기 내 처리 (에뮬레이터 지원 여부는 실행 시 확인)
 * - 초기화 실패 시 조용히 skip — 에뮬레이터 환경에서는 SDK 자체가 없을 수 있음
 * - speak / startListening 은 초기화 완료 전 호출해도 안전하게 무시됨
 */
class PleosVoiceManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var stt: SpeechToText? = null

    private var ttsReady = false
    private var sttReady = false

    private var pendingSttResult: ((String) -> Unit)? = null

    private val ttsEventListener = object : EventListener {
        override fun onStart() = Unit
        override fun onReady() = Unit
        override fun onDone() = Unit
        override fun onStop() = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onError(errorType: TtsErrorType) {
            Log.w(TAG, "TTS error: $errorType")
        }
    }

    private val sttResultListener = object : ResultListener {
        override fun onUpdated(text: String, completed: Boolean) {
            if (completed && text.isNotBlank()) {
                pendingSttResult?.invoke(text)
            }
        }
        override fun onStartedRecognition() = Unit
        override fun onEndedRecognition() = Unit
        override fun onReady() = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onUpdatedEpdData(p0: Long, p1: Long) = Unit
        override fun onError(errorType: SttErrorType) {
            Log.w(TAG, "STT error: $errorType")
        }
    }

    fun initTts() {
        try {
            tts = TextToSpeech(context, TtsMode.ON_DEVICE).also { t ->
                t.initialize(object : TtsInitListener {
                    override fun onSuccess() {
                        t.setLocale(Locale.KOREA)
                        t.addEventListener(ttsEventListener)
                        ttsReady = true
                        Log.i(TAG, "TTS ready")
                    }
                    override fun onFailure(errorType: TtsErrorType) {
                        Log.w(TAG, "TTS init failed: $errorType")
                    }
                })
            }
        } catch (e: Exception) {
            Log.w(TAG, "TTS unavailable: ${e.message}")
        }
    }

    fun initStt() {
        try {
            stt = SpeechToText(context, SttMode.ON_DEVICE).also { s ->
                s.initialize(object : SttInitListener {
                    override fun onSuccess() {
                        s.setLocale(Locale.KOREA)
                        s.addListener(sttResultListener)
                        sttReady = true
                        Log.i(TAG, "STT ready")
                    }
                    override fun onFailure(errorType: SttErrorType) {
                        Log.w(TAG, "STT init failed: $errorType")
                    }
                })
            }
        } catch (e: Exception) {
            Log.w(TAG, "STT unavailable: ${e.message}")
        }
    }

    fun speak(text: String) {
        if (!ttsReady) return
        tts?.speak(text, true)
    }

    /** 말하는 중이면 멈춘다 ("그만" 버튼, 목적지 해제) */
    fun stopSpeaking() {
        if (!ttsReady) return
        try { tts?.stop() } catch (e: Exception) { Log.w(TAG, "TTS stop 실패: ${e.message}") }
    }

    fun startListening(onResult: (String) -> Unit) {
        if (!sttReady) return
        pendingSttResult = onResult
        stt?.request()
    }

    fun stopListening() {
        stt?.stop()
        pendingSttResult = null
    }

    fun release() {
        tts?.removeEventListener(ttsEventListener)
        tts?.release()
        stt?.removeListener(sttResultListener)
        stt?.release()
        tts = null
        stt = null
        ttsReady = false
        sttReady = false
    }

    companion object {
        private const val TAG = "PleosVoiceManager"
    }
}
