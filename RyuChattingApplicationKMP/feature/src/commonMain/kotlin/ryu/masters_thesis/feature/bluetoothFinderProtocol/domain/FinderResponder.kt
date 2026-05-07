package ryu.masters_thesis.feature.bluetoothFinderProtocol.domain

// Server strana BFP — odpovídá na dotazy
interface FinderResponder {
    fun start()
    fun stop()
}