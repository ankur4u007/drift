package com.ank.websockethttptunnel.common.model

import io.netty.handler.codec.http.HttpMethod

data class Payload (
        val method: HttpMethod? = null,
        val url: String? = null,
        val queryParams: MutableMap<String, String>? = null,
        val headers: MutableMap<String, MutableList<String>>? = null,
        val body: String? = null
)