package com.yahorshymanchyk.mylamp.data

import com.squareup.moshi.Json
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// Reflection-based Moshi (KotlinJsonAdapterFactory, see HomeAssistantClient) — no codegen
// dependency, so no @JsonClass(generateAdapter = true) here; @Json still applies via reflection.
data class LightTurnOnRequest(
    @Json(name = "entity_id") val entityId: String,
    @Json(name = "brightness_pct") val brightnessPct: Int,
)

data class LightTurnOffRequest(
    @Json(name = "entity_id") val entityId: String,
)

interface HomeAssistantApi {
    // Response<ResponseBody>, not Response<Unit>: HA returns a non-empty JSON array of
    // changed states, and Moshi has no adapter to convert that into Unit — we only care
    // about the status code, so the body is left unparsed on purpose.
    @POST("api/services/light/turn_on")
    suspend fun turnOn(
        @Body body: LightTurnOnRequest,
    ): Response<ResponseBody>

    @POST("api/services/light/turn_off")
    suspend fun turnOff(
        @Body body: LightTurnOffRequest,
    ): Response<ResponseBody>
}
