package ru.unlimmitted.mtwgeasy.services

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

	void initializeConnection() {
		try {
			connect = ApiConnection.connect(mikrotikGateway)
			connect.setTimeout(500)
			connect.login(mikrotikUser, mikrotikPassword)
			wgInterfaces = getInterfaces()
		} catch (Exception e) {
			throw new RuntimeException("Failed to connect to MikroTik", e)
		}
	}

	List<WgInterface> getInterfaces() {
		List<WgInterface> wgInterfaces = new ArrayList<>()
		executeCommand('/interface/wireguard/print').forEach {
			WgInterface wgInterface = new WgInterface()
			wgInterface.name = it.get('name')
			wgInterface.running = it.get('running').toBoolean()
			wgInterface.privateKey = it.get('private-key')
			wgInterface.publicKey = it.get('public-key')
			wgInterface.disabled = it.get('disabled').toBoolean()
			wgInterface.listenPort = it.get('listen-port')
			wgInterface.mtu = it.get('mtu')
			wgInterfaces.add(wgInterface)
		}
		wgInterfaces.forEach {
			Map<String, String> intStats = executeCommand("/interface/print stats where name=${it.name}").first()
			it.rxByte = intStats.get("rx-byte")
			it.txByte = intStats.get("tx-byte")
		}
		return wgInterfaces
	}

	void reconnect() {
		try {
			initializeConnection()
		} catch (Exception e) {
			throw new RuntimeException("Reconnection failed", e)
		}
	}

	List<Map<String, String>> executeCommand(String command) {
		try {
			return connect.execute(command)
		} catch (Exception e) {
			if (e.message.contains("timed out")) {
				reconnect()
				return connect.execute(command)
			} else {
				throw new RuntimeException("Failed to execute command: $command", e)
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
