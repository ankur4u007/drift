package com.ank.websockethttptunnel.server.service

import com.ank.websockethttptunnel.common.model.Gossip
import com.ank.websockethttptunnel.common.model.Payload
import com.ank.websockethttptunnel.server.domain.ClientMetadata
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketSession
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

@Service
class SessionCacheService {
    companion object {
        val log = LoggerFactory.getLogger(SessionCacheService::class.java)
        val payloads = ConcurrentHashMap<String, Payload>()
        val activeClients = ConcurrentHashMap<String, ClientMetadata>()
    }


    fun registerClient(clientSession: WebSocketSession) {
        activeClients.putIfAbsent(clientSession.id, ClientMetadata(id = clientSession.id, session = clientSession, lastActive = Date()))
    }

    fun deRegisterClient(clientSession: WebSocketSession) {
        activeClients.minus(clientSession)
    }

    fun savePayload(gossip: Gossip) {
        payloads.putIfAbsent(gossip.requestId.orEmpty(), gossip.payload ?: Payload())
    }

    fun updateClientTimestamp(sessionId: String) {
        val clientMetadata = activeClients.get(sessionId)
        clientMetadata?.let {
            activeClients.put(sessionId, it.copy(lastActive = Date()))
        }
    }
}