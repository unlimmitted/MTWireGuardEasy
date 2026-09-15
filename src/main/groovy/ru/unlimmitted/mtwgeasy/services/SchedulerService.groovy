package ru.unlimmitted.mtwgeasy.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class)

    @Autowired
    MikroTikService mikroTikService

    @Autowired
    WebSocketService webSocketService

    @Autowired
    MikroTikFiles mikroTikFiles

    @Autowired
    PeerTrafficService peerTrafficService

    @Scheduled(fixedDelay = 30_000L)
    void restoreConnection() {
        try {
            mikroTikService.reconnectIfNeeded()
        } catch (Exception e) {
            log.debug("MikroTik is still unavailable: {}", e.message)
        }
    }

    @Scheduled(cron = "*/15 * * * * *")
    void refreshAndSendInterfaces() {
        if (!mikroTikService.isConfigured) return
        try {
            mikroTikService.setWgInterfaces()
            webSocketService.sendInterfaces(mikroTikService.getMikroTikInfo())
        } catch (Exception e) {
            log.warn("Failed to refresh interfaces: {}", e.message)
        }
    }

    @Scheduled(cron = "*/15 * * * * *")
    void refreshAndSendPeers() {
        if (!mikroTikService.isConfigured) return
        try {
            def peers = mikroTikService.getPeers()
            try {
                peerTrafficService.saveSnapshots(peers)
            } catch (Exception e) {
                log.warn("Failed to save peer traffic: {}", e.message)
            }
            webSocketService.sendPeers(peers)
        } catch (Exception e) {
            log.warn("Failed to refresh peers: {}", e.message)
        }
    }

    @Scheduled(cron = "*/30 * * * * *")
    void saveInterfaceTraffic() {
        if (!mikroTikService.isConfigured) return
        try {
            mikroTikFiles.saveInterfaceTraffic()
        } catch (Exception e) {
            log.warn("Failed to save traffic: {}", e.message)
        }
    }

    @Scheduled(cron = "*/20 * * * * *")
    void sendInterfaceTraffic() {
        if (!mikroTikService.isConfigured) return
        try {
            webSocketService.sendTrafficInterface(mikroTikFiles.getTrafficByMinutes())
        } catch (Exception e) {
            log.warn("Failed to send traffic: {}", e.message)
        }
    }
}
