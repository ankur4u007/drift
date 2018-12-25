package com.ank.websockethttptunnel.server.transport.ws

import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketHandler
import javax.inject.Inject

@Service
class WebSockethandlers @Inject constructor(
        val registrationHandler: ClientRequestHandler
) {

    fun getAllwebSockethandlers() : Map<String, WebSocketHandler>  {
        return hashMapOf<String, WebSocketHandler>(
                "/websocket" to registrationHandler
        )
    }
}