package ru.unlimmitted.mtwgeasy.dto

data class WgInterface(
    var name: String? = null,
    var listenPort: String? = null,
    var publicKey: String? = null,
    var disabled: Boolean = false,
    var privateKey: String? = null,
    var mtu: String? = null,
    var rxByte: String = "0",
    var txByte: String = "0",
    var isRouting: Boolean = false,
)
