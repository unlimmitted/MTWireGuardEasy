package ru.unlimmitted.mtwgeasy.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.whispersystems.curve25519.Curve25519
import ru.unlimmitted.mtwgeasy.dto.AddressList
import ru.unlimmitted.mtwgeasy.dto.EtherInterface
import ru.unlimmitted.mtwgeasy.dto.MikroTikInfo
import ru.unlimmitted.mtwgeasy.dto.MikroTikSettings
import ru.unlimmitted.mtwgeasy.dto.NewWireguardInterface
import ru.unlimmitted.mtwgeasy.dto.Peer
import ru.unlimmitted.mtwgeasy.dto.RenamePeerRequest
import ru.unlimmitted.mtwgeasy.dto.WgInterface
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

@Service
class MikroTikService : MikroTikExecutor() {
    private val peerPrivateKeys = ConcurrentHashMap<String, String>()

    fun runConfigurator(settings: MikroTikSettings) {
        RouterConfigurator(settings).run()
        initializeConnection()
    }

    fun getPeers(): List<Peer> {
        val currentSettings = settings
        val routingByAddress = getAddressList()
            .filter { it.listName == currentSettings?.toVpnAddressList }
            .mapNotNull { entry -> entry.address?.let { address -> address to entry } }
            .toMap()

        val rows = executeCommand("/interface/wireguard/peers/print")
        val clientConfigApi = usesClientConfigApi(rows)

        return rows
            .filter { values ->
                !isReservedPeer(values) &&
                    (clientConfigApi || !values["private-key"].isNullOrBlank()) &&
                    (!clientConfigApi || values["interface"] == currentSettings?.inputWgInterfaceName)
            }
            .map { values ->
                val interfaceName = values["interface"]
                val wgInterface = findInterface(interfaceName)
                val allowedAddress = values["allowed-address"]
                val addressEntry = allowedAddress
                    ?.substringBefore('/')
                    ?.let(routingByAddress::get)

                Peer(
                    id = values[".id"],
                    allowedAddress = allowedAddress,
                    tx = values["tx"],
                    lastHandshake = values["last-handshake"],
                    rx = values["rx"],
                    privateKey = resolvePeerPrivateKey(values, clientConfigApi),
                    name = values["name"],
                    disabled = values["disabled"].toBoolean(),
                    currentEndpointPort = values["current-endpoint-port"],
                    currentEndpointAddress = values["current-endpoint-address"],
                    publicKey = wgInterface?.publicKey,
                    peerInterface = interfaceName,
                    presharedKey = values["preshared-key"],
                    endpoint = currentSettings?.endpoint,
                    endpointPort = wgInterface?.listenPort,
                    doubleVpn = addressEntry?.disabled == false,
                )
            }
    }

    private fun resolvePeerPrivateKey(values: Map<String, String>, clientConfigApi: Boolean): String? {
        values["private-key"]?.takeIf(String::isNotBlank)?.let { return it }
        if (!clientConfigApi) return null
        val id = values[".id"]?.takeIf(String::isNotBlank) ?: return null
        peerPrivateKeys[id]?.let { return it }

        val privateKey = runCatching {
            executeCommand(
                "/interface/wireguard/peers/show-client-config number=\"$id\" show-sensitive=yes",
            )
                .firstOrNull()
                ?.get("conf")
                ?.lineSequence()
                ?.map(String::trim)
                ?.firstOrNull { line -> line.substringBefore('=').trim().equals("PrivateKey", ignoreCase = true) }
                ?.substringAfter('=')
                ?.trim()
                ?.takeIf(String::isNotBlank)
        }.onFailure { exception ->
            log.debug("Failed to read client config for WireGuard peer {}: {}", id, exception.message)
        }.getOrNull()

        if (privateKey != null) peerPrivateKeys[id] = privateKey
        return privateKey
    }

    fun getEtherInterfaces(): List<EtherInterface> =
        executeCommand("/interface/print where type=\"ether\"").map { values ->
            val name = values["name"]
            EtherInterface(
                id = values[".id"],
                name = name,
                macAddress = values["mac-address"],
                network = executeCommand("/ip/address/print where interface=\"$name\"")
                    .firstOrNull()
                    ?.get("network"),
            )
        }

    fun findInterface(interfaceName: String?): WgInterface? = wgInterfaces.find { it.name == interfaceName }

