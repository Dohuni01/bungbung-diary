package com.bungbung.diary.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JosaUtilTest {

    // 받침 없는 글자: "이" (U+C774, (C774-AC00)%28 = 0)
    // 받침 있는 글자: "킹" (U+D0B9, (D0B9-AC00)%28 = 21)

    @Test fun `받침 없는 이름에 이_가 → 가`()  = assertEquals("붕붕이가",  JosaUtil.ga("붕붕이"))
    @Test fun `받침 있는 이름에 이_가 → 이`()  = assertEquals("킹이",      JosaUtil.ga("킹"))
    @Test fun `받침 없는 이름에 을_를 → 를`()  = assertEquals("붕붕이를",  JosaUtil.eul("붕붕이"))
    @Test fun `받침 있는 이름에 을_를 → 을`()  = assertEquals("킹을",      JosaUtil.eul("킹"))
    @Test fun `받침 없는 이름에 은_는 → 는`()  = assertEquals("붕붕이는",  JosaUtil.eun("붕붕이"))
    @Test fun `받침 있는 이름에 은_는 → 은`()  = assertEquals("킹은",      JosaUtil.eun("킹"))
    @Test fun `받침 없는 이름에 아_야 → 야`()  = assertEquals("붕붕이야",  JosaUtil.ah("붕붕이"))
    @Test fun `받침 있는 이름에 아_야 → 아`()  = assertEquals("킹아",      JosaUtil.ah("킹"))
    @Test fun `이야_야 받침 없음 → 야`()       = assertEquals("붕붕이야",  JosaUtil.iya("붕붕이"))
    @Test fun `이야_야 받침 있음 → 이야`()     = assertEquals("킹이야",    JosaUtil.iya("킹"))
    @Test fun `빈 문자열은 받침 없음으로 처리`() = assertEquals("가",       JosaUtil.ga(""))
    @Test fun `영어는 받침 없음으로 처리`()     = assertEquals("Car가",     JosaUtil.ga("Car"))
}
