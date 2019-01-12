package com.ank.drift

import com.ank.drift.client.config.ClientWebConfiguration
import com.ank.drift.server.config.ServerWebConfiguration
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