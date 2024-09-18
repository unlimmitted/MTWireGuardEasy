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

	@Scheduled(fixedDelay = 15, timeUnit = TimeUnit.SECONDS)
	void sendInterfaces() {
		webSocketService.sendInterfaces(mikroTikService.getMtInfo())
	}

	@Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
	void sendPeers() {
		webSocketService.sendPeers(mikroTikService.getPeers())
	}

	@Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
	void saveInterfaceTraffic() {
		mikroTikService.saveInterfaceTraffic()
	}
}
