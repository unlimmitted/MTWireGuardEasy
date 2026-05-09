package ru.unlimmitted.mtwgeasy.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import ru.unlimmitted.mtwgeasy.dto.TrafficRate
import ru.unlimmitted.mtwgeasy.entity.TrafficRateEntity
import ru.unlimmitted.mtwgeasy.repository.TrafficRateRepository

import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class MikroTikFiles extends MikroTikExecutor {

    private static final Logger log = LoggerFactory.getLogger(MikroTikFiles.class)

    @Autowired
    TrafficRateRepository trafficRateRepository

    void saveInterfaceTraffic() {
        def inputInterface = wgInterfaces.find { it.name == settings.inputWgInterfaceName }
        if (inputInterface == null) {
            log.warn("Input WireGuard interface '{}' not found, skipping traffic save", settings.inputWgInterfaceName)
            return
        }

        TrafficRateEntity entity = new TrafficRateEntity(
                tx: inputInterface.txByte.toLong(),
                rx: inputInterface.rxByte.toLong(),
                time: Instant.now().epochSecond
        )
        trafficRateRepository.save(entity)

        long oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS).epochSecond
        trafficRateRepository.deleteOlderThan(oneHourAgo)

        log.debug("Traffic saved: tx={}, rx={}", entity.tx, entity.rx)
    }

    List<TrafficRate> getTrafficByMinutes() {
        long oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS).epochSecond
        List<TrafficRateEntity> records = trafficRateRepository.findAllSince(oneHourAgo)

        if (records.size() < 2) {
            log.debug("Not enough traffic records to calculate rate, got {}", records.size())
            return []
        }

        return (1..<records.size()).collect { int i ->
            new TrafficRate(
                    Math.max(0L, (records[i].tx - records[i - 1].tx) / 1_048_576 as Long),
                    Math.max(0L, (records[i].rx - records[i - 1].rx) / 1_048_576 as Long),
                    Instant.ofEpochSecond(records[i].time)
            )
        }
    }
}
