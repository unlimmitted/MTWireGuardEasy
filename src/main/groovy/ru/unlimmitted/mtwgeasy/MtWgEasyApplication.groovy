package ru.unlimmitted.mtwgeasy


import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
//@EnableJpaRepositories(basePackages = "ru.unlimmitted.mtwgeasy.repository")
class MtWgEasyApplication {
    static void main(String[] args) {
        SpringApplication.run(MtWgEasyApplication, args)
    }
}
