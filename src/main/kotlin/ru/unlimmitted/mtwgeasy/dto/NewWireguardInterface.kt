package ru.unlimmitted.mtwgeasy.dto

data class NewWireguardInterface(
    var name: String? = null,
    var ipAddress: String? = null,
    var allowedAddress: String? = null,
    var endpoint: String? = null,
    var endpointPort: String? = null,
    var publicKey: String? = null,
    var presharedKey: String? = null,
    var privateKey: String? = null,
)
