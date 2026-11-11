package com.bungbung.diary.domain.destination

import com.bungbung.diary.data.text.DestinationTalkGenerator
import com.bungbung.diary.domain.LlmClient
import com.bungbung.diary.domain.Tone
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DestinationThoughtServiceTest {

    /** 호출 기록을 남기는 Fake LLM */
    private class FakeLlm(
        private val response: String?,
        private val delayMs: Long = 0,
    ) : LlmClient {
        var called = false
        var lastPrompt: String? = null
        override suspend fun generate(prompt: String): String? {
            called = true
            lastPrompt = prompt
            if (delayMs > 0) delay(delayMs)
            return response
        }
    }

    private fun thought(service: DestinationThoughtService, name: String) =
        runBlocking { service.thought(name, Tone.FRIEND, "붕붕이") }

    // ── 안전 경계 ──────────────────────────────────────────────────────────

    @Test fun `민감 상황에서는 LLM을 호출조차 하지 않는다`() {
        val llm = FakeLlm("절대 들리면 안 되는 문장")
        val service = DestinationThoughtService(llm)
        for (name in listOf("서울아산병원 장례식장", "세브란스병원 응급진료센터", "서울대학교병원")) {
            val t = thought(service, name)
            assertFalse(llm.called, "$name 에서 LLM이 호출됨")
            assertFalse(t.fromLlm)
            assertTrue(
                t.message in DestinationTalkGenerator.templates(t.category, Tone.FRIEND, "붕붕이", name),
                "민감 상황 문장은 템플릿 원문 중 하나와 글자 하나까지 같아야 한다",
            )
        }
    }

    @Test fun `LLM이 없으면 템플릿으로 동작한다`() {
        val t = thought(DestinationThoughtService(llm = null), "인천국제공항 제2여객터미널")
        assertEquals(DestinationCategory.AIRPORT, t.category)
        assertFalse(t.fromLlm)
        assertTrue(t.message.isNotBlank())
    }

    // ── LLM 성공 경로 ──────────────────────────────────────────────────────

    @Test fun `비민감 상황에서 정상 응답이면 LLM 문장을 쓴다`() {
        val llm = FakeLlm("하늘을 나는 건 언제나 설레는 일이야. 좋은 여행이 되길!")
        val t = thought(DestinationThoughtService(llm), "인천국제공항 제2여객터미널")
        assertTrue(llm.called)
        assertTrue(t.fromLlm)
        assertEquals("하늘을 나는 건 언제나 설레는 일이야. 좋은 여행이 되길!", t.message)
    }

    @Test fun `프롬프트에는 목적지 이름만 들어간다 - 좌표와 주소 미전송`() {
        val llm = FakeLlm("응답")
        thought(DestinationThoughtService(llm), "라비두스 웨딩홀")
        val prompt = llm.lastPrompt!!
        assertTrue(prompt.contains("라비두스 웨딩홀"))
        assertTrue(prompt.contains("붕붕이"))
    }

    @Test fun `따옴표와 줄바꿈은 정리한다`() {
        val llm = FakeLlm("\"떠나는 길은\n언제나 설렌다\"")
        val t = thought(DestinationThoughtService(llm), "제주국제공항")
        assertTrue(t.fromLlm)
        assertEquals("떠나는 길은 언제나 설렌다", t.message)
    }

    // ── LLM 검증 실패 → 템플릿 fallback ───────────────────────────────────

    @Test fun `빈 응답이면 템플릿으로`() {
        val t = thought(DestinationThoughtService(FakeLlm("   ")), "김포공항 국내선")
        assertFalse(t.fromLlm)
    }

    @Test fun `null 응답이면 템플릿으로`() {
        val t = thought(DestinationThoughtService(FakeLlm(null)), "김포공항 국내선")
        assertFalse(t.fromLlm)
    }

    @Test fun `80자를 넘으면 템플릿으로`() {
        val t = thought(DestinationThoughtService(FakeLlm("가".repeat(81))), "김포공항 국내선")
        assertFalse(t.fromLlm)
    }

    @Test fun `목적지 이름에 없는 숫자를 지어내면 템플릿으로`() {
        val t = thought(DestinationThoughtService(FakeLlm("3번 게이트로 가면 15분 아껴져")), "김포공항 국내선")
        assertFalse(t.fromLlm)
    }

    @Test fun `목적지 이름에 있는 숫자는 허용한다`() {
        val t = thought(DestinationThoughtService(FakeLlm("제2터미널로 가는 길, 설레는 출발이야")), "인천국제공항 제2여객터미널")
        assertTrue(t.fromLlm)
    }

    @Test fun `타임아웃이 지나면 템플릿으로`() {
        val llm = FakeLlm("늦은 응답", delayMs = DestinationThoughtService.LLM_TIMEOUT_MS + 1_000)
        val t = thought(DestinationThoughtService(llm), "김포공항 국내선")
        assertFalse(t.fromLlm)
    }

    // ── 표정 매핑 ──────────────────────────────────────────────────────────

    @Test fun `카테고리별 표정이 정해져 있다`() {
        assertEquals(com.bungbung.diary.domain.Mood.CALM, DestinationCategory.FUNERAL.mood)
        assertEquals(com.bungbung.diary.domain.Mood.WORRIED, DestinationCategory.HOSPITAL_ER.mood)
        assertEquals(com.bungbung.diary.domain.Mood.CELEBRATE, DestinationCategory.WEDDING.mood)
        assertEquals(com.bungbung.diary.domain.Mood.LOVE, DestinationCategory.AIRPORT.mood)
        assertEquals(com.bungbung.diary.domain.Mood.HUNGRY, DestinationCategory.FOOD.mood)
        assertEquals(com.bungbung.diary.domain.Mood.CALM, DestinationCategory.NATURE.mood)
    }
}
