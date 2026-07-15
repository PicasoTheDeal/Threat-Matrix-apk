package com.picasothedeal.threatmatrix.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.picasothedeal.threatmatrix.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ThreatUiState {
    object Loading : ThreatUiState
    data class Success(val logs: List<ThreatLog>) : ThreatUiState
    data class Error(val message: String) : ThreatUiState
}

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Authenticated(val token: String, val username: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class ThreatViewModel(application: Application, private val repository: ThreatRepository) : AndroidViewModel(application) {

    private val _rawLogs = MutableStateFlow<List<ThreatLog>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _feedState = MutableStateFlow<ThreatUiState>(ThreatUiState.Loading)

    val uiState: StateFlow<ThreatUiState> = combine(
        _rawLogs, _searchQuery, _selectedCategory, _feedState
    ) { rawList, query, category, state ->
        if (state is ThreatUiState.Error || (state is ThreatUiState.Loading && rawList.isEmpty())) {
            state
        } else {
            val filtered = rawList.filter { log ->
                val matchesSearch = log.title.contains(query, ignoreCase = true) ||
                        log.excerpt.contains(query, ignoreCase = true) ||
                        log.id.contains(query, ignoreCase = true)
                val matchesCategory = category == null || log.category.equals(category, ignoreCase = true)
                matchesSearch && matchesCategory
            }
            ThreatUiState.Success(filtered)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThreatUiState.Loading)

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _interactions = MutableStateFlow<Map<String, InteractionData>>(emptyMap())
    val interactions: StateFlow<Map<String, InteractionData>> = _interactions.asStateFlow()

    private val _userParameters = MutableStateFlow<List<String>>(emptyList())
    val userParameters: StateFlow<List<String>> = _userParameters.asStateFlow()

    private val _parametersLoaded = MutableStateFlow(false)
    val parametersLoaded: StateFlow<Boolean> = _parametersLoaded.asStateFlow()

    val currentAuthToken: String?
        get() = (_authState.value as? AuthUiState.Authenticated)?.token

    init {
        val savedToken = TokenManager.getToken(getApplication())
        val savedUsername = TokenManager.getUsername(getApplication())
        if (savedToken != null && savedUsername != null) {
            _authState.value = AuthUiState.Authenticated(savedToken, savedUsername)
            fetchParameters()
        }
        fetchLogs()
    }

    fun fetchLogs() {
        viewModelScope.launch {
            _feedState.value = ThreatUiState.Loading
            repository.fetchLogs(currentAuthToken)
                .onSuccess { logList ->
                    _rawLogs.value = logList
                    _feedState.value = ThreatUiState.Success(logList)
                    if (logList.isNotEmpty()) {
                        fetchInteractionsForLogs(logList.map { it.id })
                    }
                }
                .onFailure { exception ->
                    _feedState.value = ThreatUiState.Error(exception.message ?: "Unknown error occurred")
                }
        }
    }

    fun fetchParameters() {
        val token = currentAuthToken ?: return
        viewModelScope.launch {
            repository.fetchParameters(token)
                .onSuccess { response ->
                    _userParameters.value = response.tags
                    _parametersLoaded.value = true
                }
                .onFailure {
                    _parametersLoaded.value = true
                }
        }
    }

    private fun fetchInteractionsForLogs(logIds: List<String>) {
        viewModelScope.launch {
            repository.fetchInteractions(logIds, currentAuthToken)
                .onSuccess { dataMap ->
                    _interactions.value = dataMap
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun login(username: String, password: String, turnstileToken: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            _parametersLoaded.value = false
            repository.login(username, password, turnstileToken)
                .onSuccess { response ->
                    TokenManager.saveToken(getApplication(), response.token, username)
                    _authState.value = AuthUiState.Authenticated(response.token, username)
                    fetchParameters()
                    fetchLogs()
                }
                .onFailure { exception ->
                    _authState.value = AuthUiState.Error(exception.message ?: "Authentication failed")
                }
        }
    }

    fun signup(username: String, password: String, turnstileToken: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            _parametersLoaded.value = false
            repository.signup(username, password, turnstileToken)
                .onSuccess { response ->
                    TokenManager.saveToken(getApplication(), response.token, username)
                    _authState.value = AuthUiState.Authenticated(response.token, username)
                    fetchParameters()
                    fetchLogs()
                }
                .onFailure { exception ->
                    _authState.value = AuthUiState.Error(exception.message ?: "Registration failed")
                }
        }
    }

    fun logout() {
        TokenManager.clear(getApplication())
        _authState.value = AuthUiState.Idle
        _userParameters.value = emptyList()
        _parametersLoaded.value = false
        fetchLogs()
    }

    fun updateProfileTags(tags: List<String>) {
        val token = currentAuthToken ?: return
        viewModelScope.launch {
            repository.updateParameters(token, tags)
                .onSuccess {
                    _userParameters.value = tags
                }
        }
    }

    fun toggleLike(logId: String) {
        val token = currentAuthToken ?: return
        val currentInteraction = _interactions.value[logId] ?: return
        viewModelScope.launch {
            val call = if (currentInteraction.liked) repository.unlike(logId, token)
            else repository.like(logId, token)
            call.onSuccess { response ->
                response.interaction?.let { updatedInteraction ->
                    _interactions.update { currentMap ->
                        currentMap + (logId to updatedInteraction)
                    }
                }
            }
        }
    }

    fun addComment(logId: String, content: String, turnstileToken: String) {
        val token = currentAuthToken ?: return
        viewModelScope.launch {
            repository.comment(logId, content, turnstileToken, token)
                .onSuccess { response ->
                    response.interaction?.let { updatedInteraction ->
                        _interactions.update { currentMap ->
                            currentMap + (logId to updatedInteraction)
                        }
                    }
                }
        }
    }

    fun deleteComment(logId: String, commentId: Long) {
        val token = currentAuthToken ?: return
        viewModelScope.launch {
            repository.deleteComment(logId, commentId, token)
                .onSuccess { response ->
                    response.interaction?.let { updatedInteraction ->
                        _interactions.update { currentMap ->
                            currentMap + (logId to updatedInteraction)
                        }
                    }
                }
        }
    }
}