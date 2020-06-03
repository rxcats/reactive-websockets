package io.github.rxcats.ws

enum class ResultCode(val code: Int) {
    Success(0),
    Unknown(90000),
    InvalidMessagePayload(90001)
}

class ServiceException(private val resultCode: ResultCode) : RuntimeException("""${resultCode.name}(${resultCode.code})""") {
    fun getError() = resultCode.name
    fun getCode() = resultCode.code
}