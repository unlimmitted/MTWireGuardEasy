package ru.unlimmitted.mtwgeasy.services

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import ru.unlimmitted.mtwgeasy.dto.MikroTikSettings
import ru.unlimmitted.mtwgeasy.dto.Peer
import ru.unlimmitted.mtwgeasy.dto.RenamePeerRequest

class MikroTikServiceTests {
    @Test
    fun reusesFirstFreeAddressBeforeCurrentMaximum() {
        val peers = listOf(
            mapOf("interface" to "WGMTEasyIn", "allowed-address" to "10.10.10.1/32"),
            mapOf("interface" to "WGMTEasyIn", "allowed-address" to "10.10.10.2/32"),
            mapOf("interface" to "WGMTEasyIn", "allowed-address" to "10.10.10.4/32"),
            mapOf("interface" to "OtherWG", "allowed-address" to "10.10.10.3/32"),
        )

        val host = MikroTikService.firstAvailableHostNumber("10.10.10.1/32", "WGMTEasyIn", peers)

        assertEquals(3, host)
    }

    @Test
    fun reportsFullWireGuardSubnet() {
        val peers = (2..254).map { host ->
            mapOf("interface" to "WGMTEasyIn", "allowed-address" to "10.10.10.$host/32")
        }

        assertThrows(IllegalStateException::class.java) {
            MikroTikService.firstAvailableHostNumber("10.10.10.1/32", "WGMTEasyIn", peers)
        }
    }

    @Test
    fun changesPeerStatusToRequestedValue() {
        val service = spy(MikroTikService())
        doAnswer { emptyList<Map<String, String>>() }
            .`when`(service).executeCommand(org.mockito.ArgumentMatchers.anyString())

        service.setPeerStatus(Peer(id = "*1", disabled = true))
        service.setPeerStatus(Peer(id = "*2", disabled = false))

        verify(service).executeCommand("/interface/wireguard/peers/disable numbers=*1")
        verify(service).executeCommand("/interface/wireguard/peers/enable numbers=*2")
    }

    @Test
    fun readsPeersThroughClientConfigApiOnNewRouterOs() {
        val service = spy(MikroTikService())
        ReflectionTestUtils.setField(
            service,
            "settings",
            MikroTikSettings(inputWgInterfaceName = "WGMTEasyIn"),
        )
        doAnswer { invocation ->
            when (invocation.getArgument<String>(0)) {
                "/ip/firewall/address-list/print" -> emptyList<Map<String, String>>()
                "/interface/wireguard/peers/print" -> listOf(
                    mapOf(
                        ".id" to "*1",
                        "name" to "phone",
                        "interface" to "WGMTEasyIn",
                        "allowed-address" to "10.10.10.2/32",
                        "client-allowed-address" to "0.0.0.0/0",
                        "private-key" to "",
                    ),
                )
                "/interface/wireguard/peers/show-client-config number=\"*1\" show-sensitive=yes" -> listOf(
                    mapOf("conf" to "[Interface]\nPrivateKey = restored-private-key\nAddress = 10.10.10.2/32\n"),
                )
                else -> emptyList<Map<String, String>>()
            }
        }.`when`(service).executeCommand(org.mockito.ArgumentMatchers.anyString())

        val peers = service.getPeers()

        assertEquals(1, peers.size)
        assertEquals("phone", peers.single().name)
        assertEquals("restored-private-key", peers.single().privateKey)
        verify(service).executeCommand(
            "/interface/wireguard/peers/show-client-config number=\"*1\" show-sensitive=yes",
        )
    }

    @Test
    fun keepsLegacyPrivateKeyMethodForOldRouterOs() {
        val service = spy(MikroTikService())
        doAnswer { invocation ->
            when (invocation.getArgument<String>(0)) {
                "/ip/firewall/address-list/print" -> emptyList<Map<String, String>>()
                "/interface/wireguard/peers/print" -> listOf(
                    mapOf(
                        ".id" to "*1",
                        "name" to "phone",
                        "private-key" to "legacy-private-key",
                        "allowed-address" to "10.10.10.2/32",
                    ),
                )
                else -> emptyList<Map<String, String>>()
            }
        }.`when`(service).executeCommand(org.mockito.ArgumentMatchers.anyString())

        val peers = service.getPeers()

        assertEquals("legacy-private-key", peers.single().privateKey)
        verify(service, never()).executeCommand(
            org.mockito.ArgumentMatchers.startsWith("/interface/wireguard/peers/show-client-config"),
        )
    }

    @Test
    fun renamesPeerAndMatchingAddressListComment() {
        val service = spy(MikroTikService())
        doAnswer { invocation ->
            when (invocation.getArgument<String>(0)) {
                "/ip/firewall/address-list/print" -> listOf(
                    mapOf(".id" to "*A", "address" to "10.10.10.2", "comment" to "old-name"),
                )
                "/interface/wireguard/peers/print" -> listOf(
                    mapOf(
                        ".id" to "*1",
                        "name" to "old-name",
                        "private-key" to "private",
                        "allowed-address" to "10.10.10.2/32",
                    ),
                )
                else -> emptyList<Map<String, String>>()
            }
        }.`when`(service).executeCommand(org.mockito.ArgumentMatchers.anyString())

        service.renamePeer(RenamePeerRequest(id = "*1", name = "new-name"))

        verify(service).executeCommand("/interface/wireguard/peers/set numbers=*1 name=\"new-name\"")
        verify(service).executeCommand("/ip/firewall/address-list/set numbers=*A comment=\"new-name\"")
    }

    @Test
    fun rejectsUnsafePeerNameBeforeCallingRouter() {
        val service = spy(MikroTikService())

        assertThrows(IllegalArgumentException::class.java) {
            service.renamePeer(RenamePeerRequest(id = "*1", name = "bad\nname"))
        }

        verify(service, never()).executeCommand(org.mockito.ArgumentMatchers.anyString())
    }

    @Test
    fun invertsDoubleVpnMangleRuleAndPersistsSetting() {
        val service = spy(MikroTikService())
        val commands = mutableListOf<String>()
        ReflectionTestUtils.setField(
            service,
            "settings",
            MikroTikSettings(
                inputWgInterfaceName = "WGMTEasyIn",
                toVpnAddressList = "WGMTEasyToVpnAddresses",
                toVpnTableName = "WGMTEasyToVpnTable",
                vpnChainMode = true,
            ),
        )
        doAnswer { invocation ->
            val command = invocation.getArgument<String>(0)
            commands += command
            when (command) {
                "/ip/firewall/mangle/print where new-routing-mark=\"WGMTEasyToVpnTable\"" -> listOf(
                    mapOf(".id" to "*B", "chain" to "prerouting", "in-interface" to "WGMTEasyIn"),
                )
                "/file/print where name=\"WGMTSettings.conf\"" -> listOf(mapOf(".id" to "*C"))
                else -> emptyList<Map<String, String>>()
            }
        }.`when`(service).executeCommand(org.mockito.ArgumentMatchers.anyString())

        val updated = service.setDoubleVpnInversion(true)

        verify(service).executeCommand(
            "/ip/firewall/mangle/set numbers=*B src-address-list=\"!WGMTEasyToVpnAddresses\"",
        )
        assertTrue(
            commands.any { command ->
                command.startsWith("/file/set numbers=*C contents='") &&
                    command.contains("\\\"doubleVpnInverted\\\":true")
            },
        )
        assertTrue(updated.doubleVpnInverted)
    }
}
