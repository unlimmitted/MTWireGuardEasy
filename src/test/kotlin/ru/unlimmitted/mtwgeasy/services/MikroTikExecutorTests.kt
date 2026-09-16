package ru.unlimmitted.mtwgeasy.services

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.SocketException

class MikroTikExecutorTests {
    @Test
    fun recognizesRecoverableConnectionFailuresAcrossCauseChain() {
        assertTrue(
            MikroTikExecutor.isRetryableConnectionFailure(
                RuntimeException("command failed", SocketException("Broken pipe")),
            ),
        )
        assertTrue(
            MikroTikExecutor.isRetryableConnectionFailure(
                IllegalStateException("MikroTik is unavailable"),
            ),
        )
        assertTrue(
            MikroTikExecutor.isRetryableConnectionFailure(
                RuntimeException("Connection reset by peer"),
            ),
        )
    }

    @Test
    fun doesNotRetryRouterValidationErrors() {
        assertFalse(
            MikroTikExecutor.isRetryableConnectionFailure(
                RuntimeException("invalid value for argument"),
            ),
        )
    }
}
