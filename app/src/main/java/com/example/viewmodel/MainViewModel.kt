package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.ActivityLog
import com.example.models.AppSettingsEntity
import com.example.models.Customer
import com.example.models.CustomerStatus
import com.example.models.DashboardSummary
import com.example.models.PaymentRecord
import com.example.network.AskAiAssistant
import com.example.network.WhatsAppService
import com.example.repository.CustomerRepository
import com.example.workers.FollowUpCheckWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class LoginUiState(
    val isAuthenticated: Boolean = false,
    val usernameInput: String = "waqar",
    val passwordInput: String = "waqar",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class FilterState(
    val searchQuery: String = "",
    val selectedStatus: CustomerStatus? = null,
    val onlyOverdue: Boolean = false,
    val onlyToday: Boolean = false,
    val onlyPendingPayment: Boolean = false,
    val onlyConfirmed: Boolean = false,
    val onlySuccess: Boolean = false
)

sealed interface Screen {
    data object Dashboard : Screen
    data object CustomerList : Screen
    data class CustomerDetail(val customerId: String) : Screen
    data class CustomerEdit(val customerId: String? = null) : Screen // null for new
    data object AskAi : Screen
    data object Settings : Screen
    data object ArchivedList : Screen
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CustomerRepository(application)

    // Navigation state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Navigation backstack
    private val backStack = mutableListOf<Screen>()

    // Login State
    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    // Filter State
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    // UI Feedback Banner / Snack
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // AI Chat History
    private val _aiMessages = MutableStateFlow(
        listOf(
            com.example.network.AiChatMessage(
                sender = "ai",
                text = "👋 Hello Waqar! I am your AI assistant for Web Record.\n\nAsk me anything about adding inquiries, recording payments, tracking overdue follow-ups, or sending automated WhatsApp reminders."
            )
        )
    )
    val aiMessages: StateFlow<List<com.example.network.AiChatMessage>> = _aiMessages.asStateFlow()
    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    // Live Data Streams
    val dashboardSummary: StateFlow<DashboardSummary> = repository.dashboardSummary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    val allActiveCustomers: StateFlow<List<Customer>> = repository.allActiveCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedCustomers: StateFlow<List<Customer>> = repository.archivedCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appSettings: StateFlow<AppSettingsEntity?> = repository.appSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Filtered customer list
    val filteredCustomers: StateFlow<List<Customer>> = combine(
        allActiveCustomers,
        _filterState
    ) { list, filter ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis
        val endOfToday = startOfToday + 86400000L

        list.filter { c ->
            // Search text
            val matchesSearch = filter.searchQuery.isBlank() ||
                c.customerName.contains(filter.searchQuery, ignoreCase = true) ||
                c.customerId.contains(filter.searchQuery, ignoreCase = true) ||
                c.mobileNumber.contains(filter.searchQuery, ignoreCase = true) ||
                c.whatsappNumber.contains(filter.searchQuery, ignoreCase = true) ||
                c.businessName.contains(filter.searchQuery, ignoreCase = true) ||
                c.city.contains(filter.searchQuery, ignoreCase = true)

            // Status filter
            val matchesStatus = filter.selectedStatus == null || c.currentStatus == filter.selectedStatus

            // Quick toggles
            val matchesOverdue = !filter.onlyOverdue || (
                c.followUpDate in 1 until startOfToday &&
                c.currentStatus != CustomerStatus.SUCCESS &&
                c.currentStatus != CustomerStatus.CANCELLED
            )

            val matchesToday = !filter.onlyToday || (
                c.followUpDate in startOfToday until endOfToday &&
                c.currentStatus != CustomerStatus.SUCCESS &&
                c.currentStatus != CustomerStatus.CANCELLED
            )

            val matchesPendingPayment = !filter.onlyPendingPayment || (c.pendingAmount > 0)
            val matchesConfirmed = !filter.onlyConfirmed || (c.currentStatus == CustomerStatus.CONFIRMED)
            val matchesSuccess = !filter.onlySuccess || (c.currentStatus == CustomerStatus.SUCCESS)

            matchesSearch && matchesStatus && matchesOverdue && matchesToday && matchesPendingPayment && matchesConfirmed && matchesSuccess
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
            FollowUpCheckWorker.schedule(application)
        }
    }

    // Navigation
    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            backStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        return if (backStack.isNotEmpty()) {
            _currentScreen.value = backStack.removeAt(backStack.size - 1)
            true
        } else {
            if (_currentScreen.value != Screen.Dashboard) {
                _currentScreen.value = Screen.Dashboard
                true
            } else false
        }
    }

    // Login Logic
    fun updateLoginUsername(v: String) {
        _loginState.value = _loginState.value.copy(usernameInput = v, errorMessage = null)
    }

    fun updateLoginPassword(v: String) {
        _loginState.value = _loginState.value.copy(passwordInput = v, errorMessage = null)
    }

    fun togglePasswordVisibility() {
        _loginState.value = _loginState.value.copy(isPasswordVisible = !_loginState.value.isPasswordVisible)
    }

    fun attemptLogin() {
        val current = _loginState.value
        _loginState.value = current.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val settings = repository.getAppSettings()
            val validUser = settings.adminUsername
            val validPass = settings.adminPasswordHash

            if (current.usernameInput.trim() == validUser && current.passwordInput == validPass) {
                _loginState.value = current.copy(isAuthenticated = true, isLoading = false, errorMessage = null)
            } else {
                _loginState.value = current.copy(
                    isLoading = false,
                    errorMessage = "Invalid credentials. Default username: '$validUser', password: '$validPass'"
                )
            }
        }
    }

    fun logout() {
        _loginState.value = LoginUiState(isAuthenticated = false)
        _currentScreen.value = Screen.Dashboard
        backStack.clear()
    }

    // Filter actions
    fun setSearchQuery(q: String) {
        _filterState.value = _filterState.value.copy(searchQuery = q)
    }

    fun setStatusFilter(status: CustomerStatus?) {
        _filterState.value = _filterState.value.copy(selectedStatus = status)
    }

    fun toggleOverdueFilter() {
        _filterState.value = _filterState.value.copy(
            onlyOverdue = !_filterState.value.onlyOverdue,
            onlyToday = false
        )
    }

    fun toggleTodayFilter() {
        _filterState.value = _filterState.value.copy(
            onlyToday = !_filterState.value.onlyToday,
            onlyOverdue = false
        )
    }

    fun togglePendingPaymentFilter() {
        _filterState.value = _filterState.value.copy(onlyPendingPayment = !_filterState.value.onlyPendingPayment)
    }

    fun clearAllFilters() {
        _filterState.value = FilterState()
    }

    // Customer CRUD & Actions
    fun saveCustomer(customer: Customer, isEdit: Boolean, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = if (isEdit) {
                repository.updateCustomer(customer, "Customer details edited and saved.")
            } else {
                repository.createCustomer(customer)
            }

            if (result.isSuccess) {
                _userMessage.value = if (isEdit) "Customer updated successfully!" else "New inquiry created successfully!"
                onDone(true)
            } else {
                _userMessage.value = result.exceptionOrNull()?.message ?: "Failed to save customer."
                onDone(false)
            }
        }
    }

    fun recordCustomerPayment(
        customer: Customer,
        amount: Double,
        paymentMethod: String,
        notes: String
    ) {
        viewModelScope.launch {
            val res = repository.recordPayment(customer, amount, paymentMethod, notes)
            if (res.isSuccess) {
                _userMessage.value = "Payment of ₹$amount recorded! Balance updated."
            } else {
                _userMessage.value = "Failed to record payment: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun markProjectSuccess(customer: Customer) {
        viewModelScope.launch {
            val res = repository.markProjectSuccess(customer)
            if (res.isSuccess) {
                _userMessage.value = "Project marked SUCCESS! Payment & project status completed."
            } else {
                _userMessage.value = "Failed to update project status."
            }
        }
    }

    fun logCustomerCall(customer: Customer) {
        viewModelScope.launch {
            repository.logCall(customer)
        }
    }

    fun logCustomerWhatsapp(customer: Customer, message: String) {
        viewModelScope.launch {
            repository.logWhatsappMessage(customer, message)
            _userMessage.value = "WhatsApp communication logged in customer timeline."
        }
    }

    fun sendWhatsappApi(customer: Customer, message: String) {
        viewModelScope.launch {
            val settings = repository.getAppSettings()
            val apiRes = WhatsAppService.sendWhatsAppViaApi(
                endpointUrl = settings.whatsappApiEndpoint,
                apiKey = settings.whatsappApiKey,
                customer = customer,
                messageText = message
            )
            if (apiRes.success) {
                repository.logWhatsappMessage(customer, message)
                _userMessage.value = apiRes.message
            } else {
                _userMessage.value = apiRes.message
            }
        }
    }

    fun archiveCustomer(id: String) {
        viewModelScope.launch {
            repository.archiveCustomer(id)
            _userMessage.value = "Customer moved to archive."
            navigateBack()
        }
    }

    fun restoreCustomer(id: String) {
        viewModelScope.launch {
            repository.restoreCustomer(id)
            _userMessage.value = "Customer restored to active records."
        }
    }

    fun deleteCustomerPermanently(id: String) {
        viewModelScope.launch {
            repository.deletePermanently(id)
            _userMessage.value = "Customer permanently removed."
            navigateBack()
        }
    }

    // Detail streams
    fun getCustomerStream(id: String) = repository.getCustomerByIdFlow(id)
    fun getLogsStream(customerId: String) = repository.getLogsForCustomer(customerId)
    fun getPaymentsStream(customerId: String) = repository.getPaymentsForCustomer(customerId)

    // AI Chat
    fun sendAiQuestion(question: String) {
        if (question.isBlank()) return
        val userMsg = com.example.network.AiChatMessage(sender = "user", text = question.trim())
        _aiMessages.value = _aiMessages.value + userMsg
        _aiLoading.value = true

        viewModelScope.launch {
            val answer = AskAiAssistant.getAiAnswer(question)
            val aiMsg = com.example.network.AiChatMessage(sender = "ai", text = answer)
            _aiMessages.value = _aiMessages.value + aiMsg
            _aiLoading.value = false
        }
    }

    fun clearAiChat() {
        _aiMessages.value = listOf(
            com.example.network.AiChatMessage(
                sender = "ai",
                text = "Conversation cleared. How can I assist you with Web Record by Waqar?"
            )
        )
    }

    // Settings & Backup
    fun saveSettings(newSettings: AppSettingsEntity) {
        viewModelScope.launch {
            repository.saveAppSettings(newSettings)
            _userMessage.value = "Settings saved successfully."
        }
    }

    fun exportBackup(onExported: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportDatabaseJson()
            repository.saveAppSettings(
                repository.getAppSettings().copy(lastBackupTimestamp = System.currentTimeMillis())
            )
            onExported(json)
            _userMessage.value = "Full database backup generated successfully!"
        }
    }

    fun restoreBackup(jsonString: String) {
        viewModelScope.launch {
            val res = repository.restoreDatabaseFromJson(jsonString)
            if (res.isSuccess) {
                _userMessage.value = "Restored ${res.getOrNull()} customer records successfully!"
            } else {
                _userMessage.value = "Restore failed: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    suspend fun getNextSuggestedId(): String = repository.generateNextCustomerId()
}
