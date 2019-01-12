package com.ank.drift.server.domain

import org.springframework.web.reactive.socket.WebSocketSession
import java.util.Date

data class ClientMetadata(

    val id: String? = null,
    val lastActive: Date? = null,
    val session: WebSocketSession? = null

)