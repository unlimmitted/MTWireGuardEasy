package ru.unlimmitted.mtwgeasy.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "peer_traffic_snapshot",
    indexes = [Index(name = "idx_peer_traffic_peer_time", columnList = "peer_id,time")],
)
class PeerTrafficSnapshotEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "peer_id", nullable = false, length = 128)
    var peerId: String = "",

    @Column(name = "peer_name", nullable = false, length = 128)
    var peerName: String = "",

    @Column(nullable = false)
    var tx: Long = 0,

    @Column(nullable = false)
    var rx: Long = 0,

    @Column(nullable = false)
    var time: Long = 0,
)
