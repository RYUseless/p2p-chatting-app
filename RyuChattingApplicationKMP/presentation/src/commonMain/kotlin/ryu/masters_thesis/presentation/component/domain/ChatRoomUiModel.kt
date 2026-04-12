package ryu.masters_thesis.presentation.component.domain

// UI model – oddělený od doménového modelu z :core
data class ChatRoomUiModel(
    val id: String,
    val name: String,
    val isActive: Boolean,
)