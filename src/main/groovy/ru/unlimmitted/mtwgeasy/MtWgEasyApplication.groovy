package ru.unlimmitted.mtwgeasy

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class MtWgEasyApplication {
	static void main(String[] args) {
		SpringApplication.run(MtWgEasyApplication, args)
	}
}
