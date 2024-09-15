package ru.unlimmitted.mtwgeasy.services

import com.fasterxml.jackson.databind.ObjectMapper
import ru.unlimmitted.mtwgeasy.dto.MtSettings

import java.util.regex.Matcher
import java.util.regex.Pattern

class RouterConfigurator extends MikroTikExecutor {

	MtSettings routerSettings

	RouterConfigurator(MtSettings routerSettings) {
		super()
		connect = super.connect
		this.routerSettings = routerSettings
		wgInterfaces = super.wgInterfaces
	}

	private void createBackup() {
		executeCommand("/system/backup/save name=\"before WGMTEASY\"")
	}

	private void createInterfaces() {
		executeCommand("/interface/wireguard/add name=\"${routerSettings.localWgInterfaceName}\" mtu=1400 " +
				"listen-port=${routerSettings.localWgEndpointPort}")
		if (routerSettings.vpnChainMode) {
			executeCommand("/interface/wireguard/add name=\"${routerSettings.externalWgInterfaceName}\" mtu=1400 " +
					"listen-port=${routerSettings.endpointPort}")
		}
	}

	private void createExternalPeer() {
		String query = "/interface/wireguard/peers/add name=\"ExternalWG\" " +
				"interface=\"${routerSettings.externalWgInterfaceName}\" public-key=\"${routerSettings.publicKey}\" " +
				"preshared-key=\"${routerSettings.presharedKey}\" endpoint-address=${routerSettings.endpoint} " +
				"endpoint-port=${routerSettings.endpointPort} allowed-address=\"${routerSettings.allowedAddress}\" " +
				"persistent-keepalive=20"
		executeCommand(query)
	}

	private void createInteriorPeer() {
		WireGuardKeyGen wireGuardKeyGen = new WireGuardKeyGen()
		String address = routerSettings.localWgNetwork.replace(
				routerSettings.localWgNetwork.split("\\.").last(),
				getHostNumber() + "/${routerSettings.localWgNetwork.split("\\.").last().split("/").last()}"
		)

		String query = "/interface/wireguard/peers/add interface=\"${routerSettings.localWgInterfaceName}\" " +
				"public-key=\"${wireGuardKeyGen.keyPair().publicKey}\" allowed-address=${address} " +
				"name=\"InteriorWG\" persistent-keepalive=20"
		executeCommand(query)
	}

	private void createRoutingTable() {
		executeCommand("/routing/table/add disabled=no fib=True name=${routerSettings.toVpnTableName}")
	}

	private void createIpRule() {
		executeCommand(
				"/ip/address/add address=${routerSettings.localWgNetwork.split("/").first()}/24 " +
						"interface=${routerSettings.localWgInterfaceName}"
		)
		if (routerSettings.vpnChainMode) {
			executeCommand(
					"/ip/address/add address=${routerSettings.ipAddress.split("/").first()}/24 " +
							"interface=${routerSettings.localWgInterfaceName}"
			)

			String address = routerSettings.localWgNetwork.split("/").first() + "/24"
			String mangleQuery = "/ip/firewall/mangle/add action=mark-routing chain=prerouting src-address=${address} " +
					"dst-address=!${address} src-address-list=\"${routerSettings.toVpnAddressList}\" " +
					"in-interface=\"${routerSettings.localWgInterfaceName}\" " +
					"new-routing-mark=\"${routerSettings.toVpnTableName}\" passthrough=yes"
			executeCommand(mangleQuery)

			String routesQuery = "/ip/route/add distance=1 dst-address=0.0.0.0/0 " +
					"gateway=${routerSettings.externalWgInterfaceName} " +
					"routing-table=${routerSettings.toVpnTableName} scope=30 suppress-hw-offload=no target-scope=10"
			executeCommand(routesQuery)
		}
	}
	void saveSettings() {
		ObjectMapper mapper = new ObjectMapper()
		String res = mapper.writeValueAsString(routerSettings).replace("\"", "\\\"")
		executeCommand("/file/add name=\"WGMTSettings.conf\" contents='${res}'")
	}

	void run() {
		createBackup()
		createInterfaces()
		createInteriorPeer()
		if (routerSettings.vpnChainMode) {
			createExternalPeer()
		}
		createRoutingTable()
		createIpRule()
		saveSettings()
	}
}
