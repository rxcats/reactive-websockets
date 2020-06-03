package io.github.rxcats.ws

data class MessagePayload(
    val act: String,

    val code: Int = 0,

    var sessionId: String = "",

    val characterNo: Long,

    val channel: String = "",

    val message: String = ""
)