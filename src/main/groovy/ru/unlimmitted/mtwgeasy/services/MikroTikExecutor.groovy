package ru.unlimmitted.mtwgeasy.services

import com.fasterxml.jackson.databind.ObjectMapper
import me.legrange.mikrotik.ApiConnection
import me.legrange.mikrotik.MikrotikApiException
import ru.unlimmitted.mtwgeasy.dto.MikroTikSettings
import ru.unlimmitted.mtwgeasy.dto.WgInterface

import java.util.regex.Matcher
import java.util.regex.Pattern

class MikroTikExecutor {

	ApiConnection connect
	MikroTikSettings settings
	List<WgInterface> wgInterfaces
	Boolean isConfigured

	final static String settingsFile = "WGMTSettings.conf"
	final String mikrotikGateway = System.getenv("GATEWAY")
	private final String mikrotikUser = System.getenv("MIKROTIK_USER")
	private final String mikrotikPassword = System.getenv("MIKROTIK_PASSWORD")

	MikroTikExecutor() {
		initializeConnection()
	}

	List<Map<String, String>> executeCommand(String command) {
		try {
			return connect.execute(command)
		} catch (MikrotikApiException e) {
			if (e.message?.contains("timed out")) {
				reconnect()
				return executeCommand(command)
			} else {
				throw new RuntimeException("Failed to execute command: $command: ${e.message}", e)
			}
		} catch (Exception e) {
			throw new RuntimeException("Unknown exception", e)
		}
	}

	Integer getHostNumber() {
		List<Integer> results = []
		executeCommand("/interface/wireguard/peers/print").forEach {
			String regex = "(?:\\d+\\.){3}(\\d{1,3})/\\d+"
			Matcher matcher = Pattern.compile(regex).matcher(it.get('allowed-address'))
			if (matcher.find()) {
				results.add(matcher.group(1).toInteger())
			}
		}
		return results.size() > 0 ? results.max() + 1 : 1
	}

	Boolean isSettings() {
		return executeCommand('/file/print').find { it.name == settingsFile } != null
	}

	protected void initializeConnection() {
		try {
			if (connect != null && connect.isConnected()) {
				connect.close()
			}
			connect = ApiConnection.connect(mikrotikGateway)
			connect.login(mikrotikUser, mikrotikPassword)
			connect.setTimeout(1_000)
			setIsConfigured()
			if (isConfigured) {
				setSettings()
				setWgInterfaces()
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to connect to MikroTik: ${e.message}", e)
		}
	}

	protected void reconnect() {
		initializeConnection()
	}

	void setIsConfigured() {
		isConfigured = isSettings()
	}

	void setWgInterfaces() {
		wgInterfaces = getInterfaces()
	}

	void setSettings() {
		settings = readSettings()
	}

	private MikroTikSettings readSettings() {
		ObjectMapper objectMapper = new ObjectMapper()
		if (isSettings()) {
			String configContent = executeCommand("/file/print where name=\"${settingsFile}\"")
					.get(0)?.get('contents')?.replace("\\\"", "\"")
			return objectMapper.readValue(configContent, MikroTikSettings.class)
		} else {
			return new MikroTikSettings()
		}
	}

	private List<WgInterface> getInterfaces() {
		return executeCommand('/interface/wireguard/print').collect {
			WgInterface wgInterface = new WgInterface()
			wgInterface.name = it.get('name')
			wgInterface.privateKey = it.get('private-key')
			wgInterface.publicKey = it.get('public-key')
			wgInterface.listenPort = it.get('listen-port')
			wgInterface.mtu = it.get('mtu')
			wgInterface.disabled = it.get('disabled').toBoolean()
			Map<String, String> intStats = executeCommand(
					"/interface/print stats where name=${wgInterface.name}"
			).first()
			wgInterface.rxByte = intStats.get("rx-byte")
			wgInterface.txByte = intStats.get("tx-byte")
			if (it.get("name") !== settings.inputWgInterfaceName) {
				String ipRouteName = System.getenv("IP_ROUTE_NAME") ?: "WGMTEasy"
				wgInterface.isRouting = executeCommand(
						"/ip/route/print where comment=\"${ipRouteName}\""
				).gateway.first == it.get("name")
			}
			return wgInterface

		}
	}
}
