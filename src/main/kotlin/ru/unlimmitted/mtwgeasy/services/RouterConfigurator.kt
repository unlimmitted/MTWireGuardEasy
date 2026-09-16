package ru.unlimmitted.mtwgeasy.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import ru.unlimmitted.mtwgeasy.dto.MikroTikSettings

class RouterConfigurator(
    private val routerSettings: MikroTikSettings,
) : MikroTikExecutor() {
    private fun createBackup() {
        executeCommand("/system/backup/save name=\"before WGMTEASY\"")
    }

    private fun createInterfaces() {
        executeCommand(
            command(
                "/interface/wireguard/add",
                "name=\"${routerSettings.inputWgInterfaceName}\"",
                "mtu=1400",
                "listen-port=${routerSettings.inputWgEndpointPort}",
            ),
        )
        if (routerSettings.vpnChainMode) {
            executeCommand(
                command(
                    "/interface/wireguard/add name=\"${routerSettings.externalWgInterfaceName}\"",
                    "mtu=1400",
                    "listen-port=${routerSettings.endpointPort}",
                    "private-key=\"${routerSettings.externalWgPrivateKey}\"",
                ),
            )
        }
    }

    private fun createExternalPeer() {
        val presharedKey = routerSettings.externalWgPresharedKey
            ?.let { "preshared-key='$it'" }
        executeCommand(
            command(
                "/interface/wireguard/peers/add name=\"ExternalWG\"",
                "interface=\"${routerSettings.externalWgInterfaceName}\"",
                "public-key=\"${routerSettings.externalWgPublicKey}\"",
                presharedKey,
                "endpoint-address=${routerSettings.endpoint}",
                "endpoint-port=${routerSettings.endpointPort}",
                "allowed-address=\"${routerSettings.allowedAddress}\"",
                "persistent-keepalive=20",
            ),
        )
    }

    private fun createRoutingTable() {
        executeCommand("/routing/table/add disabled=no fib=True name=${routerSettings.toVpnTableName}")
    }

    private fun createIpRule() {
        var wgAddress = routerSettings.inputWgAddress.orEmpty().substringBefore('/')
        executeCommand(
            command(
                "/ip/address/add",
                "address=$wgAddress/24",
                "interface=${routerSettings.inputWgInterfaceName}",
            ),
        )
        if (routerSettings.vpnChainMode) {
            executeCommand(
                command(
                    "/ip/address/add",
                    "address=${routerSettings.ipAddress.orEmpty().substringBefore('/')}/24",
                    "interface=${routerSettings.externalWgInterfaceName}",
                ),
            )

            wgAddress += "/24"
            executeCommand(
                command(
                    "/ip/firewall/mangle/add",
                    "action=mark-routing",
                    "chain=prerouting",
                    "src-address=$wgAddress",
                    "dst-address=!$wgAddress",
                    "src-address-list=\"${routerSettings.toVpnAddressList}\"",
                    "in-interface=\"${routerSettings.inputWgInterfaceName}\"",
                    "new-routing-mark=\"${routerSettings.toVpnTableName}\"",
                    "passthrough=yes",
                ),
            )
            executeCommand(
                command(
                    "/ip/route/add",
                    "comment=\"WGMTEasy\"",
                    "distance=1",
                    "dst-address=0.0.0.0/0",
                    "gateway=${routerSettings.externalWgInterfaceName}",
                    "routing-table=${routerSettings.toVpnTableName}",
                    "scope=30",
                    "suppress-hw-offload=no",
                    "target-scope=10",
                ),
            )
        }
    }

    private fun saveSettings() {
        val json = jacksonObjectMapper().writeValueAsString(routerSettings).replace("\"", "\\\"")
        executeCommand("/file/add name=\"$SETTINGS_FILE\" contents='$json'")
        setSettings()
    }

    private fun createPortForwardRule() {
        executeCommand(
            command(
                "/ip/firewall/nat/add",
                "comment=\"WGMTEasyFWD\"",
                "action=dst-nat",
                "chain=dstnat",
                "protocol=udp",
                "dst-port=${routerSettings.inputWgEndpointPort}",
                "in-interface=${routerSettings.wanInterfaceName}",
                "to-addresses=$mikrotikGateway",
            ),
        )
        if (routerSettings.vpnChainMode) {
            executeCommand(
                command(
                    "/ip/firewall/nat/add",
                    "comment=\"WGMTEasyFWD\"",
                    "action=masquerade",
                    "chain=srcnat",
                    "out-interface=${routerSettings.externalWgInterfaceName}",
                ),
            )
        }
    }

    fun run() {
        try {
            createBackup()
            createInterfaces()
            if (routerSettings.vpnChainMode) createExternalPeer()
            setWgInterfaces()
            createRoutingTable()
            createIpRule()
            saveSettings()
            createPortForwardRule()
            setIsConfigured()
        } catch (exception: Exception) {
            throw RuntimeException("Configuration error: ${exception.message}", exception)
        }
    }

    private fun command(vararg parts: String?): String = parts.filterNotNull().joinToString(" ")
}
