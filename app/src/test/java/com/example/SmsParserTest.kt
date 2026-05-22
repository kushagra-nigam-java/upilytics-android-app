package com.example

import com.example.data.parser.SmsParserService
import com.example.domain.model.TransactionCategory
import org.junit.Assert.*
import org.junit.Test

class SmsParserTest {

    @Test
    fun testParseHdfcDebit() {
        val sms = "HDFC: Debited Rs 1250.00 to SWIGGY. Ref UPI 104928372832"
        val tx = SmsParserService.parseMessage(sms)
        assertNotNull(tx)
        assertEquals(1250.0, tx!!.amount, 0.01)
        assertEquals("Swiggy", tx.merchant)
        assertEquals(TransactionCategory.Food.name, tx.category)
        assertEquals("debit", tx.transactionType)
        assertEquals("104928372832", tx.upiRefId)
    }

    @Test
    fun testParseSbiDebit() {
        val sms = "SBI A/c ...3452 debited for Rs 350.50 to AMZN. Ref 491038472918"
        val tx = SmsParserService.parseMessage(sms)
        assertNotNull(tx)
        assertEquals(350.50, tx!!.amount, 0.01)
        assertEquals("AMZN", tx.merchant)
        assertEquals(TransactionCategory.Shopping.name, tx.category)
        assertEquals("debit", tx.transactionType)
    }

    @Test
    fun testParseIncomingCredit() {
        val sms = "Credited Rs 15000.00 to SBI A/c from HDFC. Ref 402918239018"
        val tx = SmsParserService.parseMessage(sms)
        assertNotNull(tx)
        assertEquals(15000.0, tx!!.amount, 0.01)
        assertEquals("credit", tx.transactionType)
        assertEquals("SBI", tx.bankName)
    }

    @Test
    fun testParseNetflixSubscription() {
        val sms = "Paid Rs.199.00 to Netflix. UPI Ref 592810482910"
        val tx = SmsParserService.parseMessage(sms)
        assertNotNull(tx)
        assertEquals(199.0, tx!!.amount, 0.01)
        assertEquals("Netflix", tx.merchant)
        assertEquals(TransactionCategory.Subscription.name, tx.category)
        assertTrue(tx.isSubscription)
    }
}
