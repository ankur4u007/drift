package com.ank.drift.server.service

import com.ank.drift.common.model.Event
import com.ank.drift.common.model.Gossip
import com.ank.drift.server.config.ServerConfig
import com.ank.drift.server.exception.ForbiddenException
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
            Flux.just(Gossip(status = HttpResponseStatus.OK.code(), message = "Client:$id registered successFully", event = Event.CLIENT_REGISTRATION_SUCCESS))
        } else {
            log.error("${AuthService::authenticate.name}, Invalid $key, sessionId=$id")
            Flux.error(ForbiddenException(Gossip(status = HttpResponseStatus.FORBIDDEN.code(), message = "Client registration failed", event = Event.CLIENT_REGISTRATION_FAILED)))
        }
    }
}