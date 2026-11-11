package com.bungbung.diary.domain.destination

import com.bungbung.diary.data.text.DestinationTalkGenerator
import com.bungbung.diary.domain.LlmClient
import com.bungbung.diary.domain.Tone
import kotlinx.coroutines.withTimeoutOrNull

/** 목적지를 보고 붕붕이가 하는 한마디. 표정은 category.mood를 쓴다. */
data class DestinationThought(
    val destinationName: String,
    val category: DestinationCategory,
    val message: String,
    /** 로그·디버그 표시용. LLM 문장인지 템플릿 fallback인지 */
    val fromLlm: Boolean,
)

/**
 * 목적지 한마디 결정 (설계 3·5절).
 *
 * 안전 경계:
 * - 민감 카테고리(sensitive)는 LLM을 **호출조차 하지 않고** 고정 문장만 쓴다.
 * - LLM 출력은 검증(빈 문장, 길이, 지어낸 숫자)에 실패하면 버리고 템플릿을 쓴다.
 * - LLM에 보내는 건 목적지 이름·카테고리·말투·닉네임뿐이다.
 */
class DestinationThoughtService(private val llm: LlmClient?) {

    suspend fun thought(destinationName: String, tone: Tone, nickname: String): DestinationThought {
        val category = DestinationClassifier.classify(destinationName)
        val fallback = DestinationTalkGenerator.talk(category, tone, nickname, destinationName)

        if (category.sensitive || llm == null) {
            return DestinationThought(destinationName, category, fallback, fromLlm = false)
        }

        val raw = withTimeoutOrNull(LLM_TIMEOUT_MS) {
            llm.generate(buildPrompt(destinationName, category, tone, nickname))
        }
        val cleaned = raw?.trim()?.removeSurrounding("\"")?.replace('\n', ' ')?.trim()
        return if (isValid(cleaned, destinationName)) {
            DestinationThought(destinationName, category, cleaned!!, fromLlm = true)
        } else {
            DestinationThought(destinationName, category, fallback, fromLlm = false)
        }
    }

    private fun buildPrompt(name: String, category: DestinationCategory, tone: Tone, nickname: String): String = """
        너는 자동차 캐릭터 '$nickname'다. 운전자가 내비게이션 목적지를 '$name'(상황: ${category.label})로 정했다.
        이 이동이 운전자에게 어떤 의미인지 헤아려서, 따뜻하고 철학적인 한마디를 건네라.
        말투: ${tone.label}(${tone.description}).
        규칙: 한 문장, ${MAX_LENGTH}자 이내. 숫자·의료·법률 조언 금지. 따옴표·설명 없이 문장만 출력.
    """.trimIndent()

    /** 빈 문장, 길이 초과, 목적지 이름에 없는 숫자(지어낸 사실 의심)면 버린다. */
    private fun isValid(message: String?, destinationName: String): Boolean {
        if (message.isNullOrBlank()) return false
        if (message.length > MAX_LENGTH) return false
        val allowedDigits = destinationName.filter(Char::isDigit).toSet()
        if (message.any { it.isDigit() && it !in allowedDigits }) return false
        return true
    }

    companion object {
        const val LLM_TIMEOUT_MS = 2_500L
        const val MAX_LENGTH = 80
    }
}
