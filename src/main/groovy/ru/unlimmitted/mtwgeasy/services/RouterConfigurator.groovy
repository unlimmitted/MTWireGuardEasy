package ru.unlimmitted.mtwgeasy.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.whispersystems.curve25519.Curve25519
import org.whispersystems.curve25519.Curve25519KeyPair
import ru.unlimmitted.mtwgeasy.dto.MtSettings

class RouterConfigurator extends MikroTikExecutor {

	private final MtSettings routerSettings

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
					""".stripMargin().replace("\n", " ")
			executeCommand(query)
		}
	}

	private void createExternalPeer() {
		String query = """
				|/interface/wireguard/peers/add name="ExternalWG"
				|interface="${routerSettings.externalWgInterfaceName}"
				|public-key="${routerSettings.externalWgPublicKey}
				|preshared-key="${routerSettings.externalWgPresharedKey}" 
				|endpoint-address=${routerSettings.endpoint}
				|endpoint-port=${routerSettings.endpointPort} allowed-address="${routerSettings.allowedAddress}"
				|persistent-keepalive=20
				""".stripMargin().replace("\n", " ")
		executeCommand(query)
	}

	private void createInteriorPeer() {
		Curve25519KeyPair keyPair = Curve25519.getInstance(Curve25519.JAVA).generateKeyPair()
		String pubKey = Base64.getEncoder().encodeToString(keyPair.getPublicKey())
		String ipAddress = routerSettings.inputWgAddress.split("\\.").last()
		String mask = ipAddress.split("/").last()
		String address = routerSettings.inputWgAddress.replace(ipAddress, "${getHostNumber()}/${mask}")
		String query = """
				|/interface/wireguard/peers/add 
				|interface="${routerSettings.inputWgInterfaceName}"
				|public-key="${pubKey}" 
				|allowed-address=${address}
				|name="InteriorWG"
				|persistent-keepalive=20
				""".stripMargin().replace("\n", " ")
		executeCommand(query)
	}

	private void createRoutingTable() {
		executeCommand("/routing/table/add disabled=no fib=True name=${routerSettings.toVpnTableName}")
	}

	private void createIpRule() {
		String wgAddress = routerSettings.inputWgAddress.split("/").first()
		def query = """
				|/ip/address/add
				|address=${wgAddress}/24
				|interface=${routerSettings.inputWgInterfaceName}
				""".stripMargin().replace("\n", " ")
		executeCommand(query)
		if (routerSettings.vpnChainMode) {
			query = """
					|/ip/address/add
					|address=${routerSettings.ipAddress.split("/").first()}/24
					|interface=${routerSettings.inputWgInterfaceName}
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
		executeCommand("/file/add name=\"WGMTSettings.conf\" contents='${res}'")
	}

	private void createPortForwardRule() {
		String forwardQuery = """
				|/ip/firewall/nat
				|comment="WGMTEasyFWD"
				|action=dst-nat
				|chain=dstnat
				|protocol=udp
				|dst-port=${routerSettings.inputWgEndpointPort}
				|in-interface=${routerSettings.wanInterfaceName}
				|to-addresses=${mikrotikGateway}
		""".stripMargin().replace("\n", " ")
		executeCommand(forwardQuery)
		String masqueradeQuery = """
				|/ip/firewall/nat
				|comment="WGMTEasyFWD"
				|action=masquerade
				|chain=srcnat
				|out-interface=${routerSettings.externalWgInterfaceName}
		""".stripMargin().replace("\n", " ")
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
