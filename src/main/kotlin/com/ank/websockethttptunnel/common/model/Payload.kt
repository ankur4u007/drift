package com.ank.websockethttptunnel.common.model

data class Payload (
    val url: String? = null,
    val headers: Map<String, String>? = null,
    val body: String? = null
)