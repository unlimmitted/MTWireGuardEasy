package ru.unlimmitted.mtwgeasy.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.unlimmitted.mtwgeasy.dto.MikroTikSettings
import ru.unlimmitted.mtwgeasy.dto.WgInterface
import ru.unlimmitted.mtwgeasy.entity.TrafficRateEntity
import ru.unlimmitted.mtwgeasy.repository.TrafficRateRepository

class MikroTikFilesTests {
    @Test
    fun calculatesTrafficDeltaAndClampsCounterReset() {
        val repository = mock(TrafficRateRepository::class.java)
        val service = mock(MikroTikService::class.java)
        val files = MikroTikFiles(repository, service)
        `when`(repository.findAllSince(anyLong())).thenReturn(
            listOf(
                TrafficRateEntity(tx = 1_048_576, rx = 5_242_880, time = 100),
                TrafficRateEntity(tx = 4_194_304, rx = 3_145_728, time = 130),
            ),
        )

        val rates = files.getTrafficByMinutes()

        assertEquals(1, rates.size)
        assertEquals(3, rates.first().tx)
        assertEquals(0, rates.first().rx)
        assertEquals(130, rates.first().time)
    }

    @Test
    fun skipsSavingWhenSettingsAreUnavailable() {
        val repository = mock(TrafficRateRepository::class.java)
        val service = mock(MikroTikService::class.java)
        val files = MikroTikFiles(repository, service)
        `when`(service.settings).thenReturn(null)

        files.saveInterfaceTraffic()

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any(TrafficRateEntity::class.java))
    }

    @Test
    fun savesCurrentCountersForConfiguredInputInterface() {
        val repository = mock(TrafficRateRepository::class.java)
        val service = mock(MikroTikService::class.java)
        val files = MikroTikFiles(repository, service)
        `when`(service.settings).thenReturn(MikroTikSettings(inputWgInterfaceName = "wg-in"))
        `when`(service.wgInterfaces).thenReturn(
            listOf(WgInterface(name = "wg-in", txByte = "2048", rxByte = "4096")),
        )

        files.saveInterfaceTraffic()

        verify(repository).save(argThat { it.tx == 2048L && it.rx == 4096L })
        verify(repository).deleteOlderThan(anyLong())
    }
}
