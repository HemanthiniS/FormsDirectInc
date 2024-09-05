package com.formsdirectinc

import com.formsdirectinc.environment.Environment
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.hibernate.SessionFactory
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import java.util.concurrent.TimeUnit

@Configuration
open class SessionFactoryConfig @Autowired constructor(@Qualifier("customEnvironment") private val environment: Environment) {
    private val sessionFactoryRegistry = mutableMapOf<String, SessionFactory>()

    @Bean("sessionFactory")
    @Scope("singleton")
    open fun sessionFactory() : com.formsdirectinc.SessionFactory {
        loadSessionFactory()
        return SessionFactory(sessionFactoryRegistry)
    }

    private fun loadSessionFactory()  {
        environment.activeSites!!.forEach {
            sessionFactoryRegistry.put(it.toLowerCase(), getSessionFactory(
                environment.getProperty("${it.toLowerCase()}.datasource.host"),
                environment.getProperty("${it.toLowerCase()}.datasource.port"),
                environment.getProperty("${it.toLowerCase()}.datasource.name"),
                environment.getProperty("${it.toLowerCase()}.datasource.user"),
                environment.getProperty("${it.toLowerCase()}.datasource.password"),
                environment.getIntegerPropertyOrDefault("${it.toLowerCase()}.datasource.maximum.poolsize", 100),
                environment.getIntegerPropertyOrDefault("${it.toLowerCase()}.datasource.minimum.idle", 5),
                environment.getProperty("${it.toLowerCase()}.datasource.connection.timeout", "30000").toLong(),
                environment.getProperty("${it.toLowerCase()}.datasource.idle.timeout", TimeUnit.MINUTES.toMillis(10).toString()).toLong()
            ))
        }
    }

    private fun getSessionFactory(
        host: String,
        port: String,
        name: String,
        userName: String,
        password: String,
        maxPoolSize: Int,
        minimumIdle: Int,
        connectionTimeout: Long,
        idleTimeout: Long
    ): SessionFactory {
        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = "jdbc:mysql://$host:$port/$name"
        hikariConfig.username = userName
        hikariConfig.password = password
        hikariConfig.maximumPoolSize = maxPoolSize
        hikariConfig.minimumIdle = minimumIdle
        hikariConfig.connectionTimeout = connectionTimeout
        hikariConfig.idleTimeout = idleTimeout
        val dataSource = HikariDataSource(hikariConfig)
        val registry = StandardServiceRegistryBuilder()
            .applySetting(org.hibernate.cfg.Environment.DATASOURCE, dataSource).configure()
            .build()
        val metadataSources = MetadataSources(registry)
        return metadataSources.buildMetadata().buildSessionFactory()
    }
}