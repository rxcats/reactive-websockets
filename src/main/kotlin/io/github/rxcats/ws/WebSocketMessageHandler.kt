package io.github.rxcats.ws

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ReactiveRedisOperations
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.DirectProcessor
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.concurrent.ConcurrentHashMap

@Component
class WebSocketMessageHandler(private val reactiveRedisOperations: ReactiveRedisOperations<String, MessagePayload>,
                              private val objectMapper: ObjectMapper) : WebSocketHandler {

    companion object {
        private val log = LoggerFactory.getLogger(WebSocketMessageHandler::class.java)

        private val clients = ConcurrentHashMap<String, DirectProcessor<MessagePayload>>()

        private const val REDIS_CHANNEL_NAME = "wschannel"
    }

    init {
        listenRedisChannel()
    }

    private fun listenRedisChannel() {
        reactiveRedisOperations.listenToChannel(REDIS_CHANNEL_NAME)
            .subscribeOn(Schedulers.elastic())
            .doOnNext {
                for ((_, processor) in clients) {
                    processor.onNext(it.message)
                }
            }.subscribe()
    }

    fun sendMessage(payload: MessagePayload) {
        reactiveRedisOperations.convertAndSend(REDIS_CHANNEL_NAME, payload)
            .subscribeOn(Schedulers.elastic())
            .subscribe()
    }

    private fun parsePayload(websocketMessage: WebSocketMessage, sessionId: String): MessagePayload {
        try {
            val msg = objectMapper.readValue<MessagePayload>(websocketMessage.payloadAsText)
            msg.sessionId = sessionId
            return msg
        } catch (e: JsonParseException) {
            throw ServiceException(ResultCode.InvalidMessagePayload)
        }
    }

    private fun getSessionCharacterNo(session: WebSocketSession): Long {
        return session.attributes.getOrDefault("characterNo", 0) as Long
    }

    private fun setSessionCharacterNo(session: WebSocketSession, characterNo: Long) {
        session.attributes.putIfAbsent("characterNo", characterNo)
    }

    override fun handle(session: WebSocketSession): Mono<Void> {
        if (clients.containsKey(session.id).not()) {
            clients[session.id] = DirectProcessor.create()
        }

        val processor: DirectProcessor<MessagePayload> = clients[session.id]!!

        val out = processor.map {
            session.textMessage(objectMapper.writeValueAsString(it))
        }

        log.info("session {}", session.id)

        val income = session.receive()
            .map {
                parsePayload(it, session.id)
            }
            .onErrorContinue { t, _ ->
                log.error(t.message, t)
                val errorMessage = if (t is ServiceException) {
                    MessagePayload(
                        act = "error",
                        code = t.getCode(),
                        sessionId = session.id,
                        characterNo = getSessionCharacterNo(session),
                        message = t.getError()
                    )
                } else {
                    val unknown = ResultCode.Unknown
                    MessagePayload(
                        act = "error",
                        code = unknown.code,
                        sessionId = session.id,
                        characterNo = getSessionCharacterNo(session),
                        message = unknown.name
                    )
                }
                processor.onNext(errorMessage)
            }
            .doOnNext {
                when (it.act) {
                    "connect" -> {
                        setSessionCharacterNo(session, it.characterNo)
                    }
                    "send" -> {
                        sendMessage(it)
                    }
                }
            }
            .doOnError {
                log.error(it.message, it)
            }
            .doOnComplete {
                // session closed
                log.info("completed {}", session.id)
                clients.remove(session.id)
                sendMessage(MessagePayload(
                    act = "disconnect",
                    sessionId = session.id,
                    characterNo = getSessionCharacterNo(session)
                ))
            }
            .then()

        return session.send(out).and(income)
    }
}