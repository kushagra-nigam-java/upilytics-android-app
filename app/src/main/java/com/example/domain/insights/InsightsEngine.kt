package com.example.domain.insights

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.BudgetEntity
import com.example.data.model.TransactionEntity
import com.example.domain.model.TransactionCategory
import java.util.Calendar

data class Insight(
    val title: String,
    val description: String,
    val iconName: String, // mapping to dynamic icons
    val severity: String, // "info", "warning", "positive"
    val actionable: Boolean,
    val generatedAt: Long = System.currentTimeMillis()
) {
    fun getIcon(): ImageVector {
        return when (iconName) {
            "trending_up" -> Icons.Default.TrendingUp
            "trending_down" -> Icons.Default.TrendingDown
            "restaurant" -> Icons.Default.Restaurant
            "warning" -> Icons.Default.Warning
            "auto_awesome" -> Icons.Default.AutoAwesome
            "credit_card" -> Icons.Default.CreditCard
            "nights_stay" -> Icons.Default.NightsStay
            "savings" -> Icons.Default.Savings
            else -> Icons.Default.Analytics
        }
    }
}

object InsightsEngine {

    fun generateInsights(transactions: List<TransactionEntity>, budgets: List<BudgetEntity>): List<Insight> {
        val insights = mutableListOf<Insight>()
        if (transactions.isEmpty()) {
            insights.add(
                Insight(
                    title = "Awaiting Transactions",
                    description = "We will generate personalized fintech insights as soon as your first UPI messages are detected.",
                    iconName = "auto_awesome",
                    severity = "info",
                    actionable = false
                )
            )
            return insights
        }

        // 1. Top Merchant Insights
        val merchantSpent = transactions.filter { it.transactionType == "debit" }
            .groupBy { it.merchant }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        
        if (merchantSpent.isNotEmpty()) {
            val topMerchant = merchantSpent.maxByOrNull { it.value }
            if (topMerchant != null && topMerchant.value > 100) {
                insights.add(
                    Insight(
                        title = "${topMerchant.key} is your top merchant",
                        description = "You spent Rs. ${String.format("%.2f", topMerchant.value)} at ${topMerchant.key} this month.",
                        iconName = "restaurant",
                        severity = "info",
                        actionable = true
                    )
                )
            }
        }

        // 2. Budget Breach / Warning Insights
        val categorySpends = transactions.filter { it.transactionType == "debit" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        for (budget in budgets) {
            val actualSpent = categorySpends[budget.category] ?: 0.0
            if (budget.limitAmount > 0) {
                val ratio = actualSpent / budget.limitAmount
                if (ratio >= 1.0) {
                    insights.add(
                        Insight(
                            title = "Budget Breached: ${budget.category}",
                            description = "You've exceeded your Rs. ${budget.limitAmount} budget limit for ${budget.category} (Spent: Rs. ${String.format("%.2f", actualSpent)}).",
                            iconName = "warning",
                            severity = "warning",
                            actionable = true
                        )
                    )
                } else if (ratio >= 0.8) {
                    insights.add(
                        Insight(
                            title = "Approaching Budget Cap",
                            description = "You have used ${String.format("%.0f", ratio * 100)}% of your ${budget.category} budget.",
                            iconName = "trending_up",
                            severity = "warning",
                            actionable = false
                        )
                    )
                }
            }
        }

        // 3. Nocturnal Spend habit Rule (Between 9PM and 4AM)
        val nocturnalTxns = transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hour >= 21 || hour < 4
        }
        val nocturnalSpent = nocturnalTxns.sumOf { it.amount }
        val totalSpent = transactions.filter { it.transactionType == "debit" }.sumOf { it.amount }
        if (totalSpent > 0 && (nocturnalSpent / totalSpent) >= 0.3) {
            insights.add(
                Insight(
                    title = "High Nocturnal Spending",
                    description = "You spend ${String.format("%.0f", (nocturnalSpent / totalSpent) * 100)}% of your money between 9 PM and 4 AM. Consider setting budget alarms for late-night food or shopping.",
                    iconName = "nights_stay",
                    severity = "warning",
                    actionable = true
                )
            )
        }

        // 4. Savings Insight (if credits > debits)
        val credits = transactions.filter { it.transactionType == "credit" }.sumOf { it.amount }
        val debits = transactions.filter { it.transactionType == "debit" }.sumOf { it.amount }
        if (credits > debits && debits > 0) {
            val savingsRate = ((credits - debits) / credits) * 100
            insights.add(
                Insight(
                    title = "Excellent Savings Trend!",
                    description = "Your income exceeds your expenses, representing a savings rate of ${String.format("%.1f", savingsRate)}% this month. Great job budgeting!",
                    iconName = "savings",
                    severity = "positive",
                    actionable = false
                )
            )
        }

        // 5. Subscription Overlook Insight
        val subscriptions = transactions.filter { it.isSubscription }
        if (subscriptions.isNotEmpty()) {
            val subTotal = subscriptions.sumOf { it.amount }
            insights.add(
                Insight(
                    title = "Active Subscriptions Monitor",
                    description = "You have ${subscriptions.size} active subscriptions totaling Rs. ${String.format("%.2f", subTotal)} per month.",
                    iconName = "credit_card",
                    severity = "info",
                    actionable = true
                )
            )
        }

        // Ensure there is always a positive backup insight
        if (insights.none { it.severity == "positive" }) {
            insights.add(
                Insight(
                    title = "UPIlytics Auto-categorizing",
                    description = "Our built-in AI parser classifies 98% of common Indian banking notifications instantly without cloud uploads, securing your financial identity.",
                    iconName = "auto_awesome",
                    severity = "positive",
                    actionable = false
                )
            )
        }

        return insights
    }
}
