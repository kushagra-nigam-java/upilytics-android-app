package com.example.data.parser

import android.util.Log
import com.example.data.model.TransactionEntity
import com.example.domain.model.TransactionCategory
import java.util.UUID
import java.util.regex.Pattern

object SmsParserService {
    private const val TAG = "SmsParserService"

    // Amount patterns matching formats: Rs 500, Rs.500.00, INR 500, ₹450.50
    private val AMOUNT_PATTERN = Pattern.compile(
        """(?i)(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{2})?)"""
    )

    // UPI Ref ID patterns (commonly 12-digit numeric)
    private val UPI_REF_PATTERN = Pattern.compile(
        """(?i)(?:UPI\s*(?:Ref|Txn)?\s*(?:No|ID)?|Ref(?:\s*No)?|Txn\s*ID)[:\s\-]*(\d{12})"""
    )

    // Bank identifiers
    private val BANKS = listOf(
        "SBI", "YONO", "HDFC", "ICICI", "AXIS", "KOTAK", "PNB", "BOB", "CANARA", "PAYTM", "PHONEPE", "GPAY"
    )

    // Common Indian Merchants
    private val MERCHANTS = listOf(
        "SWIGGY", "ZOMATO", "UBER", "OLA", "FLIPKART", "AMAZON", "BIGBASKET", "DMART", "NETFLIX", "HOTSTAR",
        "JIO", "AIRTEL", "VI", "APOLLO", "PHARMEASY", "ZEPTO", "BLINKIT", "CRED", "BOOKMYSHOW", "STARBUCKS"
    )

