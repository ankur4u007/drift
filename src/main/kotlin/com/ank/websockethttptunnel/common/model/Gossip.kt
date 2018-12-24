package com.ank.websockethttptunnel.common.model

data class Gossip (
        val requestId: String? = null,
        val event: Event? = null,
        val message: String? = null,
        val status: Int? = null,
        val payload: Payload? = null
)

enum class Event {
    CLIENT_PING,
    SERVER_PONG,
    SERVER_REQUEST,
    CLIENT_RESPOND,
    SERVER_REQUEST_ACK
}