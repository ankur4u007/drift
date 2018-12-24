package com.ank.websockethttptunnel.client.service

import com.ank.websockethttptunnel.common.model.Event
import com.ank.websockethttptunnel.common.model.Gossip
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import javax.inject.Inject

@Service
class ClientEventHandlerService @Inject constructor(val clientCacheService: ClientCacheService) {
    companion object {
        val log = LoggerFactory.getLogger(ClientEventHandlerService::class.java)
    }

    fun handle(gossip: Gossip): Mono<Gossip>{
        log.info("${ClientEventHandlerService::handle.name}, gossip=$gossip")
        return when(gossip.event) {
            Event.SERVER_PONG -> {
                clientCacheService.decrementPong()
                Mono.just(gossip)
            }
            else -> Mono.just(gossip)
        }
    }
}