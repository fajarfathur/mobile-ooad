package com.kelompok1.simo.util

import java.text.NumberFormat
import java.util.Locale

/** Format angka ke Rupiah, mis. 1250000 -> "Rp1.250.000". */
object Rupiah {
    private val idLocale = Locale("in", "ID")

    fun format(value: Double): String {
        val nf = NumberFormat.getNumberInstance(idLocale)
        nf.maximumFractionDigits = 0
        return "Rp" + nf.format(value)
    }

    fun format(value: Long): String = format(value.toDouble())
}
