package com.example.ui.contacts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Contact
import com.example.data.repository.ContactsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val isLoading: Boolean = false,
    val permissionGranted: Boolean = false,
    val query: String = ""
)

/**
 * Shared address book state for both the Contacts screen and the dialer.
 *
 * All three streams re-run whenever the permission flips or the platform
 * reports a change to the address book, so the UI never shows stale rows.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContactsRepository.getInstance(application)

    private val _permissionGranted = MutableStateFlow(repository.hasPermission())
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val dialerInput = MutableStateFlow("")

    /** Bumped by the content observer to force a re-query. */
    private val refreshSignal = MutableStateFlow(0L)

    val uiState: StateFlow<ContactsUiState> = combine(
        _permissionGranted,
        _searchQuery.debounce { if (it.isEmpty()) 0L else SEARCH_DEBOUNCE_MS },
        refreshSignal
    ) { granted, query, tick -> LookupRequest(granted, query, tick) }
        .flatMapLatest { request ->
            if (!request.granted) {
                flowOf(ContactsUiState(permissionGranted = false, query = request.query))
            } else {
                flow {
                    emit(
                        ContactsUiState(
                            permissionGranted = true,
                            isLoading = true,
                            query = request.query
                        )
                    )
                    emit(
                        ContactsUiState(
                            contacts = repository.queryContacts(request.query),
                            permissionGranted = true,
                            query = request.query
                        )
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContactsUiState())

    /** Live smart-dial matches for the digits currently typed on the keypad. */
    val dialerSuggestions: StateFlow<List<Contact>> = combine(
        _permissionGranted,
        dialerInput.debounce { if (it.isEmpty()) 0L else DIAL_DEBOUNCE_MS },
        refreshSignal
    ) { granted, input, tick -> LookupRequest(granted, input, tick) }
        .flatMapLatest { request ->
            val digits = request.query.trimStart('+')
            if (!request.granted || digits.length < MIN_SMART_DIAL_DIGITS) {
                flowOf(emptyList())
            } else {
                flow { emit(repository.queryContacts(digits, limit = MAX_SUGGESTIONS)) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Contacts the user starred in the system address book. */
    val favorites: StateFlow<List<Contact>> = combine(
        _permissionGranted,
        refreshSignal
    ) { granted, tick -> LookupRequest(granted, "", tick) }
        .flatMapLatest { request ->
            if (!request.granted) {
                flowOf(emptyList())
            } else {
                flow { emit(repository.queryContacts(starredOnly = true, limit = MAX_FAVORITES)) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repository.contactChanges().collect {
                refreshSignal.value = System.currentTimeMillis()
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /** Feeds the keypad input into the smart-dial lookup. */
    fun onDialerInputChanged(input: String) {
        dialerInput.value = input
    }

    fun onPermissionResult(granted: Boolean) {
        if (_permissionGranted.value != granted) {
            _permissionGranted.value = granted
        }
        if (granted) {
            refreshSignal.value = System.currentTimeMillis()
        }
    }

    /** Re-checks the permission and reloads, e.g. when a screen is resumed. */
    fun refresh() {
        _permissionGranted.value = repository.hasPermission()
        refreshSignal.value = System.currentTimeMillis()
    }

    private data class LookupRequest(
        val granted: Boolean,
        val query: String,
        val tick: Long
    )

    companion object {
        /** Digits required before the keypad starts matching the address book. */
        const val MIN_SMART_DIAL_DIGITS = 2

        private const val SEARCH_DEBOUNCE_MS = 220L
        private const val DIAL_DEBOUNCE_MS = 160L
        private const val MAX_SUGGESTIONS = 4
        private const val MAX_FAVORITES = 12
    }
}