    fun getAddressList(): List<AddressList> =
        executeCommand("/ip/firewall/address-list/print").map { values ->
            AddressList(
                id = values[".id"],
                disabled = values["disabled"].toBoolean(),
                comment = values["comment"],
                listName = values["list"],
                address = values["address"],
            )
        }

    fun getMikroTikInfo(): MikroTikInfo {
        val info = MikroTikInfo(interfaces = wgInterfaces)
        try {
            val resource = executeCommand("/system/resource/print").firstOrNull().orEmpty()
            info.routerBoard = resource["board-name"] ?: resource["platform"] ?: "<undefined>"
            info.version = resource["version"] ?: "<undefined>"
        } catch (exception: Exception) {
            info.routerBoard = "<undefined>"
            info.version = "<undefined>"
            log.warn("Failed to get MikroTik system info: {}", exception.message)
        }
        return info
    }

    @Synchronized
    fun createNewPeer(peerName: String) {
        val safePeerName = requireSafeValue(peerName, "Peer name", 64)
        val currentSettings = requireSettings()
        val privateKey = Base64.getEncoder().encodeToString(
            Curve25519.getInstance(Curve25519.JAVA).generateKeyPair().privateKey,
        )
        val inputAddress = currentSettings.inputWgAddress.orEmpty()
        val prefix = inputAddress.substringBefore('/').split('.').takeIf { it.size == 4 }
            ?: throw IllegalStateException("Input WireGuard address is not configured correctly")
        val host = firstAvailableHostNumber(
            inputAddress,
            currentSettings.inputWgInterfaceName.orEmpty(),
            executeCommand("/interface/wireguard/peers/print"),
        )
        val ip = "${prefix.take(3).joinToString(".")}.$host"
        val peerQuery = command(
            "/interface/wireguard/peers/add",
            "interface=\"${currentSettings.inputWgInterfaceName}\"",
            "private-key=\"$privateKey\"",
            "allowed-address=$ip/32",
            "name=\"$safePeerName\"",
        )
        executeCommand(peerQuery)
        executeCommand(
            "/ip/firewall/address-list/add address=$ip list=${currentSettings.toVpnAddressList} comment=$safePeerName",
        )
    }

    fun renamePeer(request: RenamePeerRequest) {
        val peerId = requireRouterOsId(request.id)
        val peerName = requireSafeValue(request.name, "Peer name", 64)
        val peers = getPeers()
        val peer = peers.find { it.id == peerId }
            ?: throw IllegalArgumentException("Peer was not found")

        if (peers.any { it.id != peerId && it.name.equals(peerName, ignoreCase = true) }) {
            throw IllegalArgumentException("Peer name already exists")
        }
        if (peer.name == peerName) return

        executeCommand("/interface/wireguard/peers/set numbers=$peerId name=\"$peerName\"")
        findPeerInAddressList(getAddressList(), peer.allowedAddress?.substringBefore('/'))
            ?.id
            ?.let(::requireRouterOsId)
            ?.let { addressListId ->
                executeCommand("/ip/firewall/address-list/set numbers=$addressListId comment=\"$peerName\"")
            }
    }

    fun setPeerStatus(peer: Peer) {
        val peerId = requireRouterOsId(peer.id)
        val action = if (peer.disabled) "disable" else "enable"
        executeCommand("/interface/wireguard/peers/$action numbers=$peerId")
    }

    fun changeRouting(peer: Peer) {
        val address = peer.allowedAddress?.substringBefore('/')
        val addressEntry = findPeerInAddressList(getAddressList(), address)
            ?: throw IllegalArgumentException("Peer routing entry was not found")
        val action = if (peer.doubleVpn) "disable" else "enable"
        executeCommand("/ip/firewall/address-list/$action numbers=${requireRouterOsId(addressEntry.id)}")
    }

