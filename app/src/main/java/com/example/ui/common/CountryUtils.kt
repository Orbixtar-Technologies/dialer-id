package com.example.ui.common

data class CountryInfo(
    val code: String,
    val name: String,
    val dialCode: String,
    val flagEmoji: String,
    val ratePerMin: Double
)

object CountryUtils {
    val COUNTRIES = listOf(
        CountryInfo("US", "United States", "+1", "🇺🇸", 0.015),
        CountryInfo("CA", "Canada", "+1", "🇨🇦", 0.015),
        CountryInfo("GB", "United Kingdom", "+44", "🇬🇧", 0.025),
        CountryInfo("AU", "Australia", "+61", "🇦🇺", 0.035),
        CountryInfo("DE", "Germany", "+49", "🇩🇪", 0.028),
        CountryInfo("FR", "France", "+33", "🇫🇷", 0.028),
        CountryInfo("JP", "Japan", "+81", "🇯🇵", 0.040),
        CountryInfo("IN", "India", "+91", "🇮🇳", 0.020),
        CountryInfo("BR", "Brazil", "+55", "🇧🇷", 0.045),
        CountryInfo("MX", "Mexico", "+52", "🇲🇽", 0.030),
        CountryInfo("AE", "United Arab Emirates", "+971", "🇦🇪", 0.085),
        CountryInfo("SG", "Singapore", "+65", "🇸🇬", 0.025)
    )

    fun getCountryByDialCode(dialCode: String): CountryInfo {
        return COUNTRIES.find { it.dialCode == dialCode } ?: COUNTRIES.first()
    }

    fun estimateRateForNumber(number: String): Pair<CountryInfo, Double> {
        val clean = number.trim()
        if (clean == "3200" || clean == "444") {
            return Pair(CountryInfo("TEST", "Diagnostic Line", "", "🛠️", 0.00), 0.00)
        }
        for (c in COUNTRIES) {
            if (clean.startsWith(c.dialCode)) {
                return Pair(c, c.ratePerMin)
            }
        }
        return Pair(COUNTRIES.first(), 0.020)
    }

    fun formatPhoneNumber(input: String): String {
        val digits = input.filter { it.isDigit() || it == '+' }
        if (digits.startsWith("+1") && digits.length > 2) {
            val rest = digits.substring(2)
            return when {
                rest.length <= 3 -> "+1 ($rest"
                rest.length <= 6 -> "+1 (${rest.substring(0, 3)}) ${rest.substring(3)}"
                else -> "+1 (${rest.substring(0, 3)}) ${rest.substring(3, 6)}-${rest.substring(6).take(4)}"
            }
        }
        return input
    }
}
