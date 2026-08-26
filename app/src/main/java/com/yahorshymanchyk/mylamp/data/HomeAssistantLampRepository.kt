package com.yahorshymanchyk.mylamp.data

import android.util.Log
import com.yahorshymanchyk.mylamp.domain.LampRepository
import com.yahorshymanchyk.mylamp.secrets.Secrets
import java.io.IOException

private const val TAG = "HomeAssistantLampRepo"

class HomeAssistantLampRepository : LampRepository {
    private val api = HomeAssistantClient.api
    private val entityId = Secrets.HA_LAMP_ENTITY_ID

    override suspend fun turnOn(brightnessPct: Int): Result<Unit> =
        try {
            Log.i(TAG, "turnOn: brightnessPct=$brightnessPct")
            val response = api.turnOn(LightTurnOnRequest(entityId = entityId, brightnessPct = brightnessPct))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(RuntimeException("HTTP ${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "turnOn failed", e)
            Result.failure(e)
        }

    override suspend fun turnOff(): Result<Unit> =
        try {
            Log.i(TAG, "turnOff")
            val response = api.turnOff(LightTurnOffRequest(entityId = entityId))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(RuntimeException("HTTP ${response.code()}"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "turnOff failed", e)
            Result.failure(e)
        }
}
