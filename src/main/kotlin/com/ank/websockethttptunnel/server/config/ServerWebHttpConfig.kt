package com.ank.websockethttptunnel.server.config

import com.ank.websockethttptunnel.server.transport.http.ServerWebHttpHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import javax.inject.Inject

@Configuration
class ServerWebHttpConfig @Inject constructor(
    val serverWebHttpHandler: ServerWebHttpHandler
) {

    @Bean
    fun registerHttpMapping(): RouterFunction<ServerResponse> {
        return serverWebHttpHandler.handle()
    }
}