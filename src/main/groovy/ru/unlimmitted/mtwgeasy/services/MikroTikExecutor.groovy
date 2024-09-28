package ru.unlimmitted.mtwgeasy.services

import com.fasterxml.jackson.databind.ObjectMapper
import me.legrange.mikrotik.ApiConnection
import ru.unlimmitted.mtwgeasy.dto.MtSettings


import ru.unlimmitted.mtwgeasy.dto.WgInterface

import java.util.regex.Matcher
import java.util.regex.Pattern

class MikroTikExecutor {

	ApiConnection connect
	MtSettings settings
	List<WgInterface> wgInterfaces

	final String mikrotikGateway = System.getenv("GATEWAY")

	private String mikrotikUser = System.getenv("MIKROTIK_USER")

	private String mikrotikPassword = System.getenv("MIKROTIK_PASSWORD")

	final String settingsFile = "WGMTSettings.conf"

	Boolean isConfigured = false

	MikroTikExecutor() {
		initializeConnection()
	}

	protected void initializeConnection() {
		try {
			connect = ApiConnection.connect(mikrotikGateway)
			connect.login(mikrotikUser, mikrotikPassword)
			connect.setTimeout(5_000)
			setIsConfigured()
			if (isConfigured) {
				setSettings()
				setWgInterfaces()
			}
		} catch (Exception e) {
			if (e.message.contains("timed out")) {
				throw new RuntimeException("Time out Error")
			} else {
				throw new RuntimeException("Failed to connect to MikroTik: $e")
			}
		}
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

	private MtSettings readSettings() {
		ObjectMapper objectMapper = new ObjectMapper()
		if (isSettings()) {
			return objectMapper.readValue(
					executeCommand("/file/print where name=\"${settingsFile}\"")
							.contents
							.first
							.replace("\\\"", "\""),
					MtSettings.class
			)
		} else {
			MtSettings mtSettings = new MtSettings()
			return mtSettings
		}
	}

	Boolean isSettings() {
		return !executeCommand("/file/print where name=\"${settingsFile}\"").isEmpty()
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
				wgInterface.isRouting = executeCommand(
						"/ip/route/print where comment=\"WGMTEasy\""
				).gateway.first == it.get("name")
			}
			return wgInterface

		}
	}

	private void reconnect() {
		try {
			initializeConnection()
		} catch (Exception e) {
			throw new RuntimeException("Reconnection failed: ${e.message}", e)
		}
	}

	List<Map<String, String>> executeCommand(String command) {
		try {
			if (connect.connected) {
				return connect.execute(command)
			} else {
				reconnect()
			}
		} catch (Exception e) {
			if (e.message.contains("timed out")) {
				if (!connect.connected) {
					connect.close()
				}
				reconnect()
				return connect.execute(command)
			} else {
				throw new RuntimeException("Failed to execute command: $command: ${e.message}", e)
			}
		}
	}

	Integer getHostNumber() {
		List<Integer> results = new ArrayList<>()
		executeCommand("/interface/wireguard/peers/print").forEach {
			String regex = "(?:\\d+\\.){3}(\\d{1,3})\\/\\d+"
			Pattern pattern = Pattern.compile(regex)
			Matcher matcher = pattern.matcher(it.get('allowed-address'))
			if (matcher.find()) {
				results.add(matcher.group(1).toInteger())
			}
		}
		return results.size() > 0 ? results.max() + 1 : 1
	}
}
