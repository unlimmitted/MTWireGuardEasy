package ru.unlimmitted.mtwgeasy.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.unlimmitted.mtwgeasy.dto.MikroTikSettings
import ru.unlimmitted.mtwgeasy.dto.NewWireguardInterface
import ru.unlimmitted.mtwgeasy.dto.Peer
import ru.unlimmitted.mtwgeasy.dto.WgInterface
import ru.unlimmitted.mtwgeasy.services.MikroTikFiles
import ru.unlimmitted.mtwgeasy.services.MikroTikService

@RestController
@RequestMapping("/api/v1")
class ApiController {

	@Autowired
	MikroTikService mikroTikService

	@Autowired
	MikroTikFiles mikroTikFiles

	@GetMapping("/get-wg-peers")
	ResponseEntity<Object> getWgPeers() {
		return ResponseEntity.ok().body(mikroTikService.getPeers())
	}

	@GetMapping("/get-mikrotik-info")
	ResponseEntity<Object> getMikroTikInfo() {
		mikroTikService.setWgInterfaces()
		return ResponseEntity.ok().body(mikroTikService.getMikroTikInfo())
	}

	@GetMapping("/get-mikrotik-settings")
	ResponseEntity<Object> getMikroTikSettings() {
		mikroTikService.setSettings()
		if (mikroTikService.isSettings()) {
			return ResponseEntity.ok().body(mikroTikService.settings)
		} else {
			return ResponseEntity.ok().body(false)
		}
	}

	@PostMapping("/create-new-peer")
	ResponseEntity<Object> createNewPeer(@RequestBody String peerName) {
		mikroTikService.createNewPeer(peerName.replace("=", ""))
		return ResponseEntity.ok().body(mikroTikService.getPeers())
	}

	@PostMapping("/change-routing-peer")
	ResponseEntity<Object> changeRoutingPeer(@RequestBody Peer peer) {
		mikroTikService.changeRouting(peer)
		return ResponseEntity.ok().body(mikroTikService.getPeers())
	}

	@PostMapping("/remove-peer")
	ResponseEntity<Object> removePeer(@RequestBody Peer peer) {
		mikroTikService.removePeer(peer)
		return ResponseEntity.ok().body(mikroTikService.getPeers())
	}

	@PostMapping("/configurator")
	ResponseEntity<Object> startConfigurator(@RequestBody MikroTikSettings settings) {
		mikroTikService.runConfigurator(settings)
		return ResponseEntity.ok().body(mikroTikService.settings)
	}

	@PostMapping("/change-routing-vpn")
	ResponseEntity<Object> changeRoutingVpn(@RequestBody WgInterface wgInterface) {
		mikroTikService.changeVpnRouting(wgInterface)
		mikroTikService.setWgInterfaces()
		return ResponseEntity.ok().body(mikroTikService.getMikroTikInfo())
	}

	@GetMapping("/get-traffic-by-minutes")
	ResponseEntity<Object> getTrafficByMinutes() {
		return ResponseEntity.ok().body(mikroTikFiles.getTrafficByMinutes())
	}

	@GetMapping("/get-ether-interfaces")
	ResponseEntity<Object> getEtherInterfaces() {
		return ResponseEntity.ok().body(mikroTikService.getEtherInterfaces())
	}

	@PostMapping("/set-interface-status")
	ResponseEntity<Object> setInterfaceStatus(@RequestBody WgInterface wgInterface) {
		mikroTikService.setInterfaceStatus(wgInterface)
		mikroTikService.setWgInterfaces()
		return ResponseEntity.ok().body(mikroTikService.getMikroTikInfo())
	}

	@PostMapping("/delete-external-interface")
	ResponseEntity<Object> deleteExternalInterface(@RequestBody WgInterface wgInterface) {
		mikroTikService.deleteExternalInterface(wgInterface)
		mikroTikService.setWgInterfaces()
		return ResponseEntity.ok().body(mikroTikService.getMikroTikInfo())
	}

	@PostMapping("/create-new-interface")
	ResponseEntity<Object> createNewWgInterface(@RequestBody NewWireguardInterface wgInterface) {
		mikroTikService.createNewWgInterface(wgInterface)
		mikroTikService.setWgInterfaces()
		return ResponseEntity.ok().body(mikroTikService.getMikroTikInfo())
	}
}
