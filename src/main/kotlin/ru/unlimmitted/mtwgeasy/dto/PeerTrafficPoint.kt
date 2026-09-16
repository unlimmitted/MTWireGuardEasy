package ru.unlimmitted.mtwgeasy.dto

data class PeerTrafficPoint(
    var time: Long = 0,
    var rx: Long = 0,
    var tx: Long = 0,
)
