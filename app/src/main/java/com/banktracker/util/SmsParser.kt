package com.banktracker.util

object SmsParser {

    private val depositKeywords = listOf(
        "واریز", "وارد شد", "افزایش موجودی", "دریافت وجه",
        "واریزی", "شارژ حساب", "به حساب شما", "credit", "Credit",
        "سپرده", "واریز به"
    )

    private val withdrawalKeywords = listOf(
        "برداشت", "پرداخت موفق", "خرید از", "کسر شد",
        "کاهش موجودی", "بدهکار", "debit", "Debit",
        "پرداخت اینترنتی", "انتقال وجه از", "خروج وجه"
    )

    // Returns Pair(amount in Rials, type) or null if message not parseable
    fun parse(message: String): Pair<Long, String>? {
        val normalized = normalizeDigits(message)
        val amount = extractAmount(normalized) ?: return null
        val type = detectType(message)
        return Pair(amount, type)
    }

    private fun normalizeDigits(text: String): String = text
        .replace('۰', '0').replace('۱', '1').replace('۲', '2')
        .replace('۳', '3').replace('۴', '4').replace('۵', '5')
        .replace('۶', '6').replace('۷', '7').replace('۸', '8')
        .replace('۹', '9')

    private fun extractAmount(normalized: String): Long? {
        val patterns = listOf(
            Regex("""([\d,]+)\s*ریال"""),
            Regex("""([\d,]+)\s*تومان"""),
            Regex("""مبلغ\s*:?\s*([\d,]+)"""),
            Regex("""مبلغ\s+\(ریال\)\s*:?\s*([\d,]+)"""),
            Regex("""([\d,]{6,})""")   // fallback: any 6+ digit number
        )

        for (pattern in patterns) {
            val match = pattern.find(normalized) ?: continue
            val raw = match.groupValues[1].replace(",", "")
            val amount = raw.toLongOrNull()
            if (amount != null && amount >= 1000) return amount
        }
        return null
    }

    private fun detectType(message: String): String {
        var depositScore = 0
        var withdrawalScore = 0

        for (keyword in depositKeywords) {
            if (message.contains(keyword, ignoreCase = true)) depositScore++
        }
        for (keyword in withdrawalKeywords) {
            if (message.contains(keyword, ignoreCase = true)) withdrawalScore++
        }

        return when {
            depositScore > withdrawalScore -> "DEPOSIT"
            withdrawalScore > depositScore -> "WITHDRAWAL"
            depositScore > 0 -> "DEPOSIT"
            withdrawalScore > 0 -> "WITHDRAWAL"
            else -> "UNKNOWN"
        }
    }
}
