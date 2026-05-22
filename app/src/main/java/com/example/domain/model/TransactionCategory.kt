package com.example.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class TransactionCategory(
    val displayName: String,
    val icon: ImageVector,
    val color: Color
) {
    Food("Food & Dining", Icons.Default.Restaurant, Color(0xFFFF9800)),
    Grocery("Groceries", Icons.Default.ShoppingCart, Color(0xFF4CAF50)),
    Travel("Travel & Cab", Icons.Default.DirectionsCar, Color(0xFF00BCD4)),
    Shopping("Shopping", Icons.Default.LocalMall, Color(0xFFEC407A)),
    Entertainment("Entertainment", Icons.Default.LocalPlay, Color(0xFF9C27B0)),
    Bills("Utility Bills", Icons.Default.ReceiptLong, Color(0xFFE91E63)),
    Recharge("Phone Recharge", Icons.Default.PhonelinkRing, Color(0xFF2196F3)),
    Medical("Medical & Health", Icons.Default.MedicalServices, Color(0xFFF44336)),
    Laundry("Laundry Services", Icons.Default.DryCleaning, Color(0xFF03A9F4)),
    Education("Education & Fees", Icons.Default.School, Color(0xFF3F51B5)),
    Investment("Investments", Icons.Default.TrendingUp, Color(0xFF009688)),
    Subscription("Subscriptions", Icons.Default.LiveTv, Color(0xFF673AB7)),
    Transfer("Fund Transfers", Icons.Default.SwapHoriz, Color(0xFF607D8B)),
    Refund("Refunds", Icons.Default.SettingsBackupRestore, Color(0xFF8BC34A)),
    Other("Other Expenses", Icons.Default.Category, Color(0xFF9E9E9E));

    companion object {
        fun fromString(value: String): TransactionCategory {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: Other
        }
    }
}
