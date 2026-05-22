package com.example.ui

import android.app.Application
import androidx.lifecycle.*
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    init {
        viewModelScope.launch {
            // Delete previously seeded dummy data gracefully block by block to wipe them completely
            repository.allExpenses.first().forEach { expense ->
                if (expense.title in listOf(
                        "Monthly Salary",
                        "Tokyo Ramen Bar",
                        "Aesthetic Keycaps",
                        "Netflix Premium",
                        "Cloud VM Server",
                        "Commute Travel Card",
                        "Aesthetic Client Bonus"
                    )) {
                    repository.deleteById(expense.id)
                }
            }
        }
    }

    // Current filter states
    private val _selectedTypeFilter = MutableStateFlow("ALL") // "ALL", "EXPENSE", "INCOME"
    val selectedTypeFilter = _selectedTypeFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("ALL")
    val selectedCategoryFilter = _selectedCategoryFilter.asStateFlow()

    // Base flows
    val allExpenses = repository.allExpenses
    val totalIncome = repository.totalIncome.map { it ?: 0.0 }
    val totalExpense = repository.totalExpense.map { it ?: 0.0 }

    // Filtered list flow
    val filteredExpenses = combine(
        allExpenses,
        _selectedTypeFilter,
        _selectedCategoryFilter,
        _searchQuery
    ) { expenses, type, category, query ->
        expenses.filter { expense ->
            val matchesType = type == "ALL" || expense.type == type
            val matchesCategory = category == "ALL" || expense.category == category
            val matchesQuery = query.isEmpty() || expense.title.contains(query, ignoreCase = true) || expense.note.contains(query, ignoreCase = true)
            matchesType && matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category distribution flow for expenses (percent spent)
    val categoryDistribution = allExpenses.map { expenses ->
        val expenseTotal = expenses.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        if (expenseTotal == 0.0) emptyMap()
        else {
            expenses.filter { it.type == "EXPENSE" }
                .groupBy { it.category }
                .mapValues { (_, list) ->
                    val sum = list.sumOf { it.amount }
                    sum / expenseTotal
                }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectTypeFilter(type: String) {
        _selectedTypeFilter.value = type
    }

    fun selectCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addExpense(
        title: String,
        amount: Double,
        category: String,
        type: String,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.insert(
                Expense(
                    title = title,
                    amount = amount,
                    category = category,
                    type = type,
                    note = note,
                    timestamp = timestamp
                )
            )
        }
    }

    fun deleteExpense(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = ExpenseDatabase.getDatabase(application)
                val repository = ExpenseRepository(db.expenseDao())
                return ExpenseViewModel(repository) as T
            }
        }
    }
}
