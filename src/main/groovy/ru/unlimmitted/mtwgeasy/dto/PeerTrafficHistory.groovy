package ru.unlimmitted.mtwgeasy.dto

class PeerTrafficHistory {
    String peerId
    String peerName
    Long totalRx = 0L
    Long totalTx = 0L
    List<PeerTrafficPoint> points = []
}
