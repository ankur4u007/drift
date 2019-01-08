package com.ank.websockethttptunnel

import com.ank.websockethttptunnel.client.config.ClientWebConfiguration
import com.ank.websockethttptunnel.server.config.ServerWebConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@EnableAutoConfiguration
@Import(value = arrayOf(ServerWebConfiguration::class, ClientWebConfiguration::class))
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}