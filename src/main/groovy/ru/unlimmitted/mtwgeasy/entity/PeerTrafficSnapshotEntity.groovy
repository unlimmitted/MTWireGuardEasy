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
        indexes = [@Index(name = "idx_peer_traffic_peer_time", columnList = "peer_id,time")]
)
class PeerTrafficSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    @Column(name = "peer_id", nullable = false, length = 128)
    String peerId

    @Column(name = "peer_name", nullable = false, length = 128)
    String peerName

    @Column(nullable = false)
    Long tx

    @Column(nullable = false)
    Long rx

    @Column(nullable = false)
    Long time
}
