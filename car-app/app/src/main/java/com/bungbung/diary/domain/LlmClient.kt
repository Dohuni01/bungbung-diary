package com.bungbung.diary.domain

/**
 * LLM 한 번 호출 인터페이스.
 * - Pleos 구현: PleosLlmClient (Phase 4, SERVER 모드)
 * - 실패(미초기화·오프라인·오류)는 예외 대신 null — 호출자가 템플릿으로 대체한다.
 */
interface LlmClient {
    suspend fun generate(prompt: String): String?
}
