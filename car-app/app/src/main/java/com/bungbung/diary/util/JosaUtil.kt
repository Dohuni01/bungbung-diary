package com.bungbung.diary.util

object JosaUtil {
    fun ga(word: String): String  = if (hasFinalConsonant(word)) "${word}이" else "${word}가"
    fun eul(word: String): String = if (hasFinalConsonant(word)) "${word}을" else "${word}를"
    fun eun(word: String): String = if (hasFinalConsonant(word)) "${word}은" else "${word}는"
    fun ah(word: String): String  = if (hasFinalConsonant(word)) "${word}아" else "${word}야"
    fun iya(word: String): String = if (hasFinalConsonant(word)) "${word}이야" else "${word}야"

    private fun hasFinalConsonant(word: String): Boolean {
        if (word.isEmpty()) return false
        val last = word.last()
        if (last.code < 0xAC00 || last.code > 0xD7A3) return false
        return (last.code - 0xAC00) % 28 != 0
    }
}
