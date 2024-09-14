package ru.unlimmitted.mtwgeasy.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class MvcConfig implements WebMvcConfigurer {

	@Override
	void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/").setViewName("main")
		registry.addViewController("/login").setViewName("main");
	}
}
