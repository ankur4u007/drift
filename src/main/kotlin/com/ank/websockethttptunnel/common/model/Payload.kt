package com.ank.websockethttptunnel.common.model

import org.springframework.http.HttpMethod


data class Payload (
        val method: HttpMethod? = null,
        val url: String? = null,
        val queryParams: MutableMap<String, MutableList<String>>? = null,
        val headers: MutableMap<String, MutableList<String>>? = null,
        val body: String? = null,
        val status: Int? = null
)