package ru.unlimmitted.mtwgeasy.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import ru.unlimmitted.mtwgeasy.entity.TrafficRateEntity

@Repository
interface TrafficRateRepository extends JpaRepository<TrafficRateEntity, Long> {

    @Query("SELECT t FROM TrafficRateEntity t WHERE t.time >= :since ORDER BY t.time ASC")
    List<TrafficRateEntity> findAllSince(@Param("since") Long sinceEpochSecond)

    @Modifying
    @Transactional
    @Query("DELETE FROM TrafficRateEntity t WHERE t.time < :before")
    void deleteOlderThan(@Param("before") Long beforeEpochSecond)
}
