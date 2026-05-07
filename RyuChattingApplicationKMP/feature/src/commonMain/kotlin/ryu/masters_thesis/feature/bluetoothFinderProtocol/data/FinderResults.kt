package ryu.masters_thesis.feature.bluetoothFinderProtocol.data

sealed class FinderResult {
    data class Found(val hostAddress: String) : FinderResult()
    object NotFound                           : FinderResult()
}