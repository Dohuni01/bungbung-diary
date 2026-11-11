package com.bungbung.diary.domain.destination

import com.bungbung.diary.domain.Mood

/**
 * 목적지로 알아채는 이동의 상황 (설계: docs/superpowers/specs/2026-11-11-destination-talk-design.md)
 *
 * sensitive = true인 상황(장례식장·응급실·병원)은 사람이 검증한 고정 문장만 쓰고
 * LLM을 절대 쓰지 않는다. 코드 구조(DestinationThoughtService)와 테스트로 보장.
 */
enum class DestinationCategory(
    val label: String,
    val sensitive: Boolean,
    val mood: Mood,
) {
    // 민감 — 차를 대하는 태도가 중요한 상황
    FUNERAL("장례식장 가는 길", sensitive = true, mood = Mood.CALM),
    HOSPITAL_ER("응급실 가는 길", sensitive = true, mood = Mood.WORRIED),
    HOSPITAL("병원 가는 길", sensitive = true, mood = Mood.WORRIED),

    // 비민감 — 특별한 날
    AIRPORT("공항 가는 길", sensitive = false, mood = Mood.LOVE),
    WEDDING("결혼식 가는 길", sensitive = false, mood = Mood.CELEBRATE),
    EXAM_INTERVIEW("시험·면접 가는 길", sensitive = false, mood = Mood.FOCUS),

    // 비민감 — 일상
    HOME("집으로 가는 길", sensitive = false, mood = Mood.HAPPY),
    WORK("일하러 가는 길", sensitive = false, mood = Mood.FOCUS),
    SCHOOL("학교 가는 길", sensitive = false, mood = Mood.HAPPY),
    PLAY("놀러 가는 길", sensitive = false, mood = Mood.CELEBRATE),
    CULTURE("문화생활 가는 길", sensitive = false, mood = Mood.HAPPY),
    SHOPPING("쇼핑 가는 길", sensitive = false, mood = Mood.HAPPY),
    GYM("운동 가는 길", sensitive = false, mood = Mood.FOCUS),
    FOOD("맛집 가는 길", sensitive = false, mood = Mood.HUNGRY),
    NATURE("자연으로 가는 길", sensitive = false, mood = Mood.CALM),

    NONE("가는 길", sensitive = false, mood = Mood.HAPPY),
}
