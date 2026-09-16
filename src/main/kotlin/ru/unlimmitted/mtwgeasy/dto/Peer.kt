package ru.unlimmitted.mtwgeasy.dto

data class Peer(
    var id: String? = null,
    var name: String? = null,
    var peerInterface: String? = null,
    var publicKey: String? = null,
    var endpoint: String? = null,
    var endpointPort: String? = null,
    var allowedAddress: String? = null,
    var presharedKey: String? = null,
    var rx: String? = null,
    var tx: String? = null,
    var currentEndpointAddress: String? = null,
    var currentEndpointPort: String? = null,
    var lastHandshake: String? = null,
    var privateKey: String? = null,
    var doubleVpn: Boolean = false,
)
