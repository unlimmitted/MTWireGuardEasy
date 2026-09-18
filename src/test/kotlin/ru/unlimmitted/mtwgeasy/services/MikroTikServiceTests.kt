package ru.unlimmitted.mtwgeasy.services

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import ru.unlimmitted.mtwgeasy.dto.MikroTikSettings
import ru.unlimmitted.mtwgeasy.dto.RenamePeerRequest

class MikroTikServiceTests {
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
