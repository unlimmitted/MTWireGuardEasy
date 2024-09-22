package ru.unlimmitted.mtwgeasy.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.unlimmitted.mtwgeasy.dto.MtSettings
import ru.unlimmitted.mtwgeasy.dto.Peer
import ru.unlimmitted.mtwgeasy.services.MikroTikExecutor
import ru.unlimmitted.mtwgeasy.services.MikroTikService

@RestController
@RequestMapping("/api/v1")
class ApiController {

	@Autowired
	MikroTikService mikroTikService

	@GetMapping("/get-wg-peers")
	ResponseEntity<Object> getWgPeers() {
		return ResponseEntity.ok().body(mikroTikService.getPeers())
	}

	@GetMapping("/get-mikrotik-info")
	ResponseEntity<Object> getMikroTikInfo() {
		return ResponseEntity.ok().body(mikroTikService.getMtInfo())
	}

	@GetMapping("/get-mikrotik-settings")
	ResponseEntity<Object> getMikroTikSettings() {
		mikroTikService.isSettings()
		if (mikroTikService.isConfigured) {
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

	@PostMapping("/change-routing-vpn")
	ResponseEntity<Object> changeRoutingVpn(@RequestBody Peer peer) {
		mikroTikService.changeRouting(peer)
		return ResponseEntity.ok().body(mikroTikService.getPeers())
	}

	@PostMapping("/remove-peer")
	ResponseEntity<Object> removePeer(@RequestBody Peer peer) {
		mikroTikService.removePeer(peer)
		return ResponseEntity.ok().body(mikroTikService.getPeers())
	}

	@PostMapping("/configurator")
	ResponseEntity<Object> startConfigurator(@RequestBody MtSettings settings) {
		mikroTikService.runConfigurator(settings)
		return ResponseEntity.ok().body(mikroTikService.getPeers())
	}

	@GetMapping("/get-traffic-by-minutes")
	ResponseEntity<Object> getTrafficByMinutes() {
		return ResponseEntity.ok().body(mikroTikService.getTrafficByMinutes())
	}
}
