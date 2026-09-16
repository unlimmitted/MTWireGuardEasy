package ru.unlimmitted.mtwgeasy.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import ru.unlimmitted.mtwgeasy.entity.TrafficRateEntity

@Repository
interface TrafficRateRepository : JpaRepository<TrafficRateEntity, Long> {
    @Query("SELECT t FROM TrafficRateEntity t WHERE t.time >= :since ORDER BY t.time ASC")
    fun findAllSince(@Param("since") sinceEpochSecond: Long): List<TrafficRateEntity>

    @Modifying
    @Transactional
    @Query("DELETE FROM TrafficRateEntity t WHERE t.time < :before")
    fun deleteOlderThan(@Param("before") beforeEpochSecond: Long)
}
