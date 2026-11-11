package com.bungbung.diary.data.llm

import ai.pleos.playground.llm.LLM
import ai.pleos.playground.llm.constant.Mode
import ai.pleos.playground.llm.data.ModelParameter
import ai.pleos.playground.llm.listener.OnServerConnectionListener
import ai.pleos.playground.llm.listener.ResultListener
import android.content.Context
import android.util.Log
import com.bungbung.diary.domain.LlmClient
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Pleos LLM SDK 래핑 (SERVER 모드).
 *
 * API는 LLM AAR 2.1.3.2에서 javap으로 확인 (2026-11-11).
 * https://document.pleos.ai/en/api-reference/connect-sdk-pleos/LLM/make-the-first-api-call
 *
 * - 자격 증명이 비어 있거나(미발급) 서버 연결 전이면 generate()는 null — 호출자가 템플릿으로 대체.
 * - 타임아웃은 호출자(DestinationThoughtService)의 withTimeoutOrNull이 담당한다.
 */
class PleosLlmClient(
    private val context: Context,
    private val clientId: String,
    private val clientSecret: String,
) : LlmClient {

    private var llm: LLM? = null
    @Volatile private var connected = false

    fun start() {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            Log.i(TAG, "자격 증명 없음 — LLM 비활성 (템플릿만 사용)")
            return
        }
        // 에뮬레이터 확인 결과(2026-11-11): ON_DEVICE는 "on-device LLM is disabled",
        // SERVER는 registerApp까지 성공하나 generateContent가 HTTP 400 (Pleos 쪽 미개방 추정).
        // SERVER 모드로 두면 서비스가 열리는 즉시 동작하고, 그 전엔 타임아웃 → 템플릿 fallback.
        try {
            llm = LLM(context, Mode.SERVER).also { l ->
                l.initialize()
                l.registerApp(clientId, clientSecret, object : OnServerConnectionListener {
                    override fun onConnected() {
                        Log.i(TAG, "LLM server connected")
                        // 문서 흐름: registerApp → setModelParameter(MAX_TOKENS) → generateContent
                        // https://document.pleos.ai/en/api-reference/connect-sdk-pleos/LLM/make-the-first-api-call
                        l.setModelParameter(ModelParameter.MAX_TOKENS, "256", object : ResultListener {
                            override fun onResponse(response: String, completed: Boolean) {
                                Log.i(TAG, "MAX_TOKENS 설정 완료")
                            }
                            override fun onError(reason: String) {
                                Log.w(TAG, "MAX_TOKENS 설정 실패: $reason")
                            }
                        })
                        connected = true
                    }
                    override fun onFailed(reason: String) {
                        Log.w(TAG, "LLM registerApp 실패: $reason")
                    }
                })
            }
        } catch (e: Exception) {
            Log.w(TAG, "LLM unavailable: ${e.message}")
        }
    }

    override suspend fun generate(prompt: String): String? {
        val l = llm ?: return null
        if (!connected) return null
        return suspendCancellableCoroutine { cont ->
            val buffer = StringBuilder()
            try {
                l.generateContent(prompt, object : ResultListener {
                    override fun onResponse(response: String, completed: Boolean) {
                        buffer.append(response)
                        if (completed && cont.isActive) cont.resume(buffer.toString())
                    }
                    override fun onError(reason: String) {
                        Log.w(TAG, "LLM 응답 오류: $reason")
                        if (cont.isActive) cont.resume(null)
                    }
                })
            } catch (e: Exception) {
                Log.w(TAG, "LLM 호출 실패: ${e.message}")
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    fun release() {
        try {
            llm?.release()
        } catch (e: Exception) {
            Log.w(TAG, "LLM release 실패: ${e.message}")
        }
        llm = null
        connected = false
    }

    companion object {
        private const val TAG = "PleosLlmClient"
    }
}
