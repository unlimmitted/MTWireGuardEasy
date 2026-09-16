package ru.unlimmitted.mtwgeasy.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "traffic_rate")
class TrafficRateEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var tx: Long = 0,

    @Column(nullable = false)
    var rx: Long = 0,

    @Column(nullable = false)
    var time: Long = 0,
)
