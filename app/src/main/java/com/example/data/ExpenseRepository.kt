package com.example.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val totalIncome: Flow<Double?> = expenseDao.getTotalIncomeFlow()
    val totalExpense: Flow<Double?> = expenseDao.getTotalExpenseFlow()
    val allPlans: Flow<List<Plan>> = expenseDao.getAllPlans()

    suspend fun insert(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    suspend fun deleteById(id: Int) {
        expenseDao.deleteExpenseById(id)
    }

    suspend fun insertPlan(plan: Plan) {
        expenseDao.insertPlan(plan)
    }

    suspend fun deletePlanById(id: Int) {
        expenseDao.deletePlanById(id)
    }
}
