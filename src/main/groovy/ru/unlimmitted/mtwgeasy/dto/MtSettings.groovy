package ru.unlimmitted.mtwgeasy.dto

class MtSettings {
	String localWgInterfaceName = "WGMTEasyIn"
	String externalWgInterfaceName = "WGMTEasyOut"
	String toVpnAddressList = "WGMTEasyToVpnAddresses"
	String toVpnTableName = "WGMTEasyToVpnTable"
	String localWgNetwork
	String localWgEndpoint
	String localWgEndpointPort
	String localNetwork
	String ipAddress
	String allowedAddress
	String endpoint
	String endpointPort
	String publicKey
	String privateKey
	String wanInterfaceName
	String presharedKey
	Boolean vpnChainMode
}
