package com.ambulanceservice.mobile.data.booking.model

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

class LocalDateTimeAdapter : TypeAdapter<LocalDateTime?>() {
    override fun read(reader: JsonReader): LocalDateTime? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }

        val raw = reader.nextString()
        if (raw.isNullOrBlank()) return null

        return try {
            LocalDateTime.parse(raw)
        } catch (_: Exception) {
            try {
                OffsetDateTime.parse(raw).toLocalDateTime()
            } catch (_: Exception) {
                Instant.parse(raw).atZone(ZoneId.systemDefault()).toLocalDateTime()
            }
        }
    }

    override fun write(out: JsonWriter, value: LocalDateTime?) {
        out.value(value?.toString())
    }
}

