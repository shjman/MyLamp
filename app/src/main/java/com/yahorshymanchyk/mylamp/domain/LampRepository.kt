package com.yahorshymanchyk.mylamp.domain

interface LampRepository {
    suspend fun turnOn(brightnessPct: Int): Result<Unit>

    suspend fun turnOff(): Result<Unit>
}
