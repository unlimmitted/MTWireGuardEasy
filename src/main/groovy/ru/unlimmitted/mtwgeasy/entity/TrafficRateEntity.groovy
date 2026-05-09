package ru.unlimmitted.mtwgeasy.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "traffic_rate")
class TrafficRateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    @Column(nullable = false)
    Long tx

    @Column(nullable = false)
    Long rx

    // Unix epoch seconds — тот же формат что был в JSON
    @Column(nullable = false)
    Long time
}
