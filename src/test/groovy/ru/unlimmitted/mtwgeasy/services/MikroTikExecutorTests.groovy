package ru.unlimmitted.mtwgeasy.services

import org.junit.jupiter.api.Test

import java.net.SocketException

import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

class MikroTikExecutorTests {

    @Test
    void recognizesRecoverableConnectionFailuresAcrossCauseChain() {
        assertTrue(MikroTikExecutor.isRetryableConnectionFailure(
                new RuntimeException("command failed", new SocketException("Broken pipe"))
        ))
        assertTrue(MikroTikExecutor.isRetryableConnectionFailure(
                new IllegalStateException("MikroTik is unavailable")
        ))
        assertTrue(MikroTikExecutor.isRetryableConnectionFailure(
                new RuntimeException("Connection reset by peer")
        ))
    }

    @Test
    void doesNotRetryRouterValidationErrors() {
        assertFalse(MikroTikExecutor.isRetryableConnectionFailure(
                new RuntimeException("invalid value for argument")
        ))
    }
}
