package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BudgetEntity
import com.example.data.model.TransactionEntity
import com.example.data.parser.CategorizationEngine
import com.example.data.parser.SmsParserService
import com.example.data.repository.TransactionRepository
import com.example.domain.insights.Insight
import com.example.domain.insights.InsightsEngine
import com.example.domain.model.TransactionCategory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class FintechViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository

    val transactions: StateFlow<List<TransactionEntity>>
    val budgets: StateFlow<List<BudgetEntity>>
    val preferences: StateFlow<Map<String, String>>
    val insights: StateFlow<List<Insight>>

    // High-fidelity Phase 4 Popup Transaction State
    private val _popupTransaction = MutableStateFlow<TransactionEntity?>(null)
    val popupTransaction: StateFlow<TransactionEntity?> = _popupTransaction.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TransactionRepository(
            transactionDao = db.transactionDao(),
            budgetDao = db.budgetDao(),
            preferenceDao = db.preferenceDao()
        )

        transactions = repository.allTransactions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        budgets = repository.allBudgets
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        preferences = repository.allPreferences
            .map { list -> list.associate { it.key to it.value } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        // Reactively compute rule-based insights when transactions or budgets update
        insights = combine(transactions, budgets) { txList, bList ->
            InsightsEngine.generateInsights(txList, bList)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Seed initial budgets if empty
        viewModelScope.launch {
            val currentBudgets = repository.getAllBudgetsList()
            if (currentBudgets.isEmpty()) {
                val initial = listOf(
                    BudgetEntity(TransactionCategory.Food.name, 12000.0),
                    BudgetEntity(TransactionCategory.Grocery.name, 8000.0),
                    BudgetEntity(TransactionCategory.Travel.name, 5000.0),
                    BudgetEntity(TransactionCategory.Subscription.name, 2500.0),
                    BudgetEntity(TransactionCategory.Shopping.name, 10000.0),
                    BudgetEntity(TransactionCategory.Bills.name, 6000.0)
                )
                repository.insertBudgets(initial)
            }
        }
    }

    /**
     * Parse simulated SMS text, save transaction, and immediately trigger the interactive smart verification popup.
     */
    fun simulateSmsParsing(smsBody: String) {
        viewModelScope.launch {
            val parsedTx = SmsParserService.parseMessage(smsBody)
            if (parsedTx != null) {
                // Ensure dynamic learning overrides the category if exists
                val finalCategoryName = CategorizationEngine.categorizeMerchant(parsedTx.merchant, repository).name
                val finalTx = parsedTx.copy(category = finalCategoryName)

                // Save to Database
                repository.insertTransaction(finalTx)

                // Triggers Phase 4 smart modal overlay
                _popupTransaction.value = finalTx
            }
        }
    }

    fun dismissPopup() {
        _popupTransaction.value = null
    }

    /**
     * User modifies category correction, triggering local AI feedback.
     */
    fun correctionLearned(txId: String, merchant: String, correctedCategory: TransactionCategory) {
        viewModelScope.launch {
            // Write correction override to Local AI Database key-value pair
            CategorizationEngine.learnCorrection(merchant, correctedCategory, repository)
            
            // Update historical transaction if matches
            val tx = repository.getTransactionById(txId)
            if (tx != null) {
                repository.updateTransaction(tx.copy(category = correctedCategory.name, userVerified = true))
            }
        }
    }

    /**
     * Set monthly cap for any given category.
     */
    fun setBudgetLimit(categoryName: String, limit: Double) {
        viewModelScope.launch {
            val existing = repository.getAllBudgetsList().find { it.category == categoryName }
            if (existing != null) {
                repository.insertBudget(existing.copy(limitAmount = limit))
            } else {
                repository.insertBudget(BudgetEntity(categoryName, limit))
            }
        }
    }

    /**
     * Add manual finance transaction.
     */
    fun addManualTransaction(
        amount: Double,
        merchant: String,
        category: TransactionCategory,
        type: String,
        note: String? = null
    ) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                amount = amount,
                merchant = merchant,
                category = category.name,
                subcategory = "Manual Override",
                transactionType = type,
                timestamp = System.currentTimeMillis(),
                upiRefId = "TXN" + (100000000000L..999999999999L).random().toString(),
                bankName = "Manual Entry",
                rawMessage = "Manual Entry - $merchant: Rs.$amount",
                confidenceScore = 1.0,
                isRecurring = false,
                isSubscription = category == TransactionCategory.Subscription,
                userVerified = true,
                note = note
            )
            repository.insertTransaction(tx)
        }
    }

    fun deleteTransaction(txId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(txId)
        }
    }

    fun toggleBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            repository.savePreference("biometric_lock", enabled.toString())
        }
    }

    fun changeLanguage(langCode: String) {
        viewModelScope.launch {
            repository.savePreference("language_code", langCode)
        }
    }

    fun clearAllUserData() {
        viewModelScope.launch {
            repository.clearTransactions()
            repository.clearBudgets()
            repository.deletePreference("biometric_lock")
            repository.deletePreference("language_code")
            
            // Reseed budgets
            val initial = listOf(
                BudgetEntity(TransactionCategory.Food.name, 12000.0),
                BudgetEntity(TransactionCategory.Grocery.name, 8000.0),
                BudgetEntity(TransactionCategory.Travel.name, 5000.0),
                BudgetEntity(TransactionCategory.Subscription.name, 2500.0),
                BudgetEntity(TransactionCategory.Shopping.name, 10000.0),
                BudgetEntity(TransactionCategory.Bills.name, 6000.0)
            )
            repository.insertBudgets(initial)
        }
    }
}
