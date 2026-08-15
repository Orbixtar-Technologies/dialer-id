package com.example.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CallLogItem
import com.example.data.model.CallStatus
import com.example.data.repository.DialerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryFilter {
    ALL,
    COMPLETED,
    CANCELLED
}

class CallHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DialerRepository.getInstance(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(HistoryFilter.ALL)
    val selectedFilter: StateFlow<HistoryFilter> = _selectedFilter.asStateFlow()

    val filteredCallLogs: StateFlow<List<CallLogItem>> = combine(
        repository.allCallLogs,
        _searchQuery,
        _selectedFilter
    ) { logs, query, filter ->
        logs.filter { item ->
            val matchesQuery = query.isEmpty() ||
                item.destinationNumber.contains(query, ignoreCase = true) ||
                item.callerIdUsed.contains(query, ignoreCase = true) ||
                item.countryName.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                HistoryFilter.ALL -> true
                HistoryFilter.COMPLETED -> item.status == CallStatus.COMPLETED
                HistoryFilter.CANCELLED -> item.status == CallStatus.CANCELLED || item.status == CallStatus.FAILED
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: HistoryFilter) {
        _selectedFilter.value = filter
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearCallHistory()
        }
    }
}
