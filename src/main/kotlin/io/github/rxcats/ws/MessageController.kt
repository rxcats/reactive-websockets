package io.github.rxcats.ws

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MessageController {
    @Autowired
    private lateinit var messageHandler: WebSocketMessageHandler

    @GetMapping("/send")
    fun send(): String {
        messageHandler.sendMessage(MessagePayload(
            act = "send",
            code = 0,
            sessionId = "",
            characterNo = 0,
            message = "broadcast"
        ))
        return "ok"
    }
}