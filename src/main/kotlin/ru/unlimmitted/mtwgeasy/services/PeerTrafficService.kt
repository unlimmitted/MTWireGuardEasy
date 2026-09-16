package ru.unlimmitted.mtwgeasy.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.unlimmitted.mtwgeasy.dto.Peer
import ru.unlimmitted.mtwgeasy.dto.PeerTrafficHistory
import ru.unlimmitted.mtwgeasy.dto.PeerTrafficPoint
import ru.unlimmitted.mtwgeasy.entity.PeerTrafficSnapshotEntity
import ru.unlimmitted.mtwgeasy.repository.PeerTrafficSnapshotRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.TreeMap

@Service
class PeerTrafficService(
    private val repository: PeerTrafficSnapshotRepository,
) {
    @Transactional
    fun saveSnapshots(peers: List<Peer>?) {
        val now = Instant.now().epochSecond
        val snapshots = peers.orEmpty()
            .filter { !it.id.isNullOrBlank() && !it.name.isNullOrBlank() }
            .map { peer ->
                PeerTrafficSnapshotEntity(
                    peerId = requireNotNull(peer.id).take(128),
                    peerName = requireNotNull(peer.name).take(128),
                    rx = counterValue(peer.rx),
                    tx = counterValue(peer.tx),
                    time = now,
                )
            }
        if (snapshots.isNotEmpty()) repository.saveAll(snapshots)
        repository.deleteOlderThan(Instant.now().minus(RETENTION_HOURS, ChronoUnit.HOURS).epochSecond)
    }

    fun getHistory(peerId: String?): PeerTrafficHistory {
        val normalizedPeerId = peerId?.trim()
        if (normalizedPeerId.isNullOrEmpty() || normalizedPeerId.length > 128) {
            throw IllegalArgumentException("Peer id is required")
        }

        val since = Instant.now().minus(HISTORY_HOURS, ChronoUnit.HOURS).epochSecond
        val records = buildList {
            repository.findFirstByPeerIdAndTimeLessThanOrderByTimeDesc(normalizedPeerId, since)?.let(::add)
            addAll(repository.findByPeerIdAndTimeGreaterThanEqualOrderByTimeAsc(normalizedPeerId, since))
        }
        val result = PeerTrafficHistory(
            peerId = normalizedPeerId,
            peerName = records.lastOrNull()?.peerName,
        )
        if (records.size < 2) return result

        val buckets = TreeMap<Long, PeerTrafficPoint>()
        for (index in 1 until records.size) {
            val previous = records[index - 1]
            val current = records[index]
            if (current.time < since || current.time <= previous.time) continue

            val rxDelta = counterDelta(previous.rx, current.rx)
            val txDelta = counterDelta(previous.tx, current.tx)
            val bucketTime = current.time / BUCKET_SECONDS * BUCKET_SECONDS
            val point = buckets.getOrPut(bucketTime) { PeerTrafficPoint(time = bucketTime) }
            point.rx += rxDelta
            point.tx += txDelta
            result.totalRx += rxDelta
            result.totalTx += txDelta
        }
        result.points = buckets.values.toList()
        return result
    }

    companion object {
        const val HISTORY_HOURS = 24L
        const val RETENTION_HOURS = 25L
        const val BUCKET_SECONDS = 300L

        @JvmStatic
        fun counterValue(value: String?): Long = maxOf(0, value?.toLongOrNull() ?: 0)

        @JvmStatic
        fun counterDelta(previous: Long?, current: Long?): Long {
            val before = previous ?: 0
            val now = current ?: 0
            return if (now >= before) now - before else maxOf(0, now)
        }
    }
}
