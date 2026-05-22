package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Int)

    @Query("SELECT SUM(amount) FROM expenses WHERE type = 'INCOME'")
    fun getTotalIncomeFlow(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE type = 'EXPENSE'")
    fun getTotalExpenseFlow(): Flow<Double?>
}
