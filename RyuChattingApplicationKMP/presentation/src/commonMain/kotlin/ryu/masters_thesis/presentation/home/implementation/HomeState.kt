package ryu.masters_thesis.presentation.home.implementation

import ryu.masters_thesis.data.vault.domain.RoomSummary

// Immutable snapshot – zdroj infa pro HomeScreen
data class HomeState(
    val rooms:     List<RoomSummary> = emptyList(),
    val isLoading: Boolean           = false,
    val error:     String?           = null,
)