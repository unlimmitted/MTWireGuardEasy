package ru.unlimmitted.mtwgeasy.services

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SchedulerService(
    private val mikroTikService: MikroTikService,
    private val webSocketService: WebSocketService,
    private val mikroTikFiles: MikroTikFiles,
    private val peerTrafficService: PeerTrafficService,
) {
    @Scheduled(fixedDelay = 30_000L)
    fun restoreConnection() {
        try {
            mikroTikService.reconnectIfNeeded()
        } catch (exception: Exception) {
            log.debug("MikroTik is still unavailable: {}", exception.message)
        }
    }

    @Scheduled(cron = "*/15 * * * * *")
    fun refreshAndSendInterfaces() {
        if (!mikroTikService.isConfigured) return
        try {
            mikroTikService.setWgInterfaces()
            webSocketService.sendInterfaces(mikroTikService.getMikroTikInfo())
        } catch (exception: Exception) {
            log.warn("Failed to refresh interfaces: {}", exception.message)
        }
    }

    @Scheduled(cron = "*/15 * * * * *")
    fun refreshAndSendPeers() {
        if (!mikroTikService.isConfigured) return
        try {
            val peers = mikroTikService.getPeers()
            try {
                peerTrafficService.saveSnapshots(peers)
            } catch (exception: Exception) {
                log.warn("Failed to save peer traffic: {}", exception.message)
            }
            webSocketService.sendPeers(peers)
        } catch (exception: Exception) {
            log.warn("Failed to refresh peers: {}", exception.message)
        }
    }

    @Scheduled(cron = "*/30 * * * * *")
    fun saveInterfaceTraffic() {
        if (!mikroTikService.isConfigured) return
        try {
            mikroTikFiles.saveInterfaceTraffic()
        } catch (exception: Exception) {
            log.warn("Failed to save traffic: {}", exception.message)
        }
    }

    @Scheduled(cron = "*/20 * * * * *")
    fun sendInterfaceTraffic() {
        if (!mikroTikService.isConfigured) return
        try {
            webSocketService.sendTrafficInterface(mikroTikFiles.getTrafficByMinutes())
        } catch (exception: Exception) {
            log.warn("Failed to send traffic: {}", exception.message)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(SchedulerService::class.java)
    }
}
