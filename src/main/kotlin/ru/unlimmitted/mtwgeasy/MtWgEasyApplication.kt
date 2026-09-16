package ru.unlimmitted.mtwgeasy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class MtWgEasyApplication

fun main(args: Array<String>) {
    runApplication<MtWgEasyApplication>(*args)
}