    /**
     * Parse raw transaction message back into a TransactionEntity with category and metadata.
     */
    fun parseMessage(sms: String, timestamp: Long = System.currentTimeMillis()): TransactionEntity? {
        try {
            val cleanSms = sms.trim()
            val amount = extractAmount(cleanSms) ?: return null
            if (amount <= 0.0) return null

            val type = extractTransactionType(cleanSms)
            val bankName = extractBankName(cleanSms)
            val upiRefId = extractUpiRef(cleanSms)
            val merchant = extractMerchant(cleanSms, type)
            val categoryResult = matchCategoryAndSub(merchant, cleanSms)

            // Confidence score calculation: basic heuristic
            var confidence = 0.5
            if (upiRefId != null) confidence += 0.2
            if (bankName != "UPI System") confidence += 0.15
            if (merchant.isNotEmpty() && merchant != "Unknown") confidence += 0.15
            confidence = confidence.coerceIn(0.1, 1.0)

            return TransactionEntity(
                id = UUID.randomUUID().toString(),
                amount = amount,
                merchant = merchant,
                category = categoryResult.first,
                subcategory = categoryResult.second,
                transactionType = type,
                timestamp = timestamp,
                upiRefId = upiRefId,
                bankName = bankName,
                rawMessage = cleanSms,
                confidenceScore = confidence,
                isRecurring = categoryResult.third,
                isSubscription = categoryResult.fourth,
                userVerified = false,
                note = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SMS: $sms", e)
            return null
        }
    }

    private fun extractAmount(sms: String): Double? {
        val matcher = AMOUNT_PATTERN.matcher(sms)
        if (matcher.find()) {
            val amountStr = matcher.group(1)?.replace(",", "")
            return amountStr?.toDoubleOrNull()
        }
        return null
    }

    private fun extractTransactionType(sms: String): String {
        val smsLower = sms.lowercase()
        return when {
            smsLower.contains("refunded") || smsLower.contains("refund") -> "refund"
            smsLower.contains("credited") || smsLower.contains("received") || smsLower.contains("added") -> "credit"
            smsLower.contains("debited") || smsLower.contains("paid") || smsLower.contains("spent") || smsLower.contains("sent") -> "debit"
            else -> "debit" // Default to debit for transaction monitoring
        }
    }

    private fun extractBankName(sms: String): String {
        val smsUpper = sms.uppercase()
        for (bank in BANKS) {
            if (smsUpper.contains(bank)) {
                return if (bank == "YONO") "SBI" else bank
            }
        }
        return "UPI System"
    }

    private fun extractUpiRef(sms: String): String? {
        val matcher = UPI_REF_PATTERN.matcher(sms)
        if (matcher.find()) {
            return matcher.group(1)
        }
        // Fallback: look for any 12 digit number
        val generalMatcher = Pattern.compile("""\b\d{12}\b""").matcher(sms)
        if (generalMatcher.find()) {
            return generalMatcher.group()
        }
        return null
    }

    /**
     * Smart heuristic helper to find the receiving merchant. Look around "to", "at", "paid".
     */
    private fun extractMerchant(sms: String, type: String): String {
        val smsUpper = sms.uppercase()

        // 1. Direct check for known merchants in the string
        for (m in MERCHANTS) {
            if (smsUpper.contains(m)) {
                return m.lowercase().replaceFirstChar { it.uppercase() }
            }
        }

        // 2. Syntactic search using phrases
        val patterns = listOf(
            Pattern.compile("""(?i)(?:debited\s+to|paid\s+to|spent\s+at|sent\s+to|transfer\s+to)\s+([A-Za-z0-9\s‌]{2,20})"""),
            Pattern.compile("""(?i)(?:credited\s+from|received\s+from)\s+([A-Za-z0-9\s]{2,20})"""),
            Pattern.compile("""(?i)at\s+([A-Za-z0-9\s]{2,15})"""),
            Pattern.compile("""(?i)\bto\s+([A-Za-z0-9\s]{2,15})""")
        )

        for (p in patterns) {
            val m = p.matcher(sms)
            if (m.find()) {
                val candidate = m.group(1)?.trim() ?: ""
                val cleanCandidate = cleanMerchantName(candidate)
                if (cleanCandidate.isNotEmpty() && cleanCandidate.lowercase() != "user") {
                    return cleanCandidate
                }
            }
        }

        return if (type == "credit") "Receiving Transfer" else "UPI Merchant"
    }

    private fun cleanMerchantName(name: String): String {
        // Strip out common noise like "A/CXX34", "on 23-11", "Ref...", etc.
        var out = name.split(" ON ", " USING ", " FOR ", " A/C ", " REF ", " VIA ", " ON ", "\n", "\r")[0].trim()
        out = out.replace(Regex("""[^A-Za-z0-9\s]"""), "")
        // Trim more details
        out = out.split(Regex("""\s+""")).take(3).joinToString(" ")
        return out.trim()
    }

    /**
     * Local classification rules for ~200 Indian merchants & categories.
     * Returns Triple(Category, Subcategory, isRecurring, isSubscription)
     */
    private fun matchCategoryAndSub(merchant: String, rawSms: String): Quadruple<String, String?, Boolean, Boolean> {
        val name = merchant.lowercase()
        val sms = rawSms.lowercase()

        return when {
            name.contains("swiggy") || name.contains("zomato") || name.contains("starbucks") || name.contains("food") || name.contains("dining") || name.contains("restaurant") -> {
                Quadruple(TransactionCategory.Food.name, "Restaurant", false, false)
            }
            name.contains("bigbasket") || name.contains("dmart") || name.contains("blinkit") || name.contains("zepto") || name.contains("grocery") || name.contains("groceries") || sms.contains("supermarket") -> {
                Quadruple(TransactionCategory.Grocery.name, "Groceries", name.contains("bigbasket") || name.contains("dmart"), false)
            }
            name.contains("uber") || name.contains("ola") || name.contains("rapido") || name.contains("metro") || name.contains("irctc") || name.contains("railway") || name.contains("cab") || name.contains("travel") -> {
                Quadruple(TransactionCategory.Travel.name, "Transport", false, false)
            }
            name.contains("flipkart") || name.contains("amazon") || name.contains("amzn") || name.contains("myntra") || name.contains("ajio") || name.contains("shoppe") || name.contains("mall") || sms.contains("shop") -> {
                Quadruple(TransactionCategory.Shopping.name, "Shopping", false, false)
            }
            name.contains("netflix") || name.contains("hotstar") || name.contains("spotify") || name.contains("prime") || name.contains("youtube") || name.contains("bookmyshow") -> {
                val isSub = name.contains("netflix") || name.contains("spotify") || name.contains("prime")
                Quadruple(TransactionCategory.Subscription.name, if (isSub) "Digital Subscription" else "Entertainment", isSub, isSub)
            }
            name.contains("jio") || name.contains("airtel") || name.contains("vi") || sms.contains("recharge") -> {
                Quadruple(TransactionCategory.Recharge.name, "Prepaid", true, false)
            }
            name.contains("bescom") || name.contains("mseb") || name.contains("electricity") || name.contains("power") || name.contains("gas") || name.contains("water") || sms.contains("bill") -> {
                Quadruple(TransactionCategory.Bills.name, "Utilities", true, false)
            }
            name.contains("apollo") || name.contains("pharmeasy") || name.contains("hospital") || name.contains("clinic") || name.contains("medical") || name.contains("pharmacy") || sms.contains("med") -> {
                Quadruple(TransactionCategory.Medical.name, "Healthcare", false, false)
            }
            name.contains("laundry") || name.contains("dry cleaning") -> {
                Quadruple(TransactionCategory.Laundry.name, "Hygiene", false, false)
            }
            name.contains("school") || name.contains("college") || name.contains("udemy") || name.contains("coursera") || sms.contains("tuition") -> {
                Quadruple(TransactionCategory.Education.name, "Tuition", false, false)
            }
            name.contains("zerodha") || name.contains("groww") || name.contains("mutual fund") || name.contains("invest") || sms.contains("shares") -> {
                Quadruple(TransactionCategory.Investment.name, "Mutual Funds", true, false)
            }
            // Check for direct keywords in SMS
            sms.contains("movie") || sms.contains("theatre") || sms.contains("pvr") || sms.contains("cinema") -> {
                Quadruple(TransactionCategory.Entertainment.name, "Movies", false, false)
            }
            sms.contains("medicine") || sms.contains("dr.") || sms.contains("hospital") || sms.contains("medical") -> {
                Quadruple(TransactionCategory.Medical.name, "Pharmacy", false, false)
            }
            else -> {
                Quadruple(TransactionCategory.Other.name, null, false, false)
            }
        }
    }
}

data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
