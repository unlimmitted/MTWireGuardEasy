package ru.unlimmitted.mtwgeasy.dto

import java.time.Instant

data class TrafficRate(
    var tx: Long = 0,
    var rx: Long = 0,
    var time: Long = 0,
) {
    constructor(tx: Long, rx: Long, time: Instant) : this(tx, rx, time.epochSecond)
}
