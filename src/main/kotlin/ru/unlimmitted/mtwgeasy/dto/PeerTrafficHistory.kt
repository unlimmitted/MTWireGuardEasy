package ru.unlimmitted.mtwgeasy.dto

data class PeerTrafficHistory(
    var peerId: String? = null,
    var peerName: String? = null,
    var totalRx: Long = 0,
    var totalTx: Long = 0,
    var points: List<PeerTrafficPoint> = emptyList(),
)
