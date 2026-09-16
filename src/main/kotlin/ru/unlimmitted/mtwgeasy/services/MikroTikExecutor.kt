package ru.unlimmitted.mtwgeasy.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import me.legrange.mikrotik.ApiConnection
import org.slf4j.LoggerFactory
import ru.unlimmitted.mtwgeasy.dto.MikroTikSettings
import ru.unlimmitted.mtwgeasy.dto.WgInterface
import java.util.Locale

open class MikroTikExecutor {
    protected var connect: ApiConnection? = null
    var settings: MikroTikSettings? = null
        protected set
    var wgInterfaces: List<WgInterface> = emptyList()
        protected set
    var isConfigured: Boolean = false
        protected set

    protected val mikrotikGateway: String? = System.getenv("GATEWAY")
    private val mikrotikUser: String? = System.getenv("MIKROTIK_USER")
    private val mikrotikPassword: String? = System.getenv("MIKROTIK_PASSWORD")

    init {
        initializeConnection()
    }

    @Synchronized
    open fun executeCommand(command: String): List<Map<String, String>> {
        for (attempt in 0..MAX_RETRIES) {
            try {
                ensureConnected()
                return requireNotNull(connect).execute(command)
            } catch (exception: Exception) {
                if (!isRetryableConnectionFailure(exception) || attempt == MAX_RETRIES) {
                    throw RuntimeException("MikroTik command failed: ${exception.message}", exception)
                }
                log.warn(
                    "MikroTik connection failed ({}); reconnecting ({}/{})",
                    exception.message,
                    attempt + 1,
                    MAX_RETRIES,
                )
                closeConnection()
            }
        }
        throw IllegalStateException("MikroTik command retry loop ended unexpectedly")
    }

    fun getHostNumber(): Int {
        val hostNumbers = executeCommand("/interface/wireguard/peers/print").mapNotNull { peer ->
            ALLOWED_ADDRESS_REGEX.find(peer["allowed-address"].orEmpty())?.groupValues?.get(1)?.toIntOrNull()
        }
        return (hostNumbers.maxOrNull() ?: 0) + 1
    }

    fun isSettings(): Boolean = executeCommand("/file/print").any { it["name"] == SETTINGS_FILE }

    @Synchronized
    protected fun initializeConnection() {
        if (mikrotikGateway.isNullOrBlank() || mikrotikUser.isNullOrBlank() || mikrotikPassword.isNullOrBlank()) {
            log.warn("MikroTik connection is disabled: set GATEWAY, MIKROTIK_USER and MIKROTIK_PASSWORD")
            isConfigured = false
            connect = null
            return
        }
        try {
            if (connect?.isConnected == true) {
                connect?.close()
            }
            connect = ApiConnection.connect(mikrotikGateway).also { connection ->
                connection.login(mikrotikUser, mikrotikPassword)
                connection.setTimeout(CONNECTION_TIMEOUT_MS)
            }
            setIsConfigured()
            if (isConfigured) {
                setSettings()
                setWgInterfaces()
            }
            log.info("Connected to MikroTik at {}", mikrotikGateway)
        } catch (exception: Exception) {
            log.error("Failed to connect to MikroTik: {}", exception.message)
            isConfigured = false
            connect = null
        }
    }

    private fun ensureConnected() {
        if (connect?.isConnected != true) {
            log.info("Reconnecting to MikroTik...")
            initializeConnection()
        }
        if (connect?.isConnected != true) {
            throw IllegalStateException("MikroTik is unavailable")
        }
    }

    private fun closeConnection() {
        try {
            connect?.close()
        } catch (exception: Exception) {
            log.debug("Failed to close stale MikroTik connection", exception)
        } finally {
            connect = null
            isConfigured = false
        }
    }

    fun setIsConfigured() {
        isConfigured = isSettings()
    }

    fun setWgInterfaces() {
        wgInterfaces = getInterfaces()
    }

    fun setSettings() {
        settings = readSettings()
    }

    @Synchronized
    fun reconnectIfNeeded() {
        if (connect?.isConnected != true) {
            initializeConnection()
        }
    }

    private fun readSettings(): MikroTikSettings {
        if (!isSettings()) return MikroTikSettings()
        val configContent = executeCommand("/file/print where name=\"$SETTINGS_FILE\"")
            .firstOrNull()
            ?.get("contents")
            ?.replace("\\\"", "\"")
            ?: return MikroTikSettings()
        return jacksonObjectMapper().readValue(configContent, MikroTikSettings::class.java)
    }

    private fun getInterfaces(): List<WgInterface> {
        val routeName = System.getenv("IP_ROUTE_NAME") ?: "WGMTEasy"
        val routes = executeCommand("/ip/route/print where comment=\"$routeName\"")
        val statsByName = executeCommand("/interface/print stats")
            .mapNotNull { stats -> stats["name"]?.let { name -> name to stats } }
            .toMap()

        return executeCommand("/interface/wireguard/print").map { values ->
            val name = values["name"]
            val stats = statsByName[name].orEmpty()
            WgInterface(
                name = name,
                privateKey = values["private-key"],
                publicKey = values["public-key"],
                listenPort = values["listen-port"],
                mtu = values["mtu"],
                disabled = values["disabled"].toBoolean(),
                rxByte = stats["rx-byte"] ?: "0",
                txByte = stats["tx-byte"] ?: "0",
                isRouting = name != settings?.inputWgInterfaceName && routes.any { it["gateway"] == name },
            )
        }
    }

    companion object {
        const val SETTINGS_FILE = "WGMTSettings.conf"
        private const val MAX_RETRIES = 3
        private const val CONNECTION_TIMEOUT_MS = 5_000
        private val ALLOWED_ADDRESS_REGEX = Regex("(?:\\d+\\.){3}(\\d{1,3})/\\d+")
        private val RETRYABLE_MESSAGES = setOf(
            "timed out",
            "timeout",
            "broken pipe",
            "connection reset",
            "socket closed",
            "connection closed",
            "not connected",
            "connection aborted",
            "end of stream",
            "eof",
            "mikrotik is unavailable",
        )
        private val log = LoggerFactory.getLogger(MikroTikExecutor::class.java)

        @JvmStatic
        fun isRetryableConnectionFailure(error: Throwable): Boolean {
            var current: Throwable? = error
            while (current != null) {
                val message = current.message.orEmpty().lowercase(Locale.ROOT)
                if (RETRYABLE_MESSAGES.any(message::contains)) return true
                current = current.cause
            }
            return false
        }
    }
}
