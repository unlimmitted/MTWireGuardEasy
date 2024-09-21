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

	String mikrotikGateway = System.getenv("GATEWAY")

	String mikrotikUser = System.getenv("MIKROTIK_USER")

	String mikrotikPassword = System.getenv("MIKROTIK_PASSWORD")

	MikroTikExecutor() {
		initializeConnection()
	}

	protected void initializeConnection() {
		try {
			connect = ApiConnection.connect(mikrotikGateway)
			connect.login(mikrotikUser, mikrotikPassword)
			connect.setTimeout(5_000)
			wgInterfaces = getInterfaces()
			settings = readSettings()
		} catch (Exception e) {
			throw new RuntimeException("Failed to connect to MikroTik", e)
		}
	}

	private MtSettings readSettings() {
		ObjectMapper objectMapper = new ObjectMapper()
		if (isSettings()) {
			return objectMapper.readValue(
					executeCommand('/file/print').find {
						it.name == 'WGMTSettings.conf'
					}.contents.replace("\\\"", "\""),
					MtSettings.class
			)
		} else {
			MtSettings mtSettings = new MtSettings()
			return mtSettings
		}
	}

	Boolean isSettings() {
		return executeCommand('/file/print').find { it.name == 'WGMTSettings.conf' }
	}

	private List<WgInterface> getInterfaces() {
		return executeCommand('/interface/wireguard/print').collect {
			WgInterface wgInterface = new WgInterface()
			wgInterface.name = it.get('name')
			wgInterface.running = it.get('running').toBoolean()
			wgInterface.privateKey = it.get('private-key')
			wgInterface.publicKey = it.get('public-key')
			wgInterface.disabled = it.get('disabled').toBoolean()
			wgInterface.listenPort = it.get('listen-port')
			wgInterface.mtu = it.get('mtu')
			Map<String, String> intStats = executeCommand("/interface/print stats where name=${wgInterface.name}").first()
			wgInterface.rxByte = intStats.get("rx-byte")
			wgInterface.txByte = intStats.get("tx-byte")
			return wgInterface
		}
	}

	private void reconnect() {
		try {
			initializeConnection()
		} catch (Exception e) {
			throw new RuntimeException("Reconnection failed: ${e.message}")
		}
	}

	List<Map<String, String>> executeCommand(String command) {
		try {
			return connect.execute(command)
		} catch (Exception e) {
			if (e.message.contains("timed out")) {
				reconnect()
				return executeCommand(command)
			} else {
				throw new RuntimeException("Failed to execute command: $command: ${e.message}")
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
