package ru.unlimmitted.mtwgeasy.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.whispersystems.curve25519.Curve25519
import org.whispersystems.curve25519.Curve25519KeyPair
import ru.unlimmitted.mtwgeasy.dto.*

import java.util.regex.Matcher

@Service
class MikroTikService extends MikroTikExecutor {

    private static final Logger log = LoggerFactory.getLogger(MikroTikService.class)

    MikroTikService() {
        super()
    }

    void runConfigurator(MikroTikSettings settings) {
        new RouterConfigurator(settings).run()
        initializeConnection()
    }

    List<Peer> getPeers() {
        List<AddressList> lists = getAddressList()
        Map<String, AddressList> routingByAddress = lists
                .findAll { it.listName == settings?.toVpnAddressList }
                .findAll { it.address }
                .collectEntries { [(it.address): it] }

        return executeCommand("/interface/wireguard/peers/print").findAll {
            it != null &&
            it.get("private-key") &&
            it.comment != "ExternalWG" &&
            it.comment != "InteriorWG"
        }.collect { Map<String, String> it ->
            Peer peer = new Peer()
            peer.id = it.get(".id")
            peer.allowedAddress = it.get("allowed-address")
            peer.tx = it.get("tx")
            peer.lastHandshake = it.get("last-handshake")
            peer.rx = it.get("rx")
            peer.privateKey = it.get('private-key')
            peer.name = it.get("name")
            peer.currentEndpointPort = it.get("current-endpoint-port")
            peer.currentEndpointAddress = it.get("current-endpoint-address")
            peer.publicKey = findInterface(it.get("interface"))?.publicKey
            peer.peerInterface = it.get("interface")
            peer.presharedKey = it.get("preshared-key")
            peer.endpoint = settings.endpoint

            WgInterface wgInterface = findInterface(it.get("interface"))
            if (wgInterface != null) {
                peer.endpointPort = wgInterface.listenPort
            }

            String[] allowedAddressParts = it.get("allowed-address").split("/")
            if (allowedAddressParts.length > 0) {
                AddressList addressEntry = routingByAddress.get(allowedAddressParts.first())
                if (addressEntry != null) {
                    peer.doubleVpn = !addressEntry.disabled
                }
            }
            return peer
        }
    }

    List<EtherInterface> getEtherInterfaces() {
        List<EtherInterface> interfaces = new ArrayList<>()
        executeCommand("/interface/print where type=\"ether\"").forEach {
            EtherInterface etherInterface = new EtherInterface()
            etherInterface.id = it.get(".id")
            etherInterface.name = it.get("name")
            etherInterface.macAddress = it.get("mac-address")
            etherInterface.network = executeCommand(
                    "/ip/address/print where interface=\"${it.get("name")}\""
            ).find()?.get("network")
            interfaces.add(etherInterface)
        }
        return interfaces
    }

    WgInterface findInterface(String interfaceName) {
        return wgInterfaces?.find { it.name == interfaceName }
    }

    List<AddressList> getAddressList() {
        List<AddressList> addressListList = new ArrayList<>()
        executeCommand('/ip/firewall/address-list/print').forEach({
            AddressList addressList = new AddressList()
            addressList.id = it.get('.id')
            addressList.disabled = it.get('disabled') != null ? it.get('disabled').toBoolean() : false
            addressList.comment = it.get('comment')
            addressList.listName = it.get('list')
            addressList.address = it.get('address')
            addressListList.add(addressList)
        })
        return addressListList
    }

    static AddressList findPeerInAddressList(List<AddressList> lists, String address) {
        return lists.find { it.address == address }
    }

    MikroTikInfo getMikroTikInfo() {
        MikroTikInfo mtInfo = new MikroTikInfo()
        mtInfo.interfaces = wgInterfaces ?: []
        try {
            Map<String, String> resource = executeCommand("/system/resource/print").find() ?: [:]
            mtInfo.routerBoard = resource.get('board-name') ?: resource.get('platform') ?: "<undefined>"
            mtInfo.version = resource.get('version') ?: "<undefined>"
        } catch (Exception e) {
            mtInfo.routerBoard = "<undefined>"
            mtInfo.version = "<undefined>"
            log.warn("Failed to get MikroTik system info: {}", e.message)
        }
        return mtInfo
    }

    void createNewPeer(String peerName) {
        peerName = requireSafeValue(peerName, "Peer name", 64)
        Curve25519KeyPair keyPair = Curve25519.getInstance(Curve25519.JAVA).generateKeyPair()
        String pri = Base64.getEncoder().encodeToString(keyPair.getPrivateKey())
        String regex = /^(\d{1,3})\.(\d{1,3})\.(\d{1,3})/
        String inputAddress = settings?.inputWgAddress
        Matcher matcher = (inputAddress ?: "") =~ regex
        if (!matcher.find()) {
            throw new IllegalStateException("Input WireGuard address is not configured correctly")
        }
        String ip = "${matcher.group(1)}.${matcher.group(2)}.${matcher.group(3)}.${getHostNumber()}"
        String peerQueryParams = """
            |/interface/wireguard/peers/add
            |interface="${settings.inputWgInterfaceName}"
            |private-key="$pri"
            |allowed-address=$ip/32
            |name="${peerName}"
            """.stripMargin().replace("\n", " ")

        executeCommand("$peerQueryParams")
        String addressListQueryParam = "address=$ip list=${settings.toVpnAddressList} comment=$peerName"
        executeCommand("/ip/firewall/address-list/add $addressListQueryParam")
    }

