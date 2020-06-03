package io.github.rxcats.ws

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisOperations
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration(proxyBeanMethods = false)
class RedisConfiguration {
    @Bean
    fun redisOperations(connectionFactory: ReactiveRedisConnectionFactory): ReactiveRedisOperations<String, MessagePayload> {
        val serializer = Jackson2JsonRedisSerializer(MessagePayload::class.java)
        val builder = RedisSerializationContext.newSerializationContext<String, MessagePayload>(StringRedisSerializer())
        val context = builder.value(serializer).build()
        return ReactiveRedisTemplate(connectionFactory, context)
    }

    @Bean
    fun container(connectionFactory: ReactiveRedisConnectionFactory): ReactiveRedisMessageListenerContainer {
        return ReactiveRedisMessageListenerContainer(connectionFactory)
    }
}