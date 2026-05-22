package com.example.data.repository

import com.example.data.local.BudgetDao
import com.example.data.local.PreferenceDao
import com.example.data.local.TransactionDao
import com.example.data.model.BudgetEntity
import com.example.data.model.PreferenceEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val preferenceDao: PreferenceDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactionsFlow()
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgetsFlow()
    val allPreferences: Flow<List<PreferenceEntity>> = preferenceDao.getAllPreferencesFlow()

    suspend fun getAllTransactionsList(): List<TransactionEntity> = transactionDao.getAllTransactions()
    suspend fun getTransactionById(id: String): TransactionEntity? = transactionDao.getTransactionById(id)
    suspend fun insertTransaction(transaction: TransactionEntity) = transactionDao.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: TransactionEntity) = transactionDao.updateTransaction(transaction)
    suspend fun deleteTransaction(id: String) = transactionDao.deleteTransactionById(id)
    suspend fun clearTransactions() = transactionDao.clearAllTransactions()

    // Budgets
    suspend fun getAllBudgetsList(): List<BudgetEntity> = budgetDao.getAllBudgets()
    suspend fun insertBudget(budget: BudgetEntity) = budgetDao.insertBudget(budget)
    suspend fun insertBudgets(budgets: List<BudgetEntity>) = budgetDao.insertBudgets(budgets)
    suspend fun deleteBudgetByCategory(category: String) = budgetDao.deleteBudgetByCategory(category)
    suspend fun clearBudgets() = budgetDao.clearAllBudgets()

    // Preferences
    suspend fun getPreferenceValue(key: String): String? = preferenceDao.getPreferenceValue(key)?.value
    suspend fun savePreference(key: String, value: String) {
        preferenceDao.insertPreference(PreferenceEntity(key, value))
    }
    suspend fun deletePreference(key: String) = preferenceDao.deletePreferenceByKey(key)
}
