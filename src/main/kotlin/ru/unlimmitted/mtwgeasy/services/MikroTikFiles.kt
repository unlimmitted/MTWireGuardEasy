package ru.unlimmitted.mtwgeasy.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.unlimmitted.mtwgeasy.dto.TrafficRate
import ru.unlimmitted.mtwgeasy.entity.TrafficRateEntity
import ru.unlimmitted.mtwgeasy.repository.TrafficRateRepository
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class MikroTikFiles(
    private val trafficRateRepository: TrafficRateRepository,
    private val mikroTikService: MikroTikService,
) {
    fun saveInterfaceTraffic() {
        val settings = mikroTikService.settings
        if (settings == null) {
            log.debug("MikroTik settings are not loaded, skipping traffic save")
            return
        }
        val inputInterface = mikroTikService.wgInterfaces.find { it.name == settings.inputWgInterfaceName }
        if (inputInterface == null) {
            log.warn("Input WireGuard interface '{}' not found, skipping traffic save", settings.inputWgInterfaceName)
            return
        }

        val entity = TrafficRateEntity(
            tx = inputInterface.txByte.toLongOrNull() ?: 0,
            rx = inputInterface.rxByte.toLongOrNull() ?: 0,
            time = Instant.now().epochSecond,
        )
        trafficRateRepository.save(entity)
        trafficRateRepository.deleteOlderThan(Instant.now().minus(1, ChronoUnit.HOURS).epochSecond)
        log.debug("Traffic saved: tx={}, rx={}", entity.tx, entity.rx)
    }

    fun getTrafficByMinutes(): List<TrafficRate> {
        val oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS).epochSecond
        val records = trafficRateRepository.findAllSince(oneHourAgo)
        if (records.size < 2) {
            log.debug("Not enough traffic records to calculate rate, got {}", records.size)
            return emptyList()
        }
        return (1 until records.size).map { index ->
            TrafficRate(
                tx = maxOf(0, (records[index].tx - records[index - 1].tx) / BYTES_IN_MEBIBYTE),
                rx = maxOf(0, (records[index].rx - records[index - 1].rx) / BYTES_IN_MEBIBYTE),
                time = records[index].time,
            )
        }
    }

    companion object {
        private const val BYTES_IN_MEBIBYTE = 1_048_576L
        private val log = LoggerFactory.getLogger(MikroTikFiles::class.java)
    }
}
