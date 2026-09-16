package ru.unlimmitted.mtwgeasy.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class MvcConfig : WebMvcConfigurer {
    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addViewController("/").setViewName("main")
        registry.addViewController("/login").setViewName("main")
        registry.addViewController("/settings").setViewName("main")
    }
}
