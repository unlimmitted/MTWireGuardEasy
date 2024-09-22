package ru.unlimmitted.mtwgeasy.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import java.util.concurrent.TimeUnit

@Service
class Scheduler {

	@Autowired
	MikroTikService mikroTikService

	@Autowired
	WebSocketService webSocketService

	@Scheduled(cron = "0 * * * * *")
	void sendInterfaces() {
		if (mikroTikService.isConfigured) {
			webSocketService.sendInterfaces(mikroTikService.getMtInfo())
		}
	}

	@Scheduled(cron = "*/25 * * * * *")
	void sendPeers() {
		if (mikroTikService.isConfigured) {
			webSocketService.sendPeers(mikroTikService.getPeers())
		}
	}

	@Scheduled(cron = "0 * * * * *")
	void saveInterfaceTraffic() {
		if (mikroTikService.isConfigured) {
			mikroTikService.saveInterfaceTraffic()
		}
	}

	@Scheduled(cron = "0 * * * * *")
	void sendInterfaceTraffic() {
		if (mikroTikService.isConfigured) {
			webSocketService.sendTrafficInterface(mikroTikService.getTrafficByMinutes())
		}
	}
}
