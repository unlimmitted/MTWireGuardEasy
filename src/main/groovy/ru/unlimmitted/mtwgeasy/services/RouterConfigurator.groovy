package ru.unlimmitted.mtwgeasy.services

import com.fasterxml.jackson.databind.ObjectMapper
import ru.unlimmitted.mtwgeasy.dto.MikroTikSettings

class RouterConfigurator extends MikroTikExecutor {

	private final MikroTikSettings routerSettings

	RouterConfigurator(MikroTikSettings routerSettings) {
		super()
		this.routerSettings = routerSettings
	}

	private void createBackup() {
		executeCommand("/system/backup/save name=\"before WGMTEASY\"")
	}

	private void createInterfaces() {
		String query = """
				|/interface/wireguard/add
				|name="${routerSettings.inputWgInterfaceName}" 
				|mtu=1400
				|listen-port=${routerSettings.inputWgEndpointPort}
				""".stripMargin().replace("\n", " ")
		executeCommand(query)
		if (routerSettings.vpnChainMode) {
			query = """
					|/interface/wireguard/add name="${routerSettings.externalWgInterfaceName}" 
					|mtu=1400
					|listen-port=${routerSettings.endpointPort}
					|private-key="${routerSettings.externalWgPrivateKey}"
					""".stripMargin().replace("\n", " ")
			executeCommand(query)
		}
	}

	private void createExternalPeer() {
		String query = """
				|/interface/wireguard/peers/add name="ExternalWG"
				|interface="${routerSettings.externalWgInterfaceName}"
				|public-key="${routerSettings.externalWgPublicKey}"
				|${routerSettings.externalWgPresharedKey !== null ? "preshared-key='${routerSettings.externalWgPresharedKey}'" : ''}
				|endpoint-address=${routerSettings.endpoint}
				|endpoint-port=${routerSettings.endpointPort} allowed-address="${routerSettings.allowedAddress}"
				|persistent-keepalive=20
				""".stripMargin().replace("\n", " ")
		executeCommand(query)
	}

	private void createRoutingTable() {
		executeCommand("/routing/table/add disabled=no fib=True name=${routerSettings.toVpnTableName}")
	}

	private void createIpRule() {
		String wgAddress = routerSettings.inputWgAddress.split("/").first()
		String query = """
			|/ip/address/add
			|address=${wgAddress}/24
			|interface=${routerSettings.inputWgInterfaceName}
			""".stripMargin().replace("\n", " ")
		executeCommand(query)
		if (routerSettings.vpnChainMode) {
			query = """
				|/ip/address/add
				|address=${routerSettings.ipAddress.split("/").first()}/24
				|interface=${routerSettings.externalWgInterfaceName}
				""".stripMargin().replace("\n", " ")
			executeCommand(query)

			wgAddress += "/24"
			String mangleQuery = """
					|/ip/firewall/mangle/add
					|action=mark-routing 
					|chain=prerouting 
					|src-address=${wgAddress}
					|dst-address=!${wgAddress} 
					|src-address-list="${routerSettings.toVpnAddressList}"
					|in-interface="${routerSettings.inputWgInterfaceName}"
					|new-routing-mark="${routerSettings.toVpnTableName}"
					|passthrough=yes
			""".stripMargin().replace("\n", " ")
			executeCommand(mangleQuery)

			String routesQuery = """
					|/ip/route/add
					|comment="WGMTEasy"
					|distance=1
					|dst-address=0.0.0.0/0
					|gateway=${routerSettings.externalWgInterfaceName}
					|routing-table=${routerSettings.toVpnTableName}
					|scope=30
					|suppress-hw-offload=no
					|target-scope=10
					""".stripMargin().replace("\n", " ")
			executeCommand(routesQuery)
		}
	}

	private void saveSettings() {
		ObjectMapper mapper = new ObjectMapper()
		String res = mapper.writeValueAsString(routerSettings).replace("\"", "\\\"")
		executeCommand("/file/add name=\"${settingsFile}\" contents='${res}'")
		setSettings()
	}

	private void createPortForwardRule() {
		String forwardQuery = """
				|/ip/firewall/nat/add
				|comment="WGMTEasyFWD"
				|action=dst-nat
				|chain=dstnat
				|protocol=udp
				|dst-port=${routerSettings.inputWgEndpointPort}
				|in-interface=${routerSettings.wanInterfaceName}
				|to-addresses=${mikrotikGateway}
		""".stripMargin().replace("\n", " ")
		executeCommand(forwardQuery)
		if (routerSettings.vpnChainMode) {
			String masqueradeQuery = """
				|/ip/firewall/nat/add
				|comment="WGMTEasyFWD"
				|action=masquerade
				|chain=srcnat
				|out-interface=${routerSettings.externalWgInterfaceName}
			""".stripMargin().replace("\n", " ")
			executeCommand(masqueradeQuery)
		}
	}

	void run() {
		try {
			createBackup()
			createInterfaces()
			if (routerSettings.vpnChainMode) {
				createExternalPeer()
			}
			setWgInterfaces()
			createRoutingTable()
			createIpRule()
			saveSettings()
			createPortForwardRule()
			setIsConfigured()
		} catch (Exception ex) {
			throw new RuntimeException("Configuration error: ${ex.message}", ex)
		}
	}
}
