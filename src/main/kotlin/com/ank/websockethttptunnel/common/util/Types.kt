package com.ank.websockethttptunnel.common.util

import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

fun MutableMap<String, MutableList<String>>?.toMultiValueMap(): MultiValueMap<String, String> {
    val linkedMultiValueMap = LinkedMultiValueMap<String, String>()
    this?.forEach { entry ->
        entry.value.forEach { value ->
            linkedMultiValueMap.add(entry.key, value)
        }
    }
    return linkedMultiValueMap
}