@file:OptIn(ExperimentalSerializationApi::class)

package io.jeremymorren.opentelemetry

import com.intellij.openapi.diagnostic.Logger
import io.jeremymorren.opentelemetry.listeners.DebugMessageListener
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class TelemetryFactory {
    // Very long lines can be split into multiple parts
    // We need to concatenate them before parsing
    private val currentValue: StringBuilder = StringBuilder()

    public fun tryCreateFromDebugOutputLog(output: String): Telemetry? {
        val openTelemetryLogPrefix = "{\"activity\":{\"traceId\":\""

        val value = output.trimEnd()

        if (currentValue.isEmpty() && !value.startsWith(openTelemetryLogPrefix)) {
            // Not a telemetry log
            return null
        }

        currentValue.append(value)
        if (!value.endsWith("}")) {
            return null
        }

        val json = currentValue.toString()
        currentValue.clear()

        return try {
            val telemetry = Json.decodeFromString<TelemetryInfo>(json)
            return Telemetry(formatJson(json), telemetry)
        } catch (e: SerializationException) {
            val logger = Logger.getInstance(TelemetryFactory::class.java)
            logger.error("Failed to parse telemetry log", e)
            currentValue.clear()
            return null
        }
    }

    private fun formatJson(json: String): String {
        val prettyJson = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
        val obj = Json.decodeFromString<JsonObject>(json)
        return prettyJson.encodeToString(JsonObject.serializer(), obj)
    }
}