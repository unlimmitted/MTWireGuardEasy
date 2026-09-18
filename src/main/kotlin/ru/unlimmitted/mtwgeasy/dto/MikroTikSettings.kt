package ru.unlimmitted.mtwgeasy.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MikroTikSettings(
    var inputWgInterfaceName: String = "WGMTEasyIn",
    var externalWgInterfaceName: String = "WGMTEasyOut",
    var toVpnAddressList: String = "WGMTEasyToVpnAddresses",
    var toVpnTableName: String = "WGMTEasyToVpnTable",
    var inputWgAddress: String? = null,
    var inputWgEndpoint: String? = null,
    var inputWgEndpointPort: String? = null,
    var localNetwork: String? = null,
    var ipAddress: String? = null,
    var allowedAddress: String? = null,
    var endpoint: String? = null,
    var endpointPort: String? = null,
    var externalWgPublicKey: String? = null,
    var externalWgPrivateKey: String? = null,
    var wanInterfaceName: String? = null,
    var externalWgPresharedKey: String? = null,
    var vpnChainMode: Boolean = false,
    var doubleVpnInverted: Boolean = false,
)
