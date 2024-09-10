package ru.unlimmitted.mtwgeasy.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping

@Controller
@CrossOrigin
class WebController {
	@GetMapping("")
	String rootMapping() {
		return "main"
	}
}
