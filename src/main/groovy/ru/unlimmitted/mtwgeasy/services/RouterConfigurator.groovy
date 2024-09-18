package ru.unlimmitted.mtwgeasy.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.whispersystems.curve25519.Curve25519
import org.whispersystems.curve25519.Curve25519KeyPair
import ru.unlimmitted.mtwgeasy.dto.MtSettings

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
		executeCommand("/interface/wireguard/add name=\"${routerSettings.inputWgInterfaceName}\" mtu=1400 " +
				"listen-port=${routerSettings.inputWgEndpointPort}")
		if (routerSettings.vpnChainMode) {
			executeCommand("/interface/wireguard/add name=\"${routerSettings.externalWgInterfaceName}\" mtu=1400 " +
					"listen-port=${routerSettings.endpointPort}")
		}
	}

	private void createExternalPeer() {
		String query = "/interface/wireguard/peers/add name=\"ExternalWG\" " +
				"interface=\"${routerSettings.externalWgInterfaceName}\" public-key=\"${routerSettings.externalWgPublicKey}\" " +
				"preshared-key=\"${routerSettings.externalWgPresharedKey}\" endpoint-address=${routerSettings.endpoint} " +
				"endpoint-port=${routerSettings.endpointPort} allowed-address=\"${routerSettings.allowedAddress}\" " +
				"persistent-keepalive=20"
		executeCommand(query)
	}

	private void createInteriorPeer() {
		Curve25519KeyPair keyPair = Curve25519.getInstance(Curve25519.JAVA).generateKeyPair()
		String pub = Base64.getEncoder().encodeToString(keyPair.getPublicKey())
		String address = routerSettings.inputWgAddress.replace(
				routerSettings.inputWgAddress.split("\\.").last(),
				getHostNumber() + "/${routerSettings.inputWgAddress.split("\\.").last().split("/").last()}"
		)

		String query = "/interface/wireguard/peers/add interface=\"${routerSettings.inputWgInterfaceName}\" " +
				"public-key=\"${pub}\" allowed-address=${address} " +
				"name=\"InteriorWG\" persistent-keepalive=20"
		executeCommand(query)
	}

	private void createRoutingTable() {
		executeCommand("/routing/table/add disabled=no fib=True name=${routerSettings.toVpnTableName}")
	}

	private void createIpRule() {
		executeCommand(
				"/ip/address/add address=${routerSettings.inputWgAddress.split("/").first()}/24 " +
						"interface=${routerSettings.inputWgInterfaceName}"
		)
		if (routerSettings.vpnChainMode) {
			executeCommand(
					"/ip/address/add address=${routerSettings.ipAddress.split("/").first()}/24 " +
							"interface=${routerSettings.inputWgInterfaceName}"
			)

			String address = routerSettings.inputWgAddress.split("/").first() + "/24"
			String mangleQuery = "/ip/firewall/mangle/add action=mark-routing chain=prerouting src-address=${address} " +
					"dst-address=!${address} src-address-list=\"${routerSettings.toVpnAddressList}\" " +
					"in-interface=\"${routerSettings.inputWgInterfaceName}\" " +
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

	void createPortForwardRule() {
		String forwardQuery = "/ip/firewall/nat comment=\"WGMTEasyFWD\" action=dst-nat chain=dstnat protocol=udp " +
				"dst-port=${routerSettings.inputWgEndpointPort} in-interface=${routerSettings.wanInterfaceName} " +
				"to-addresses=${mikrotikGateway}"
		executeCommand(forwardQuery)
		String masqueradeQuery = "/ip/firewall/nat comment=\"WGMTEasyFWD\" action=masquerade chain=srcnat " +
				"out-interface=${routerSettings.externalWgInterfaceName}"
		executeCommand(masqueradeQuery)

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
		createPortForwardRule()
	}
}
