package ru.unlimmitted.mtwgeasy.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.unlimmitted.mtwgeasy.dto.DoubleVpnInversionRequest
import ru.unlimmitted.mtwgeasy.dto.MikroTikSettings
import ru.unlimmitted.mtwgeasy.dto.NewWireguardInterface
import ru.unlimmitted.mtwgeasy.dto.Peer
import ru.unlimmitted.mtwgeasy.dto.RenamePeerRequest
import ru.unlimmitted.mtwgeasy.dto.WgInterface
import ru.unlimmitted.mtwgeasy.services.MikroTikFiles
import ru.unlimmitted.mtwgeasy.services.MikroTikService
import ru.unlimmitted.mtwgeasy.services.PeerTrafficService

@RestController
@RequestMapping("/api/v1")
class ApiController(
    private val mikroTikService: MikroTikService,
    private val mikroTikFiles: MikroTikFiles,
    private val peerTrafficService: PeerTrafficService,
) {
    @GetMapping("/get-wg-peers")
    fun getWgPeers(): ResponseEntity<Any> = ResponseEntity.ok(mikroTikService.getPeers())

    @GetMapping("/get-mikrotik-info")
    fun getMikroTikInfo(): ResponseEntity<Any> = ResponseEntity.ok(mikroTikService.getMikroTikInfo())

    @GetMapping("/get-mikrotik-settings")
    fun getMikroTikSettings(): ResponseEntity<Any> =
        ResponseEntity.ok(if (mikroTikService.isSettings()) mikroTikService.settings else false)

    @PostMapping("/create-new-peer")
    fun createNewPeer(@RequestBody peerName: String): ResponseEntity<Any> {
        mikroTikService.createNewPeer(peerName.replace("=", ""))
        return ResponseEntity.ok(mikroTikService.getPeers())
    }

    @PostMapping("/rename-peer")
    fun renamePeer(@RequestBody request: RenamePeerRequest): ResponseEntity<Any> {
        mikroTikService.renamePeer(request)
        return ResponseEntity.ok(mikroTikService.getPeers())
    }

    @PostMapping("/change-routing-peer")
    fun changeRoutingPeer(@RequestBody peer: Peer): ResponseEntity<Any> {
        mikroTikService.changeRouting(peer)
        return ResponseEntity.ok(mikroTikService.getPeers())
    }

    @PostMapping("/set-double-vpn-inversion")
    fun setDoubleVpnInversion(@RequestBody request: DoubleVpnInversionRequest): ResponseEntity<Any> =
        ResponseEntity.ok(mikroTikService.setDoubleVpnInversion(request.inverted))

    @PostMapping("/remove-peer")
    fun removePeer(@RequestBody peer: Peer): ResponseEntity<Any> {
        mikroTikService.removePeer(peer)
        return ResponseEntity.ok(mikroTikService.getPeers())
    }

    @PostMapping("/configurator")
    fun startConfigurator(@RequestBody settings: MikroTikSettings): ResponseEntity<Any> {
        mikroTikService.runConfigurator(settings)
        return ResponseEntity.ok(mikroTikService.settings)
    }

    @PostMapping("/change-routing-vpn")
    fun changeRoutingVpn(@RequestBody wgInterface: WgInterface): ResponseEntity<Any> {
        mikroTikService.changeVpnRouting(wgInterface)
        mikroTikService.setWgInterfaces()
        return ResponseEntity.ok(mikroTikService.getMikroTikInfo())
    }

    @GetMapping("/get-traffic-by-minutes")
    fun getTrafficByMinutes(): ResponseEntity<Any> = ResponseEntity.ok(mikroTikFiles.getTrafficByMinutes())

    @GetMapping("/peer-traffic")
    fun getPeerTraffic(@RequestParam("peerId") peerId: String): ResponseEntity<Any> =
        ResponseEntity.ok(peerTrafficService.getHistory(peerId))

    @GetMapping("/get-ether-interfaces")
    fun getEtherInterfaces(): ResponseEntity<Any> = ResponseEntity.ok(mikroTikService.getEtherInterfaces())

    @PostMapping("/set-interface-status")
    fun setInterfaceStatus(@RequestBody wgInterface: WgInterface): ResponseEntity<Any> {
        mikroTikService.setInterfaceStatus(wgInterface)
        mikroTikService.setWgInterfaces()
        return ResponseEntity.ok(mikroTikService.getMikroTikInfo())
    }

    @PostMapping("/delete-external-interface")
    fun deleteExternalInterface(@RequestBody wgInterface: WgInterface): ResponseEntity<Any> {
        mikroTikService.deleteExternalInterface(wgInterface)
        mikroTikService.setWgInterfaces()
        return ResponseEntity.ok(mikroTikService.getMikroTikInfo())
    }

    @PostMapping("/create-new-interface")
    fun createNewWgInterface(@RequestBody wgInterface: NewWireguardInterface): ResponseEntity<Any> {
        mikroTikService.createNewWgInterface(wgInterface)
        mikroTikService.setWgInterfaces()
        return ResponseEntity.ok(mikroTikService.getMikroTikInfo())
    }
}
