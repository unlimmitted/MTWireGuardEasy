package ru.unlimmitted.mtwgeasy.services

import com.fasterxml.jackson.databind.ObjectMapper
import me.legrange.mikrotik.ApiConnection
import ru.unlimmitted.mtwgeasy.dto.MtSettings
import ru.unlimmitted.mtwgeasy.dto.WgInterface

class MikroTikExecutor {

	ApiConnection connect
	MtSettings settings
	List<WgInterface> wgInterfaces

	MikroTikExecutor() {
		initializeConnection()
	}

	void initializeConnection() {
		try {
			connect = ApiConnection.connect(System.getenv("GATEWAY"))
			connect.setTimeout(500)
			connect.login(System.getenv("MIKROTIK_USER"), System.getenv("MIKROTIK_PASSWORD"))
			settings = readSettings()
			wgInterfaces = getInterfaces()
		} catch (Exception e) {
			throw new RuntimeException("Failed to connect to MikroTik", e)
		}
	}

	MtSettings readSettings() {
		ObjectMapper objectMapper = new ObjectMapper()
		return objectMapper.readValue(
				executeCommand('/file/print').find {
					it.name == 'WG-WebMode-Settings.conf'
				}.contents,
				MtSettings.class
		)
	}

	List<WgInterface> getInterfaces() {
		List<WgInterface> wgInterfaces = new ArrayList<>()
		executeCommand('/interface/wireguard/print').forEach({
			WgInterface wgInterface = new WgInterface()
			wgInterface.name = it.get('name')
			wgInterface.running = it.get('running').toBoolean()
			wgInterface.privateKey = it.get('private-key')
			wgInterface.publicKey = it.get('public-key')
			wgInterface.disabled = it.get('disabled').toBoolean()
			wgInterface.listenPort = it.get('listen-port')
			wgInterface.mtu = it.get('mtu')
			wgInterfaces.add(wgInterface)
		})
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
}
