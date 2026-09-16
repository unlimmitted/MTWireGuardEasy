package ru.unlimmitted.mtwgeasy.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import ru.unlimmitted.mtwgeasy.entity.PeerTrafficSnapshotEntity

@Repository
interface PeerTrafficSnapshotRepository : JpaRepository<PeerTrafficSnapshotEntity, Long> {
    fun findByPeerIdAndTimeGreaterThanEqualOrderByTimeAsc(
        peerId: String,
        time: Long,
    ): List<PeerTrafficSnapshotEntity>

    fun findFirstByPeerIdAndTimeLessThanOrderByTimeDesc(
        peerId: String,
        time: Long,
    ): PeerTrafficSnapshotEntity?

    @Modifying
    @Transactional
    @Query("DELETE FROM PeerTrafficSnapshotEntity p WHERE p.time < :before")
    fun deleteOlderThan(@Param("before") beforeEpochSecond: Long)
}
