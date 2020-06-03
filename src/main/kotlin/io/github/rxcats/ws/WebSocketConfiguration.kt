package io.github.rxcats.ws

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisOperations
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

@Configuration(proxyBeanMethods = false)
class WebSocketConfiguration {
    @Bean
    fun websocketHandlerAdapter() = WebSocketHandlerAdapter()

    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper().registerModule(KotlinModule(nullIsSameAsDefault = true))

    @Bean
    fun reactiveRedisOperations(reactiveRedisConnectionFactory: ReactiveRedisConnectionFactory,
                                objectMapper: ObjectMapper): ReactiveRedisOperations<String, MessagePayload> {

        val serializer = Jackson2JsonRedisSerializer(MessagePayload::class.java)
        serializer.setObjectMapper(objectMapper)

        val serializationContext = RedisSerializationContext.newSerializationContext<String, MessagePayload>()
            .key(RedisSerializer.string())
            .value(serializer)
            .hashKey(RedisSerializer.string())
            .hashValue(serializer)
            .build()
        return ReactiveRedisTemplate(reactiveRedisConnectionFactory, serializationContext)
    }

    @Bean
    fun handlerMapping(messageHandler: WebSocketMessageHandler): HandlerMapping {
        val handlerMapping = SimpleUrlHandlerMapping()
        handlerMapping.urlMap = mapOf("/ws" to messageHandler)
        handlerMapping.order = 1
        return handlerMapping
    }
}