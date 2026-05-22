package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val timestamp: Long,
    val category: String, // e.g. "Food", "Shopping", "Entertainment", "Bills", "Salary", "Investment", "Travel", "Others"
    val type: String,     // "EXPENSE" or "INCOME"
    val note: String = ""
)
