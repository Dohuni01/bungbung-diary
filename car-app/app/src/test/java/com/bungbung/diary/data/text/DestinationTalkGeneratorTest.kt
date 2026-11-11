package com.bungbung.diary.data.text

import com.bungbung.diary.domain.Tone
import com.bungbung.diary.domain.destination.DestinationCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class DestinationTalkGeneratorTest {

    @Test fun `모든 카테고리와 말투 조합에 문장이 여러 개 있고 전부 80자 이내다`() {
        for (category in DestinationCategory.entries) {
            for (tone in Tone.entries) {
                val list = DestinationTalkGenerator.templates(category, tone, "붕붕이", "테스트 목적지")
                assertTrue(list.size >= 3, "$category x $tone 문장이 ${list.size}개뿐")
                for (msg in list) {
                    assertTrue(msg.isNotBlank(), "$category x $tone 에 빈 문장")
                    assertTrue(msg.length <= 80, "$category x $tone 80자 초과: $msg")
                }
            }
        }
    }

    @Test fun `talk는 템플릿 목록 안에서 고른다`() {
        val random = Random(42)
        repeat(20) {
            val msg = DestinationTalkGenerator.talk(
                DestinationCategory.AIRPORT, Tone.FRIEND, "붕붕이", "인천국제공항", random,
            )
            val list = DestinationTalkGenerator.templates(
                DestinationCategory.AIRPORT, Tone.FRIEND, "붕붕이", "인천국제공항",
            )
            assertTrue(msg in list)
        }
    }

    @Test fun `민감 상황 문장군은 말투와 무관하게 같다`() {
        for (category in DestinationCategory.entries.filter { it.sensitive }) {
            val lists = Tone.entries.map {
                DestinationTalkGenerator.templates(category, it, "붕붕이", "어딘가")
            }.toSet()
            assertEquals(1, lists.size, "$category 문장군이 말투에 따라 달라짐")
        }
    }

    @Test fun `일반 목적지 문장에는 항상 목적지 이름이 들어간다`() {
        for (tone in Tone.entries) {
            val list = DestinationTalkGenerator.templates(DestinationCategory.NONE, tone, "붕붕이", "경포해변")
            for (msg in list) {
                assertTrue(msg.contains("경포해변"), "$tone 문장에 이름 없음: $msg")
            }
        }
    }
}
