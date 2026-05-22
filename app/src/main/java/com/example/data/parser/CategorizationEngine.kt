package com.example.data.parser

import com.example.data.repository.TransactionRepository
import com.example.domain.model.TransactionCategory

object CategorizationEngine {

    private val MERCHANTS_KEYWORDS = mapOf(
        "swiggy" to TransactionCategory.Food,
        "zomato" to TransactionCategory.Food,
        "starbucks" to TransactionCategory.Food,
        "mcdonalds" to TransactionCategory.Food,
        "dominos" to TransactionCategory.Food,
        "kfc" to TransactionCategory.Food,
        "burger king" to TransactionCategory.Food,
        "dine-in" to TransactionCategory.Food,
        "dhaba" to TransactionCategory.Food,
        
        "bigbasket" to TransactionCategory.Grocery,
        "dmart" to TransactionCategory.Grocery,
        "blinkit" to TransactionCategory.Grocery,
        "zepto" to TransactionCategory.Grocery,
        "reliance fresh" to TransactionCategory.Grocery,
        "grocery" to TransactionCategory.Grocery,
        "supermarket" to TransactionCategory.Grocery,
        "spencers" to TransactionCategory.Grocery,
        
        "uber" to TransactionCategory.Travel,
        "ola" to TransactionCategory.Travel,
        "rapido" to TransactionCategory.Travel,
        "irctc" to TransactionCategory.Travel,
        "metro" to TransactionCategory.Travel,
        "railway" to TransactionCategory.Travel,
        "easemytrip" to TransactionCategory.Travel,
        "makemytrip" to TransactionCategory.Travel,
        "indigo" to TransactionCategory.Travel,
        "redbus" to TransactionCategory.Travel,
        
        "flipkart" to TransactionCategory.Shopping,
        "amazon" to TransactionCategory.Shopping,
        "myntra" to TransactionCategory.Shopping,
        "ajio" to TransactionCategory.Shopping,
        "shoppe" to TransactionCategory.Shopping,
        "nykaa" to TransactionCategory.Shopping,
        "decathlon" to TransactionCategory.Shopping,
        "zara" to TransactionCategory.Shopping,
        "h&m" to TransactionCategory.Shopping,
        
        "pvr" to TransactionCategory.Entertainment,
        "inox" to TransactionCategory.Entertainment,
        "bookmyshow" to TransactionCategory.Entertainment,
        "theatre" to TransactionCategory.Entertainment,
        "gaming" to TransactionCategory.Entertainment,
        "disney" to TransactionCategory.Entertainment,
        "poker" to TransactionCategory.Entertainment,
        
        "jio" to TransactionCategory.Recharge,
        "airtel" to TransactionCategory.Recharge,
        "vodaphone" to TransactionCategory.Recharge,
        "idea" to TransactionCategory.Recharge,
        "isps" to TransactionCategory.Recharge,
        "wi-fi" to TransactionCategory.Recharge,
        
        "bescom" to TransactionCategory.Bills,
        "mseb" to TransactionCategory.Bills,
        "tata power" to TransactionCategory.Bills,
        "gas cylinder" to TransactionCategory.Bills,
        "electricity" to TransactionCategory.Bills,
        "water bill" to TransactionCategory.Bills,
        "insurance" to TransactionCategory.Bills,
        "piped gas" to TransactionCategory.Bills,
        
        "apollo" to TransactionCategory.Medical,
        "pharmeasy" to TransactionCategory.Medical,
        "dischem" to TransactionCategory.Medical,
        "hospital" to TransactionCategory.Medical,
        "clinic" to TransactionCategory.Medical,
        "dentist" to TransactionCategory.Medical,
        "lab" to TransactionCategory.Medical,
        "pharmacy" to TransactionCategory.Medical,
        
        "netflix" to TransactionCategory.Subscription,
        "spotify" to TransactionCategory.Subscription,
        "apple" to TransactionCategory.Subscription,
        "youtube premium" to TransactionCategory.Subscription,
        "amazon prime" to TransactionCategory.Subscription,
        "hotstar" to TransactionCategory.Subscription,
        "disney plus" to TransactionCategory.Subscription,
        
        "zerodha" to TransactionCategory.Investment,
        "groww" to TransactionCategory.Investment,
        "angel" to TransactionCategory.Investment,
        "mutual fund" to TransactionCategory.Investment,
        "sip" to TransactionCategory.Investment,
        "shares" to TransactionCategory.Investment,
        "etfs" to TransactionCategory.Investment
    )

    /**
     * Determines the optimal TransactionCategory given a merchant string,
     * incorporating explicit historic user corrections stored in the repository.
     */
    suspend fun categorizeMerchant(merchant: String, repository: TransactionRepository): TransactionCategory {
        val cleanName = merchant.trim().lowercase()
        if (cleanName.isEmpty()) return TransactionCategory.Other

        // 1. Look up user learned preference
        val learnedCatString = repository.getPreferenceValue("learn_merchant_$cleanName")
        if (learnedCatString != null) {
            return TransactionCategory.fromString(learnedCatString)
        }

        // 2. Direct keyword mapping check
        for ((keyword, category) in MERCHANTS_KEYWORDS) {
            if (cleanName.contains(keyword)) {
                return category
            }
        }

        // 3. Fuzzy match fallback
        var closestKeyword: String? = null
        var minDistance = Int.MAX_VALUE
        for (keyword in MERCHANTS_KEYWORDS.keys) {
            val dist = levenshteinDistance(cleanName, keyword)
            if (dist < minDistance && dist <= 2) { // Allow up to 2 character insertions/deletions/replacements
                minDistance = dist
                closestKeyword = keyword
            }
        }

        closestKeyword?.let {
            return MERCHANTS_KEYWORDS[it] ?: TransactionCategory.Other
        }

        return TransactionCategory.Other
    }

    /**
     * Learn user corrected category for future automated detections.
     */
    suspend fun learnCorrection(merchant: String, correctCategory: TransactionCategory, repository: TransactionRepository) {
        val cleanName = merchant.trim().lowercase()
        if (cleanName.isNotEmpty()) {
            repository.savePreference("learn_merchant_$cleanName", correctCategory.name)
        }
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = IntArray(s2.length + 1) { it }
        for (i in 1..s1.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..s2.length) {
                val temp = dp[j]
                if (s1[i - 1] == s2[j - 1]) {
                    dp[j] = prev
                } else {
                    dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + 1)
                }
                prev = temp
            }
        }
        return dp[s2.length]
    }
}
