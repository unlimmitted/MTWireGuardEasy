package ru.unlimmitted.mtwgeasy.dto

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JsonContractTests {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun preservesIsRoutingJsonProperty() {
        val json = objectMapper.writeValueAsString(WgInterface(name = "wg-out", isRouting = true))
        assertTrue(json.contains("\"isRouting\":true"), json)

        val restored = objectMapper.readValue<WgInterface>("""{"name":"wg-out","isRouting":true}""")
        assertTrue(restored.isRouting)
    }
}
