package ru.unlimmitted.mtwgeasy.services

import org.junit.jupiter.api.Test
import ru.unlimmitted.mtwgeasy.dto.MikroTikSettings
import ru.unlimmitted.mtwgeasy.dto.WgInterface
import ru.unlimmitted.mtwgeasy.entity.TrafficRateEntity
import ru.unlimmitted.mtwgeasy.repository.TrafficRateRepository

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.mockito.ArgumentMatchers.anyLong
import static org.mockito.Mockito.*

class MikroTikFilesTests {

    @Test
    void calculatesTrafficDeltaAndClampsCounterReset() {
        TrafficRateRepository repository = mock(TrafficRateRepository)
        MikroTikService service = mock(MikroTikService)
        MikroTikFiles files = new MikroTikFiles(
                trafficRateRepository: repository,
                mikroTikService: service
        )
        when(repository.findAllSince(anyLong())).thenReturn([
                new TrafficRateEntity(tx: 1_048_576L, rx: 5_242_880L, time: 100L),
                new TrafficRateEntity(tx: 4_194_304L, rx: 3_145_728L, time: 130L)
        ])

        def rates = files.getTrafficByMinutes()

        assertEquals(1, rates.size())
        assertEquals(3L, rates.first().tx)
        assertEquals(0L, rates.first().rx)
        assertEquals(130L, rates.first().time)
    }

    @Test
    void skipsSavingWhenSettingsAreUnavailable() {
        TrafficRateRepository repository = mock(TrafficRateRepository)
        MikroTikService service = mock(MikroTikService)
        MikroTikFiles files = new MikroTikFiles(
                trafficRateRepository: repository,
                mikroTikService: service
        )
        when(service.getSettings()).thenReturn(null)

        files.saveInterfaceTraffic()

        verify(repository, never()).save(any(TrafficRateEntity))
    }

    @Test
    void savesCurrentCountersForConfiguredInputInterface() {
        TrafficRateRepository repository = mock(TrafficRateRepository)
        MikroTikService service = mock(MikroTikService)
        MikroTikFiles files = new MikroTikFiles(
                trafficRateRepository: repository,
                mikroTikService: service
        )
        when(service.getSettings()).thenReturn(new MikroTikSettings(inputWgInterfaceName: "wg-in"))
        when(service.getWgInterfaces()).thenReturn([
                new WgInterface(name: "wg-in", txByte: "2048", rxByte: "4096")
        ])

        files.saveInterfaceTraffic()

        verify(repository).save(argThat { it.tx == 2048L && it.rx == 4096L })
        verify(repository).deleteOlderThan(anyLong())
    }
}
