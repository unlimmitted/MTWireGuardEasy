package ru.unlimmitted.mtwgeasy.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SchedulerService {

	@Autowired
	MikroTikService mikroTikService

	@Autowired
	WebSocketService webSocketService

	@Autowired
	MikroTikFiles mikroTikFiles

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

	@Scheduled(cron = "*/30 * * * * *")
	void saveInterfaceTraffic() {
		if (mikroTikService.isConfigured) {
			mikroTikFiles.saveInterfaceTraffic()
		}
	}

	@Scheduled(cron = "0 * * * * *")
	void sendInterfaceTraffic() {
		if (mikroTikService.isConfigured) {
			webSocketService.sendTrafficInterface(mikroTikFiles.getTrafficByMinutes())
		}
	}
}
