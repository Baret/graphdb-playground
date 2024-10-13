package de.gleex.graphdb.playground.neo4j.spring.controller

import de.gleex.graphdb.playground.model.InvalidModelException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus

@ControllerAdvice
class ExceptionHandler {

    @ExceptionHandler(InvalidModelException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleInvalidModelException(exception: InvalidModelException): ErrorResponse =
        ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.message ?: "Invalid model detected")
}

data class ErrorResponse(
    val httpErrorCode: Int,
    val errorMessage: String
)