    fun setDoubleVpnInversion(inverted: Boolean): MikroTikSettings {
        val currentSettings = requireSettings()
        if (!currentSettings.vpnChainMode) {
            throw IllegalStateException("Double VPN mode is not configured")
        }
        if (currentSettings.doubleVpnInverted == inverted) return currentSettings

        val routingMark = requireSafeValue(currentSettings.toVpnTableName, "Routing table name", 64)
        val addressList = requireSafeValue(currentSettings.toVpnAddressList, "Address list name", 64)
        val mangleRuleId = executeCommand(
            "/ip/firewall/mangle/print where new-routing-mark=\"$routingMark\"",
        ).firstOrNull { rule ->
            rule["chain"] == "prerouting" &&
                rule["in-interface"] == currentSettings.inputWgInterfaceName
        }?.get(".id")?.let(::requireRouterOsId)
            ?: throw IllegalStateException("Double VPN routing rule was not found")

        val settingsFileId = executeCommand("/file/print where name=\"${MikroTikExecutor.SETTINGS_FILE}\"")
            .firstOrNull()
            ?.get(".id")
            ?.let(::requireRouterOsId)
            ?: throw IllegalStateException("MikroTik settings file was not found")

        val updatedSettings = currentSettings.copy(doubleVpnInverted = inverted)
        val sourceAddressList = if (inverted) "!$addressList" else addressList
        val previousSourceAddressList = if (currentSettings.doubleVpnInverted) "!$addressList" else addressList

        executeCommand(
            "/ip/firewall/mangle/set numbers=$mangleRuleId src-address-list=\"$sourceAddressList\"",
        )
        try {
            val json = jacksonObjectMapper()
                .writeValueAsString(updatedSettings)
                .replace("\"", "\\\"")
            executeCommand("/file/set numbers=$settingsFileId contents='$json'")
        } catch (exception: Exception) {
            runCatching {
                executeCommand(
                    "/ip/firewall/mangle/set numbers=$mangleRuleId " +
                        "src-address-list=\"$previousSourceAddressList\"",
                )
            }
            throw exception
        }

        settings = updatedSettings
        return updatedSettings
    }

    fun removePeer(peer: Peer) {
        val peerId = requireRouterOsId(peer.id)
        val addressEntry = findPeerInAddressList(getAddressList(), peer.allowedAddress?.substringBefore('/'))
            ?: throw IllegalArgumentException("Peer routing entry was not found")
        executeCommand("/interface/wireguard/peers/remove numbers=$peerId")
        executeCommand("/ip/firewall/address-list/remove numbers=${requireRouterOsId(addressEntry.id)}")
    }

    fun changeVpnRouting(wgInterface: WgInterface) {
        val interfaceName = requireSafeValue(wgInterface.name, "Interface name", 64)
        val routeComment = System.getenv("IP_ROUTE_NAME") ?: "WGMTEasy"
        val id = executeCommand("/ip/route/print where comment=\"$routeComment\"")
            .firstOrNull()
            ?.get(".id")
            ?: throw IllegalStateException("WireGuard routing entry was not found")
        executeCommand("/ip/route/set gateway=\"$interfaceName\" numbers=${requireRouterOsId(id)}")
    }

    fun setInterfaceStatus(wgInterface: WgInterface) {
        val interfaceName = requireSafeValue(wgInterface.name, "Interface name", 64)
        val action = if (wgInterface.disabled) "enable" else "disable"
        executeCommand("/interface/wireguard/$action numbers=\"$interfaceName\"")
    }

    fun deleteExternalInterface(wgInterface: WgInterface) {
        val interfaceName = requireSafeValue(wgInterface.name, "Interface name", 64)
        try {
            val id = executeCommand("/interface/wireguard/peers/print where interface=\"$interfaceName\"")
                .firstOrNull()
                ?.get(".id")
            if (id != null) {
                executeCommand("/interface/wireguard/peers/remove numbers=\"${requireRouterOsId(id)}\"")
            }
            executeCommand("/interface/wireguard/remove numbers=\"$interfaceName\"")
        } catch (exception: Exception) {
            log.error("Failed to delete interface {}: {}", wgInterface.name, exception.message, exception)
            throw exception
        }
    }

    fun createNewWgInterface(wgInterface: NewWireguardInterface) {
        validateNewInterface(wgInterface)
        val name = requireNotNull(wgInterface.name)
        executeCommand(
            command(
                "/interface/wireguard/add name=\"$name\"",
                "mtu=1400",
                "listen-port=${wgInterface.endpointPort}",
                "private-key=\"${wgInterface.privateKey}\"",
            ),
        )

        val presharedKey = wgInterface.presharedKey
            ?.takeIf(String::isNotBlank)
            ?.let { "preshared-key='$it'" }
        executeCommand(
            command(
                "/interface/wireguard/peers/add name=\"$name\"",
                "interface=\"$name\"",
                "public-key=\"${wgInterface.publicKey}\"",
                presharedKey,
                "endpoint-address=${wgInterface.endpoint}",
                "endpoint-port=${wgInterface.endpointPort}",
                "allowed-address=\"${wgInterface.allowedAddress}\"",
                "persistent-keepalive=20",
            ),
        )
        executeCommand(
            command(
                "/ip/address/add",
                "address=${wgInterface.ipAddress?.substringBefore('/')}/24",
                "interface=$name",
            ),
        )
    }

