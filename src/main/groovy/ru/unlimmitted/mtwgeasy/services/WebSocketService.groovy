package ru.unlimmitted.mtwgeasy.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.CrossOrigin
import ru.unlimmitted.mtwgeasy.dto.MikroTikInfo
import ru.unlimmitted.mtwgeasy.dto.Peer
import ru.unlimmitted.mtwgeasy.dto.TrafficRate

@Service
@CrossOrigin
class WebSocketService {
	@Autowired
	private SimpMessagingTemplate simpMessaging

	void sendPeers(List<Peer> message) {
		simpMessaging.convertAndSend("/topic/peers/", message)
	}

	void sendInterfaces(MikroTikInfo message) {
		simpMessaging.convertAndSend("/topic/interface/", message)
	}

	void sendTrafficInterface(List<TrafficRate> message) {
		simpMessaging.convertAndSend("/topic/trafficInInterface/", message)
	}
}
