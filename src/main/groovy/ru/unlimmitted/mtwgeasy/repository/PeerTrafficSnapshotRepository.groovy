package ru.unlimmitted.mtwgeasy.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import ru.unlimmitted.mtwgeasy.entity.PeerTrafficSnapshotEntity

@Repository
interface PeerTrafficSnapshotRepository extends JpaRepository<PeerTrafficSnapshotEntity, Long> {

    List<PeerTrafficSnapshotEntity> findByPeerIdAndTimeGreaterThanEqualOrderByTimeAsc(
            String peerId,
            Long time
    )

    PeerTrafficSnapshotEntity findFirstByPeerIdAndTimeLessThanOrderByTimeDesc(
            String peerId,
            Long time
    )

    @Modifying
    @Transactional
    @Query("DELETE FROM PeerTrafficSnapshotEntity p WHERE p.time < :before")
    void deleteOlderThan(@Param("before") Long beforeEpochSecond)
}
