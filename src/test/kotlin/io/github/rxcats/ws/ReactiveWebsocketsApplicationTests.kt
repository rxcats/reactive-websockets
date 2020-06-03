package io.github.rxcats.ws

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.ReactiveRedisOperations
import reactor.core.Disposable
import reactor.core.scheduler.Schedulers
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootTest
class ReactiveWebsocketsApplicationTests {

    @Autowired
    private lateinit var reactiveRedisOperations: ReactiveRedisOperations<String, MessagePayload>

    class MessageSubscriber(private val reactiveRedisOperations: ReactiveRedisOperations<String, MessagePayload>) {
        private lateinit var disposable: Disposable

        fun onSubscribe() {
            disposable = reactiveRedisOperations.listenToChannel("wschannel")
                .subscribeOn(Schedulers.elastic())
                .doOnNext {
                    println(it.message)
                }
                .doOnComplete {
                    println("completed")
                }
                .subscribe()
        }

        fun dispose() {
            disposable.dispose()
        }

        fun test(): Boolean {
            return disposable.isDisposed
        }
    }

    @Test
    fun send() {
        val subscriber = MessageSubscriber(reactiveRedisOperations)
        subscriber.onSubscribe()

        val msg = MessagePayload(act = "send", characterNo = 0, message = "test")

        reactiveRedisOperations.convertAndSend("wschannel", msg)
            .subscribe()

        reactiveRedisOperations.convertAndSend("wschannel", msg)
            .subscribe()

        Thread.sleep(100)
        subscriber.dispose()

        println(subscriber.test())

        reactiveRedisOperations.convertAndSend("wschannel", msg)
            .subscribe()

        reactiveRedisOperations.convertAndSend("wschannel", msg)
            .subscribe()

        Thread.sleep(100)
    }

}
