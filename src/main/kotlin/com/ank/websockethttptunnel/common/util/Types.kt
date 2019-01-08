package com.ank.websockethttptunnel.common.util

import org.slf4j.Logger
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.util.SerializationUtils
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

fun MutableMap<String, MutableList<String>>?.toMultiValueMap(): MultiValueMap<String, String> {
    val linkedMultiValueMap = LinkedMultiValueMap<String, String>()
    this?.forEach { entry ->
        entry.value.forEach { value ->
            linkedMultiValueMap.add(entry.key, value)
        }
    }
    return linkedMultiValueMap
}

fun ByteArray?.orEmpty(): ByteArray {
    return this ?: "".toByteArray()
}

fun <T> WebSocketSession.sendAsyncBinaryData(data: T, scheduler: Scheduler, log: Logger, methodName: String): Disposable {
    return this.send(this.binaryMessage {
        log.info("op=$methodName, Sending=$data")
        it.wrap(SerializationUtils.serialize(data).orEmpty())
    }.toMono()).subscribeOn(scheduler).doOnError {
        log.error("op=$methodName, ${it.message}", it)
    }.subscribe()
}