package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.*

data class TransactionCategory(
    val id: String,
    val displayName: String,
    val icon: ImageVector,
    val color: Color
)

object CategoryHelper {
    val categories = listOf(
        TransactionCategory("Food", "Food & Dining", Icons.Default.Fastfood, MintPrimary),
        TransactionCategory("Shopping", "Shopping", Icons.Default.ShoppingCart, CyanSecondary),
        TransactionCategory("Entertainment", "Entertainment", Icons.Default.Tv, NeonPurple),
        TransactionCategory("Bills", "Bills & Utilities", Icons.Default.Receipt, ExpenseRed),
        TransactionCategory("Travel", "Travel & Transport", Icons.Default.DirectionsCar, GoldAccent),
        TransactionCategory("Salary", "Salary Income", Icons.Default.AttachMoney, IncomeGreen),
        TransactionCategory("Investment", "Investments", Icons.Default.TrendingUp, CyanSecondary),
        TransactionCategory("Others", "Others", Icons.Default.Category, TextDarkSecondary)
    )

    fun getCategory(id: String): TransactionCategory {
        return categories.find { it.id.equals(id, ignoreCase = true) }
            ?: TransactionCategory(id, id, Icons.Default.Category, TextDarkMuted)
    }
}
