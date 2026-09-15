package ru.unlimmitted.mtwgeasy.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.unlimmitted.mtwgeasy.dto.Peer
import ru.unlimmitted.mtwgeasy.dto.PeerTrafficHistory
import ru.unlimmitted.mtwgeasy.dto.PeerTrafficPoint
import ru.unlimmitted.mtwgeasy.entity.PeerTrafficSnapshotEntity
import ru.unlimmitted.mtwgeasy.repository.PeerTrafficSnapshotRepository

import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class PeerTrafficService {

    static final long HISTORY_HOURS = 24L
    static final long RETENTION_HOURS = 25L
    static final long BUCKET_SECONDS = 300L

    @Autowired
    PeerTrafficSnapshotRepository repository

    @Transactional
    void saveSnapshots(List<Peer> peers) {
        long now = Instant.now().epochSecond
        List<PeerTrafficSnapshotEntity> snapshots = (peers ?: [])
                .findAll { it?.id && it.name }
                .collect { Peer peer ->
                    new PeerTrafficSnapshotEntity(
                            peerId: peer.id.take(128),
                            peerName: peer.name.take(128),
                            rx: counterValue(peer.rx),
                            tx: counterValue(peer.tx),
                            time: now
                    )
                }

        if (!snapshots.isEmpty()) {
            repository.saveAll(snapshots)
        }
        repository.deleteOlderThan(Instant.now().minus(RETENTION_HOURS, ChronoUnit.HOURS).epochSecond)
    }

    PeerTrafficHistory getHistory(String peerId) {
        String normalizedPeerId = peerId?.trim()
        if (!normalizedPeerId || normalizedPeerId.length() > 128) {
            throw new IllegalArgumentException("Peer id is required")
        }

        long since = Instant.now().minus(HISTORY_HOURS, ChronoUnit.HOURS).epochSecond
        List<PeerTrafficSnapshotEntity> records = []
        PeerTrafficSnapshotEntity baseline = repository
                .findFirstByPeerIdAndTimeLessThanOrderByTimeDesc(normalizedPeerId, since)
        if (baseline != null) {
            records.add(baseline)
        }
        records.addAll(repository.findByPeerIdAndTimeGreaterThanEqualOrderByTimeAsc(normalizedPeerId, since))

        PeerTrafficHistory result = new PeerTrafficHistory(peerId: normalizedPeerId)
        if (!records.isEmpty()) {
            result.peerName = records.last().peerName
        }
        if (records.size() < 2) {
            return result
        }

        Map<Long, PeerTrafficPoint> buckets = new TreeMap<>()
        for (int index = 1; index < records.size(); index++) {
            PeerTrafficSnapshotEntity previous = records[index - 1]
            PeerTrafficSnapshotEntity current = records[index]
            if (current.time < since || current.time <= previous.time) continue

            long rxDelta = counterDelta(previous.rx, current.rx)
            long txDelta = counterDelta(previous.tx, current.tx)
            long bucketTime = (current.time / BUCKET_SECONDS as long) * BUCKET_SECONDS
            PeerTrafficPoint point = buckets.computeIfAbsent(bucketTime) {
                new PeerTrafficPoint(time: bucketTime, rx: 0L, tx: 0L)
            }
            point.rx += rxDelta
            point.tx += txDelta
            result.totalRx += rxDelta
            result.totalTx += txDelta
        }
        result.points = buckets.values() as List<PeerTrafficPoint>
        return result
    }

    static long counterValue(String value) {
        try {
            return Math.max(0L, value?.toLong() ?: 0L)
        } catch (NumberFormatException ignored) {
            return 0L
        }
    }

    static long counterDelta(Long previous, Long current) {
        long before = previous ?: 0L
        long now = current ?: 0L
        return now >= before ? now - before : Math.max(0L, now)
    }
}