    private fun requireSettings(): MikroTikSettings =
        settings ?: throw IllegalStateException("MikroTik settings are not loaded")

    companion object {
        private val log = LoggerFactory.getLogger(MikroTikService::class.java)
        private val ROUTER_OS_ID = Regex("\\*{1,2}[A-Za-z0-9]+")
        private val UNSAFE_VALUE = Regex("[\\r\\n\"';]")

        @JvmStatic
        fun findPeerInAddressList(lists: List<AddressList>, address: String?): AddressList? =
            lists.find { it.address == address }

        @JvmStatic
        fun usesClientConfigApi(rows: List<Map<String, String>>): Boolean =
            rows.any { values ->
                values.containsKey("client-allowed-address") || values.containsKey("client-endpoint")
            }

        @JvmStatic
        fun isReservedPeer(values: Map<String, String>): Boolean =
            listOf(values["comment"], values["name"]).any { value ->
                value == "ExternalWG" || value == "InteriorWG"
            }

        @JvmStatic
        fun firstAvailableHostNumber(
            inputAddress: String,
            inputInterface: String,
            peers: List<Map<String, String>>,
        ): Int {
            val inputParts = inputAddress.substringBefore('/').split('.').mapNotNull(String::toIntOrNull)
            if (inputParts.size != 4 || inputParts.any { it !in 0..255 }) {
                throw IllegalStateException("Input WireGuard address is not configured correctly")
            }
            val prefix = inputParts.take(3)
            val routerHost = inputParts[3]
            val usedHosts = peers.asSequence()
                .filter { peer -> peer["interface"].isNullOrBlank() || peer["interface"] == inputInterface }
                .flatMap { peer -> peer["allowed-address"].orEmpty().split(',').asSequence() }
                .map { address -> address.trim().substringBefore('/').split('.') }
                .filter { parts -> parts.size == 4 && parts.take(3).mapNotNull(String::toIntOrNull) == prefix }
                .mapNotNull { parts -> parts[3].toIntOrNull() }
                .filter { host -> host in 1..254 }
                .toSet()

            return (1..254).firstOrNull { host -> host != routerHost && host !in usedHosts }
                ?: throw IllegalStateException("WireGuard subnet has no free host address")
        }

        private fun validateNewInterface(wgInterface: NewWireguardInterface) {
            wgInterface.name = requireSafeValue(wgInterface.name, "Interface name", 64)
            wgInterface.ipAddress = requireSafeValue(wgInterface.ipAddress, "IP address", 64)
            wgInterface.allowedAddress = requireSafeValue(wgInterface.allowedAddress, "Allowed address", 255)
            wgInterface.endpoint = requireSafeValue(wgInterface.endpoint, "Endpoint", 255)
            wgInterface.publicKey = requireSafeValue(wgInterface.publicKey, "Public key", 128)
            wgInterface.privateKey = requireSafeValue(wgInterface.privateKey, "Private key", 128)
            wgInterface.presharedKey = wgInterface.presharedKey
                ?.takeIf(String::isNotBlank)
                ?.let { requireSafeValue(it, "Preshared key", 128) }

            val port = wgInterface.endpointPort?.toIntOrNull()
                ?: throw IllegalArgumentException("Endpoint port must be a number")
            if (port !in 1..65_535) {
                throw IllegalArgumentException("Endpoint port must be between 1 and 65535")
            }
        }

        private fun requireSafeValue(value: String?, field: String, maxLength: Int): String {
            val normalized = value?.trim()
            if (normalized.isNullOrEmpty() || normalized.length > maxLength || UNSAFE_VALUE.containsMatchIn(normalized)) {
                throw IllegalArgumentException("$field contains unsupported characters")
            }
            return normalized
        }

        private fun requireRouterOsId(value: String?): String {
            if (value == null || !ROUTER_OS_ID.matches(value)) {
                throw IllegalArgumentException("Invalid RouterOS object id")
            }
            return value
        }

        private fun command(vararg parts: String?): String = parts.filterNotNull().joinToString(" ")
    }
}
