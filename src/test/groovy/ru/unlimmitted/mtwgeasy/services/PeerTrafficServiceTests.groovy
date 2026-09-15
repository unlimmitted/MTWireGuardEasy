package ru.unlimmitted.mtwgeasy.services

import org.junit.jupiter.api.Test
import ru.unlimmitted.mtwgeasy.dto.Peer
import ru.unlimmitted.mtwgeasy.entity.PeerTrafficSnapshotEntity
import ru.unlimmitted.mtwgeasy.repository.PeerTrafficSnapshotRepository

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.mockito.ArgumentMatchers.anyLong
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.*

class PeerTrafficServiceTests {

    @Test
    void savesCurrentPeerCountersAndCleansExpiredSnapshots() {
        PeerTrafficSnapshotRepository repository = mock(PeerTrafficSnapshotRepository)
        PeerTrafficService service = new PeerTrafficService(repository: repository)

        service.saveSnapshots([
                new Peer(id: "*1", name: "phone", rx: "1048576", tx: "2097152"),
                new Peer(id: "*2", name: "offline", rx: null, tx: "invalid")
        ])

        verify(repository).saveAll(argThat { snapshots ->
            snapshots.size() == 2 &&
                    snapshots[0].peerId == "*1" &&
                    snapshots[0].rx == 1_048_576L &&
                    snapshots[0].tx == 2_097_152L &&
                    snapshots[1].rx == 0L &&
                    snapshots[1].tx == 0L
        })
        verify(repository).deleteOlderThan(anyLong())
    }

    @Test
    void calculatesFiveMinuteBucketsTotalsAndCounterReset() {
        PeerTrafficSnapshotRepository repository = mock(PeerTrafficSnapshotRepository)
        PeerTrafficService service = new PeerTrafficService(repository: repository)
        long now = System.currentTimeMillis().intdiv(1000)
        long since = now - 3600

        when(repository.findFirstByPeerIdAndTimeLessThanOrderByTimeDesc(anyString(), anyLong()))
                .thenReturn(new PeerTrafficSnapshotEntity(
                        peerId: "*1", peerName: "phone", rx: 100L, tx: 200L, time: since - 10
                ))
        when(repository.findByPeerIdAndTimeGreaterThanEqualOrderByTimeAsc(anyString(), anyLong()))
                .thenReturn([
                        new PeerTrafficSnapshotEntity(
                                peerId: "*1", peerName: "phone", rx: 160L, tx: 260L, time: since + 10
                        ),
                        new PeerTrafficSnapshotEntity(
                                peerId: "*1", peerName: "phone", rx: 20L, tx: 30L, time: since + 20
                        ),
                        new PeerTrafficSnapshotEntity(
                                peerId: "*1", peerName: "phone", rx: 50L, tx: 90L, time: since + 310
                        )
                ])

        def history = service.getHistory("*1")

        assertEquals("phone", history.peerName)
        assertEquals(110L, history.totalRx)
        assertEquals(150L, history.totalTx)
        assertEquals(2, history.points.size())
        assertEquals(110L, history.points.sum { it.rx } as Long)
        assertEquals(150L, history.points.sum { it.tx } as Long)
    }

    @Test
    void rejectsMissingPeerId() {
        PeerTrafficService service = new PeerTrafficService(repository: mock(PeerTrafficSnapshotRepository))
        assertThrows(IllegalArgumentException) { service.getHistory(" ") }
    }
}
