package ru.unlimmitted.mtwgeasy.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class MikroTikSettings {
	String inputWgInterfaceName = "WGMTEasyIn"
	String externalWgInterfaceName = "WGMTEasyOut"
	String toVpnAddressList = "WGMTEasyToVpnAddresses"
	String toVpnTableName = "WGMTEasyToVpnTable"
	String inputWgAddress
	String inputWgEndpoint
	String inputWgEndpointPort
	String localNetwork
	String ipAddress
	String allowedAddress
	String endpoint
	String endpointPort
	String externalWgPublicKey
	String externalWgPrivateKey
	String wanInterfaceName
	String externalWgPresharedKey
	Boolean vpnChainMode
}
