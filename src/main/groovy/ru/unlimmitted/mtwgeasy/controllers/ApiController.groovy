package ru.unlimmitted.mtwgeasy.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.unlimmitted.mtwgeasy.services.MikroTikService

@RestController
@RequestMapping("/api/v1")
@CrossOrigin
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

}
