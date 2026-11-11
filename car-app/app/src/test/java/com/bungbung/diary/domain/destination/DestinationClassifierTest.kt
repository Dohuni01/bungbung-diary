package com.bungbung.diary.domain.destination

import com.bungbung.diary.domain.destination.DestinationCategory.AIRPORT
import com.bungbung.diary.domain.destination.DestinationCategory.CULTURE
import com.bungbung.diary.domain.destination.DestinationCategory.EXAM_INTERVIEW
import com.bungbung.diary.domain.destination.DestinationCategory.FOOD
import com.bungbung.diary.domain.destination.DestinationCategory.FUNERAL
import com.bungbung.diary.domain.destination.DestinationCategory.GYM
import com.bungbung.diary.domain.destination.DestinationCategory.HOME
import com.bungbung.diary.domain.destination.DestinationCategory.HOSPITAL
import com.bungbung.diary.domain.destination.DestinationCategory.HOSPITAL_ER
import com.bungbung.diary.domain.destination.DestinationCategory.NATURE
import com.bungbung.diary.domain.destination.DestinationCategory.NONE
import com.bungbung.diary.domain.destination.DestinationCategory.PLAY
import com.bungbung.diary.domain.destination.DestinationCategory.SCHOOL
import com.bungbung.diary.domain.destination.DestinationCategory.SHOPPING
import com.bungbung.diary.domain.destination.DestinationCategory.WEDDING
import com.bungbung.diary.domain.destination.DestinationCategory.WORK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** 향하다 PROMPT 부록 B 골든 케이스 포팅 */
class DestinationClassifierTest {

    private fun expect(name: String, category: DestinationCategory) =
        assertEquals(category, DestinationClassifier.classify(name), name)

    @Test fun `병원과 장례식장이 같이 있으면 장례식장이 이긴다`() {
        expect("서울아산병원 장례식장", FUNERAL)
        expect("삼성서울병원 장례식장 주차장", FUNERAL)
    }

    @Test fun `장례 관련 시설`() {
        expect("서울시립승화원", FUNERAL)
        expect("분당메모리얼파크", FUNERAL)
        expect("OO장례문화원", FUNERAL)
    }

    @Test fun `응급실은 병원보다 먼저`() {
        expect("세브란스병원 응급진료센터", HOSPITAL_ER)
        expect("서울대학교병원 권역응급의료센터", HOSPITAL_ER)
    }

    @Test fun `일반 병원`() {
        expect("서울대학교병원", HOSPITAL)
        expect("국립중앙의료원", HOSPITAL)
        expect("OO병원 주차장", HOSPITAL)
    }

    @Test fun `동물병원과 동네 의원은 묻지 않는다`() {
        expect("행복동물병원", NONE)
        expect("OO내과의원", NONE)
        expect("OO치과", NONE)
        expect("OO한의원", NONE)
    }

    @Test fun `공항`() {
        expect("인천국제공항 제2여객터미널", AIRPORT)
        expect("인천공항 장기주차장", AIRPORT)
        expect("김포공항 국내선", AIRPORT)
        expect("제주국제공항", AIRPORT)
    }

    @Test fun `공항 제외어 - 동네 이름, 역, 정류장`() {
        expect("공항동 주민센터", NONE)
        expect("김포공항역", NONE)
        expect("공항리무진 정류장", NONE)
    }

    @Test fun `프랜차이즈 지점은 공항·병원·결혼식·학교가 아니다`() {
        expect("BHC치킨 서울공항점", FOOD)       // 공항 아님, 치킨집
        expect("스타벅스 김포공항점", NONE)       // 공항 아님 (브랜드명은 키워드에 없음)
        expect("스타벅스 서울대학교병원점", NONE) // 병원도 학교도 아님
        expect("파리바게뜨 OO예식장점", NONE)     // 예식장 아님
    }

    @Test fun `집과 회사 - 네비 즐겨찾기 이름은 정확히 일치할 때만`() {
        expect("집", HOME)
        expect("우리집", HOME)
        expect("본가", HOME)
        expect("맛집거리", FOOD) // '집'이 들어있다고 HOME이 되면 안 됨 (정확 일치만) — 맛집이라 FOOD
        expect("회사", WORK)
        expect("OO빌딩 사무실", WORK)
    }

    @Test fun `학교`() {
        expect("OO고등학교", SCHOOL)
        expect("서울대학교", SCHOOL)
        expect("서울대학교병원", HOSPITAL) // 병원이 학교보다 우선
    }

    @Test fun `놀이공원과 문화생활`() {
        expect("에버랜드", PLAY)
        expect("서울랜드 놀이공원", PLAY)
        expect("롯데월드타워", NONE)       // 타워는 놀이공원이 아님
        expect("CGV 용산아이파크몰", CULTURE)
        expect("국립중앙박물관", CULTURE)
        expect("예술의전당 공연장", CULTURE)
    }

    @Test fun `쇼핑과 운동과 맛집`() {
        expect("신세계백화점 강남점", SHOPPING) // 백화점은 '점'으로 끝나도 가게가 맞음
        expect("이마트 성수점", SHOPPING)
        expect("강남 헬스장", GYM)
        expect("OO수영장", GYM)
        expect("교촌치킨 역삼점", FOOD)
        expect("성수동 맛집", FOOD)
    }

    @Test fun `자연`() {
        expect("경포해변", NATURE)
        expect("설악산 국립공원", NATURE)
        expect("서울숲 공원", NATURE)
        expect("분당추모공원", FUNERAL) // 공원이지만 추모공원은 장례가 우선
    }

    @Test fun `결혼식장`() {
        expect("라비두스 웨딩홀", WEDDING)
        expect("OO예식장", WEDDING)
    }

    @Test fun `웨딩 제외어 - 드레스샵, 컨벤션센터`() {
        expect("OO웨딩드레스", NONE)
        expect("코엑스 컨벤션센터", NONE)
    }

    @Test fun `시험장과 면접장`() {
        expect("서울남부운전면허시험장", EXAM_INTERVIEW)
        expect("한국산업인력공단 OO상설시험장", EXAM_INTERVIEW)
    }

    @Test fun `애매한 이름은 전부 NONE`() {
        expect("스타벅스 강남역점", NONE)
        expect("롯데월드타워", NONE)
        expect("더채플앳청담", NONE) // 키워드 없음 — 규칙만으로는 NONE
    }

    @Test fun `빈 문자열은 NONE`() {
        expect("", NONE)
        expect("   ", NONE)
    }

    @Test fun `정규화 - 공백과 특수문자 제거, 영문 소문자`() {
        assertEquals("인천국제공항t2", DestinationClassifier.normalize("인천국제공항 (T2)"))
    }

    @Test fun `민감 카테고리 지정이 맞다`() {
        assertEquals(true, FUNERAL.sensitive)
        assertEquals(true, HOSPITAL_ER.sensitive)
        assertEquals(true, HOSPITAL.sensitive)
        assertEquals(false, AIRPORT.sensitive)
        assertEquals(false, WEDDING.sensitive)
        assertEquals(false, EXAM_INTERVIEW.sensitive)
        assertEquals(false, NONE.sensitive)
    }
}
