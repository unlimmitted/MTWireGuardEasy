package ru.unlimmitted.mtwgeasy.services

import com.fasterxml.jackson.databind.ObjectMapper
import me.legrange.mikrotik.ApiConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.unlimmitted.mtwgeasy.dto.MikroTikSettings
import ru.unlimmitted.mtwgeasy.dto.WgInterface

import java.util.regex.Matcher
import java.util.regex.Pattern

class MikroTikExecutor {

    private static final Logger log = LoggerFactory.getLogger(MikroTikExecutor.class)
    private static final int MAX_RETRIES = 3
    private static final int CONNECTION_TIMEOUT_MS = 5_000

    ApiConnection connect
    MikroTikSettings settings
    List<WgInterface> wgInterfaces
    Boolean isConfigured

    final static String settingsFile = "WGMTSettings.conf"
    final String mikrotikGateway = System.getenv("GATEWAY")
    private final String mikrotikUser = System.getenv("MIKROTIK_USER")
    private final String mikrotikPassword = System.getenv("MIKROTIK_PASSWORD")

    MikroTikExecutor() {
        initializeConnection()
    }

    synchronized List<Map<String, String>> executeCommand(String command) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                ensureConnected()
                return connect.execute(command)
            } catch (Exception e) {
                if (!isRetryableConnectionFailure(e) || attempt == MAX_RETRIES) {
                    throw new RuntimeException("MikroTik command failed: ${e.message}", e)
                }
                log.warn(
                        "MikroTik connection failed ({}); reconnecting ({}/{})",
                        e.message,
                        attempt + 1,
                        MAX_RETRIES
                )
                closeConnection()
            }
        }
        throw new IllegalStateException("MikroTik command retry loop ended unexpectedly")
    }

    static boolean isRetryableConnectionFailure(Throwable error) {
        Set<String> retryableMessages = [
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
                "mikrotik is unavailable"
        ]
        Throwable current = error
        while (current != null) {
            String message = current.message?.toLowerCase(Locale.ROOT) ?: ""
            if (retryableMessages.any { message.contains(it) }) {
                return true
            }
            current = current.cause
        }
        return false
    }

    Integer getHostNumber() {
        List<Integer> results = []
        executeCommand("/interface/wireguard/peers/print").forEach {
            String regex = "(?:\\d+\\.){3}(\\d{1,3})/\\d+"
            Matcher matcher = Pattern.compile(regex).matcher(it.get('allowed-address') ?: '')
            if (matcher.find()) {
                results.add(matcher.group(1).toInteger())
            }
        }
        return results ? results.max() + 1 : 1
    }

    Boolean isSettings() {
        return executeCommand('/file/print').find {
            it.name == settingsFile
        } != null
    }

    protected synchronized void initializeConnection() {
        if (!mikrotikGateway || !mikrotikUser || !mikrotikPassword) {
            log.warn("MikroTik connection is disabled: set GATEWAY, MIKROTIK_USER and MIKROTIK_PASSWORD")
            isConfigured = false
            connect = null
            return
        }
        try {
            if (connect != null && connect.isConnected()) {
                connect.close()
            }
            connect = ApiConnection.connect(mikrotikGateway)
            connect.login(mikrotikUser, mikrotikPassword)
            connect.setTimeout(CONNECTION_TIMEOUT_MS)
            setIsConfigured()
            if (isConfigured) {
                setSettings()
                setWgInterfaces()
            }
            log.info("Connected to MikroTik at {}", mikrotikGateway)
        } catch (Exception e) {
            log.error("Failed to connect to MikroTik: {}", e.message)
            isConfigured = false
            connect = null
        }
    }

    private void ensureConnected() {
        if (connect == null || !connect.isConnected()) {
            log.info("Reconnecting to MikroTik...")
            initializeConnection()
        }
        if (connect == null || !connect.isConnected()) {
            throw new IllegalStateException("MikroTik is unavailable")
        }
    }

    private void closeConnection() {
        try {
            connect?.close()
        } catch (Exception e) {
            log.debug("Failed to close stale MikroTik connection", e)
        } finally {
            connect = null
            isConfigured = false
        }
    }

    void setIsConfigured() {
        isConfigured = isSettings()
    }

    void setWgInterfaces() {
        wgInterfaces = getInterfaces()
    }

    void setSettings() {
        settings = readSettings()
    }

    synchronized void reconnectIfNeeded() {
        if (connect == null || !connect.isConnected()) {
            initializeConnection()
        }
    }

    private MikroTikSettings readSettings() {
        ObjectMapper objectMapper = new ObjectMapper()
        if (isSettings()) {
            String configContent = executeCommand("/file/print where name=\"${settingsFile}\"")
                    .get(0)?.get('contents')?.replace("\\\"", "\"")
            return objectMapper.readValue(configContent, MikroTikSettings.class)
        } else {
            return new MikroTikSettings()
        }
    }

    private List<WgInterface> getInterfaces() {
        String ipRouteName = System.getenv("IP_ROUTE_NAME") ?: "WGMTEasy"
        List<Map<String, String>> routes = executeCommand("/ip/route/print where comment=\"${ipRouteName}\"")
        Map<String, Map<String, String>> statsByName = executeCommand("/interface/print stats")
                .findAll { it.get("name") }
                .collectEntries { [(it.get("name")): it] }

        return executeCommand('/interface/wireguard/print').collect {
            WgInterface wgInterface = new WgInterface()
            wgInterface.name = it.get('name')
            wgInterface.privateKey = it.get('private-key')
            wgInterface.publicKey = it.get('public-key')
            wgInterface.listenPort = it.get('listen-port')
            wgInterface.mtu = it.get('mtu')
            wgInterface.disabled = it.get('disabled').toBoolean()

            Map<String, String> intStats = statsByName.get(wgInterface.name) ?: [:]
            wgInterface.rxByte = intStats.get("rx-byte") ?: "0"
            wgInterface.txByte = intStats.get("tx-byte") ?: "0"

            if (wgInterface.name != settings?.inputWgInterfaceName) {
                wgInterface.isRouting = routes.any { route -> route.gateway == wgInterface.name }
            }

            return wgInterface
        }
    }
}
