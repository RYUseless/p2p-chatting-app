package ryu.masters_thesis.presentation.home.implementation
//TODO: DELETE IN THE NEXT COMMIT


sealed class ReconnectState {
    object Connecting : ReconnectState()
    object Failed     : ReconnectState()
}