    void changeRouting(Peer peer) {
        List<AddressList> lists = getAddressList()
        AddressList addressEntry = findPeerInAddressList(lists, peer.allowedAddress?.split('/')?.first())
        if (addressEntry == null) {
            throw new IllegalArgumentException("Peer routing entry was not found")
        }
        String listId = requireRouterOsId(addressEntry.id)
        String queryParam = "${peer.doubleVpn ? 'disable' : 'enable'} numbers=$listId"
        executeCommand("/ip/firewall/address-list/$queryParam")
    }

    void removePeer(Peer peer) {
        String peerId = requireRouterOsId(peer.id)
        List<AddressList> lists = getAddressList()
        AddressList addressEntry = findPeerInAddressList(lists, peer.allowedAddress?.split('/')?.first())
        if (addressEntry == null) {
            throw new IllegalArgumentException("Peer routing entry was not found")
        }
        String addressListId = requireRouterOsId(addressEntry.id)
        executeCommand("/interface/wireguard/peers/remove numbers=$peerId")
        executeCommand("/ip/firewall/address-list/remove numbers=$addressListId")
    }

    void changeVpnRouting(WgInterface wgInterface) {
        String interfaceName = requireSafeValue(wgInterface.name, "Interface name", 64)
        String routeComment = System.getenv("IP_ROUTE_NAME") ?: "WGMTEasy"
        String id = executeCommand("/ip/route/print where comment=\"${routeComment}\"").find()?.get(".id")
        if (id == null) {
            throw new IllegalStateException("WireGuard routing entry was not found")
        }
        executeCommand("/ip/route/set gateway=\"${interfaceName}\" numbers=${requireRouterOsId(id)}")
    }

    void setInterfaceStatus(WgInterface wgInterface) {
        String interfaceName = requireSafeValue(wgInterface.name, "Interface name", 64)
        String query = "/interface/wireguard/${wgInterface.disabled ? 'enable' : 'disable'} numbers=\"${interfaceName}\""
        executeCommand(query)
    }

    void deleteExternalInterface(WgInterface wgInterface) {
        String interfaceName = requireSafeValue(wgInterface.name, "Interface name", 64)
        try {
            String id = executeCommand(
                    "/interface/wireguard/peers/print where interface=\"${interfaceName}\""
            ).find()?.get(".id")
            if (id != null) {
                executeCommand("/interface/wireguard/peers/remove numbers=\"${requireRouterOsId(id)}\"")
            }
            executeCommand("/interface/wireguard/remove numbers=\"${interfaceName}\"")
        } catch (Exception ex) {
            log.error("Failed to delete interface {}: {}", wgInterface.name, ex.message, ex)
            throw ex
        }
    }

    void createNewWgInterface(NewWireguardInterface wgInterface) {
        validateNewInterface(wgInterface)
        String interfaceQuery = """
            |/interface/wireguard/add name="${wgInterface.name}"
            |mtu=1400
            |listen-port=${wgInterface.endpointPort}
            |private-key="${wgInterface.privateKey}"
            """.stripMargin().replace("\n", " ")
        executeCommand(interfaceQuery)

        String peerQuery = """
            |/interface/wireguard/peers/add name="${wgInterface.name}"
            |interface="${wgInterface.name}"
            |public-key="${wgInterface.publicKey}"
            |${wgInterface.presharedKey != null ? "preshared-key='${wgInterface.presharedKey}'" : ''}
            |endpoint-address=${wgInterface.endpoint}
            |endpoint-port=${wgInterface.endpointPort} allowed-address="${wgInterface.allowedAddress}"
            |persistent-keepalive=20
            """.stripMargin().replace("\n", " ")
        executeCommand(peerQuery)

        String ipAddressQuery = """
            |/ip/address/add
            |address=${wgInterface.ipAddress.split("/").first()}/24
            |interface=${wgInterface.name}
            """.stripMargin().replace("\n", " ")
        executeCommand(ipAddressQuery)
    }

    private static void validateNewInterface(NewWireguardInterface wgInterface) {
        wgInterface.name = requireSafeValue(wgInterface.name, "Interface name", 64)
        wgInterface.ipAddress = requireSafeValue(wgInterface.ipAddress, "IP address", 64)
        wgInterface.allowedAddress = requireSafeValue(wgInterface.allowedAddress, "Allowed address", 255)
        wgInterface.endpoint = requireSafeValue(wgInterface.endpoint, "Endpoint", 255)
        wgInterface.publicKey = requireSafeValue(wgInterface.publicKey, "Public key", 128)
        wgInterface.privateKey = requireSafeValue(wgInterface.privateKey, "Private key", 128)
        if (wgInterface.presharedKey) {
            wgInterface.presharedKey = requireSafeValue(wgInterface.presharedKey, "Preshared key", 128)
        }
        int port
        try {
            port = wgInterface.endpointPort?.toInteger()
        } catch (Exception ignored) {
            throw new IllegalArgumentException("Endpoint port must be a number")
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Endpoint port must be between 1 and 65535")
        }
    }

    private static String requireSafeValue(String value, String field, int maxLength) {
        String normalized = value?.trim()
        if (!normalized || normalized.length() > maxLength || normalized.find(/[\r\n"';]/)) {
            throw new IllegalArgumentException("${field} contains unsupported characters")
        }
        return normalized
    }

    private static String requireRouterOsId(String value) {
        if (value == null || !(value ==~ /\*?[0-9A-Fa-f]+/)) {
            throw new IllegalArgumentException("Invalid RouterOS object id")
        }
        return value
    }
}
