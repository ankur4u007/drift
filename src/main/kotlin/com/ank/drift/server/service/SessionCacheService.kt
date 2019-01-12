package com.ank.drift.server.service

import com.ank.drift.common.contants.SIXTY_SECONDS
import com.ank.drift.common.model.Gossip
import com.ank.drift.common.model.Payload
import com.ank.drift.server.config.ServerConfig
import com.ank.drift.server.domain.ClientMetadata
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import javax.inject.Inject

@Service
class SessionCacheService @Inject constructor(val serverConfig: ServerConfig) {
    companion object {
        val log = LoggerFactory.getLogger(SessionCacheService::class.java)
    }

    private val payloads = ConcurrentHashMap<String, LinkedBlockingQueue<Payload>>()
    private val activeClients = ConcurrentHashMap<String, ClientMetadata>()

    fun registerClient(clientSession: WebSocketSession) {
        activeClients.putIfAbsent(clientSession.id, ClientMetadata(id = clientSession.id, session = clientSession, lastActive = Date()))
    }

    fun deRegisterClient(sessionId: String): Mono<Void> {
        log.info("${SessionCacheService::deRegisterClient.name}, Removing ClientId=$sessionId from session cache")
        return activeClients.remove(sessionId)?.session?.close() ?: Mono.empty()
    }

    fun savePayload(gossip: Gossip) {
        payloads.get(gossip.requestId.orEmpty())?.let { queue ->
            gossip.payload?.let {
                queue.offer(it.copy(date = Date()))
            }
        } ?: gossip.payload?.let {
            payloads.put(gossip.requestId.orEmpty(), LinkedBlockingQueue(listOf(gossip.payload.copy(date = Date()))))
        }
    }

    fun getPayload(requestId: String): Payload? {
        return payloads.get(requestId)?.poll()
    }

    fun updateClientTimestamp(sessionId: String) {
        val clientMetadata = activeClients.get(sessionId)
        clientMetadata?.let {
            activeClients.put(sessionId, it.copy(lastActive = Date()))
        }
    }

    fun evictStaleClientSession(): Mono<Void> {
        return activeClients.filter {
            DateTime(it.value.lastActive).plusSeconds((serverConfig.remoteClient?.evictDurationInSec
                            ?: SIXTY_SECONDS).toInt()).isBeforeNow
        }.map {
            it.key
        }.toFlux().flatMap {
            deRegisterClient(it)
        }.then()
    }

    fun evictStalePayloads(seconds: Long) {
        payloads.filter {
            it.value.isNullOrEmpty() || DateTime(it.value.peek().date).isBefore(DateTime().minusSeconds(seconds.toInt()))
        }.map {
            it.key
        }.forEach {
            payloads.remove(it)
        }
    }

    fun getClient(): ClientMetadata? {
        return if (activeClients.isNotEmpty()) {
            return activeClients.values.sortedByDescending {
                DateTime(it.lastActive)
            }.firstOrNull()
        } else {
            null
        }
    }
}