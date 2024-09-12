package com.formsdirectinc.spring.config

import com.formsdirectinc.security.registry.RoutingCryptoRegistry
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.web.servlet.ViewResolver
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.view.InternalResourceViewResolver
import java.security.Security
import java.util.*


@SpringBootApplication
@EnableWebMvc
@ComponentScan(basePackages = arrayOf("com.formsdirectinc"))
open class Application: SpringBootServletInitializer(), ApplicationListener<ContextRefreshedEvent> {
    private var pbeInitialized = false

	override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
		return application.sources(Application::class.java)
	}

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        if (!pbeInitialized) {
            val cryptoRegistry = event.applicationContext.getBean("cryptoRegistry") as RoutingCryptoRegistry
            if (event.applicationContext.environment.getProperty("serverMode", "test") != "test") {
                event.applicationContext.environment.getProperty("active.sites").split(",").forEach {
                    val password = System.console().readPassword("Please enter the PBE password for Site ${it.toUpperCase()}: ")
                    cryptoRegistry.registerEncryptor(it, password)
                    Arrays.fill(password, '0')
                }
            }
        }
        pbeInitialized = true
    }

    @Bean
    open fun getViewResolver(): ViewResolver {
        val resolver = InternalResourceViewResolver()
        resolver.setPrefix("/")
        resolver.setSuffix(".jsp")
        return resolver
    }
}

fun main(args: Array<String>) {
    Security.addProvider(BouncyCastleProvider())
    SpringApplication.run(Application::class.java, *args)
}
