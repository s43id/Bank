package com.banktracker.util

import java.util.Calendar

object JalaliUtil {

    fun gregorianToJalali(year: Int, month: Int, day: Int): Triple<Int, Int, Int> {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        val gy = year - 1600
        val gm = month - 1
        val gd = day - 1

        var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
        for (i in 0 until gm) gDayNo += gDaysInMonth[i]
        if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0)) gDayNo++
        gDayNo += gd

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var i = 0
        while (i < 11 && jDayNo >= jDaysInMonth[i]) {
            jDayNo -= jDaysInMonth[i]
            i++
        }

        return Triple(jy, i + 1, jDayNo + 1)
    }

    fun todayJalali(): Triple<Int, Int, Int> {
        val cal = Calendar.getInstance()
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun yesterdayJalali(): Triple<Int, Int, Int> {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun toDateString(t: Triple<Int, Int, Int>): String =
        "${t.first}/${t.second.toString().padStart(2, '0')}/${t.third.toString().padStart(2, '0')}"

    fun timestampToJalaliDate(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return toDateString(
            gregorianToJalali(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        )
    }

    fun toPrettyDate(dateStr: String): String {
        val parts = dateStr.split("/")
        if (parts.size != 3) return dateStr
        val year = parts[0].toIntOrNull() ?: return dateStr
        val month = parts[1].toIntOrNull() ?: return dateStr
        val day = parts[2].toIntOrNull() ?: return dateStr
        val monthNames = listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
        )
        val monthName = if (month in 1..12) monthNames[month - 1] else ""
        return "$day $monthName $year"
    }

    fun formatAmount(amount: Long): String = String.format("%,d", amount)
}
