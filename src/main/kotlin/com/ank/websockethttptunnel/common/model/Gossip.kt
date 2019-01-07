package com.ank.websockethttptunnel.common.model

import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpMethod
import org.springframework.util.MultiValueMap
import java.io.Serializable

data class Gossip (
        val requestId: String? = null,
        val event: Event? = null,
        val message: String? = null,
        val status: Int? = null,
        val payload: Payload? = null
) : Serializable

enum class Event : Serializable {
    CLIENT_PING,
    SERVER_PONG,
    SERVER_REQUEST,
    CLIENT_RESPOND,
    SERVER_REQUEST_ACK
}

data class Payload (
        val method: HttpMethod? = null,
        val url: String? = null,
        val queryParams: MultiValueMap<String, String>? = null,
        val headers: MultiValueMap<String, String>? = null,
        val body: ByteArray = ByteArray(0),
        val status: Int? = null
) : Serializable