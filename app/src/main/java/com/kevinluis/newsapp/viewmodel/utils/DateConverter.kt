package com.kevinluis.newsapp.viewmodel.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

class DateConverter {

    companion object {
        @SuppressLint("ConstantLocale")
        private val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        private val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))

        init {
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        }

        /**
         * Mengkonversi format tanggal dari ISO 8601 (2025-07-21T00:00:00Z)
         * menjadi format Indonesia (21 Juli 2025)
         */
        fun convertToIndonesianDate(isoDateString: String): String {
            return try {
                val date = inputFormat.parse(isoDateString)
                date?.let { outputFormat.format(it) } ?: "Invalid Date"
            } catch (e: Exception) {
                "Invalid Date"
            }
        }

        /**
         * Alternatif menggunakan modern Java Time API (Android API 26+)
         */
        fun convertToIndonesianDateModern(isoDateString: String): String {
            return try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val instant = java.time.Instant.parse(isoDateString)
                    val localDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()

                    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID"))
                    localDate.format(formatter)
                } else {
                    // Fallback ke method lama untuk API < 26
                    convertToIndonesianDate(isoDateString)
                }
            } catch (e: Exception) {
                "Invalid Date"
            }
        }
    }
}