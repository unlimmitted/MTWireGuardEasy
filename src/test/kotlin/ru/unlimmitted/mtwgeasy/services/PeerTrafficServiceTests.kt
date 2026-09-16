package ru.unlimmitted.mtwgeasy.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.unlimmitted.mtwgeasy.dto.Peer
import ru.unlimmitted.mtwgeasy.entity.PeerTrafficSnapshotEntity
import ru.unlimmitted.mtwgeasy.repository.PeerTrafficSnapshotRepository
import java.time.Instant

class PeerTrafficServiceTests {
    @Test
    fun savesCurrentPeerCountersAndCleansExpiredSnapshots() {
        val repository = mock(PeerTrafficSnapshotRepository::class.java)
        val service = PeerTrafficService(repository)

        service.saveSnapshots(
            listOf(
                Peer(id = "*1", name = "phone", rx = "1048576", tx = "2097152"),
                Peer(id = "*2", name = "offline", rx = null, tx = "invalid"),
            ),
        )

        verify(repository).saveAll(
            argThat<List<PeerTrafficSnapshotEntity>> { snapshots ->
                snapshots.size == 2 &&
                    snapshots[0].peerId == "*1" &&
                    snapshots[0].rx == 1_048_576L &&
                    snapshots[0].tx == 2_097_152L &&
                    snapshots[1].rx == 0L &&
                    snapshots[1].tx == 0L
            },
        )
        verify(repository).deleteOlderThan(anyLong())
    }

    @Test
    fun calculatesFiveMinuteBucketsTotalsAndCounterReset() {
        val repository = mock(PeerTrafficSnapshotRepository::class.java)
        val service = PeerTrafficService(repository)
        val now = Instant.now().epochSecond
        val since = now - 3600

        `when`(repository.findFirstByPeerIdAndTimeLessThanOrderByTimeDesc(anyString(), anyLong())).thenReturn(
            PeerTrafficSnapshotEntity(peerId = "*1", peerName = "phone", rx = 100, tx = 200, time = since - 10),
        )
        `when`(repository.findByPeerIdAndTimeGreaterThanEqualOrderByTimeAsc(anyString(), anyLong())).thenReturn(
            listOf(
                PeerTrafficSnapshotEntity(peerId = "*1", peerName = "phone", rx = 160, tx = 260, time = since + 10),
                PeerTrafficSnapshotEntity(peerId = "*1", peerName = "phone", rx = 20, tx = 30, time = since + 20),
                PeerTrafficSnapshotEntity(peerId = "*1", peerName = "phone", rx = 50, tx = 90, time = since + 310),
            ),
        )

        val history = service.getHistory("*1")

        assertEquals("phone", history.peerName)
        assertEquals(110, history.totalRx)
        assertEquals(150, history.totalTx)
        assertEquals(2, history.points.size)
        assertEquals(110, history.points.sumOf { it.rx })
        assertEquals(150, history.points.sumOf { it.tx })
    }

    @Test
    fun rejectsMissingPeerId() {
        val service = PeerTrafficService(mock(PeerTrafficSnapshotRepository::class.java))
        assertThrows(IllegalArgumentException::class.java) { service.getHistory(" ") }
    }
}
