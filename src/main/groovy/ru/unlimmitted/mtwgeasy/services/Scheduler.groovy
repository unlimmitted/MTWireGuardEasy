package ru.unlimmitted.mtwgeasy.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class Scheduler {

	@Autowired
	MikroTikService mikroTikService

	@Autowired
	WebSocketService webSocketService

	@Scheduled(cron = '*/15 * * * * *')
	void sendInterfaces () {
		webSocketService.sendInterfaces(mikroTikService.getMtInfo())
	}

	@Scheduled(cron = '0 */1 * * * *')
	void sendPeers() {
		webSocketService.sendPeers(mikroTikService.getPeers())
	}
}
