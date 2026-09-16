package ru.unlimmitted.mtwgeasy.services

import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.CrossOrigin
import ru.unlimmitted.mtwgeasy.dto.MikroTikInfo
import ru.unlimmitted.mtwgeasy.dto.Peer
import ru.unlimmitted.mtwgeasy.dto.TrafficRate

@Service
@CrossOrigin
class WebSocketService(
    private val simpMessaging: SimpMessagingTemplate,
) {
    fun sendPeers(message: List<Peer>) {
        simpMessaging.convertAndSend("/topic/peers/", message)
    }

    fun sendInterfaces(message: MikroTikInfo) {
        simpMessaging.convertAndSend("/topic/interface/", message)
    }

    fun sendTrafficInterface(message: List<TrafficRate>) {
        simpMessaging.convertAndSend("/topic/trafficInInterface/", message)
    }
}
