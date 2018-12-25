package com.ank.websockethttptunnel.server.service

import com.ank.websockethttptunnel.common.contants.SIXTY_SECONDS
import com.ank.websockethttptunnel.common.model.Gossip
import com.ank.websockethttptunnel.common.model.Payload
import com.ank.websockethttptunnel.server.config.ServerConfig
import com.ank.websockethttptunnel.server.domain.ClientMetadata
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketSession
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.random.Random

@Service
class SessionCacheService @Inject constructor(val serverConfig: ServerConfig) {
    companion object {
        val log = LoggerFactory.getLogger(SessionCacheService::class.java)

    }

    val payloads = ConcurrentHashMap<String, Payload>()
    val activeClients = ConcurrentHashMap<String, ClientMetadata>()
    val randomGenerator = Random(Int.MAX_VALUE)


    fun registerClient(clientSession: WebSocketSession) {
        activeClients.putIfAbsent(clientSession.id, ClientMetadata(id = clientSession.id, session = clientSession, lastActive = Date()))
    }

    fun deRegisterClient(sessionId: String) {
        log.info("${SessionCacheService::deRegisterClient.name}, Removing ClientId=${sessionId} from session cache")
        activeClients.remove(sessionId)?.session?.close()?.subscribe()

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

    fun evictCache() {

        activeClients.filter {
            DateTime(it.value.lastActive).plusSeconds((serverConfig.remoteClient?.evictDurationInSec
                            ?: SIXTY_SECONDS).toInt()).isBeforeNow
        }.forEach {
            deRegisterClient(it.key)
        }
    }

    fun getClient() : ClientMetadata? {
        return activeClients.values.elementAtOrElse(randomGenerator.nextInt(activeClients.size), {
            activeClients.values.firstOrNull()
        })
    }
}