package com.ank.websockethttptunnel.server.service

import com.ank.websockethttptunnel.common.model.Gossip
import com.ank.websockethttptunnel.server.Exception.ForbiddenException
import com.ank.websockethttptunnel.server.config.ServerConfig
import io.netty.handler.codec.http.HttpResponseStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import javax.inject.Inject

@Service
class AuthService @Inject constructor(val serverConfig: ServerConfig) {

    companion object {
        val log = LoggerFactory.getLogger(AuthService::class.java)
    }

    fun authenticate(key: String, id: String): Flux<Gossip> {
        return if (key == "key=${serverConfig.key}") {
            Flux.just(Gossip(status = HttpResponseStatus.OK.code(), message = "Client:$id registered successFully"))
        } else {
            log.error("${AuthService::authenticate.name}, Invalid $key")
            Flux.error(ForbiddenException(Gossip(status = HttpResponseStatus.FORBIDDEN.code(), message = "Client registration failed")))
        }
    }
}