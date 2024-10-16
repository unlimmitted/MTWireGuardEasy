package ru.unlimmitted.mtwgeasy.services


import org.springframework.stereotype.Service
import org.whispersystems.curve25519.Curve25519
import org.whispersystems.curve25519.Curve25519KeyPair
import ru.unlimmitted.mtwgeasy.dto.*

import java.util.regex.Matcher

@Service
class MikroTikService extends MikroTikExecutor {

	MikroTikService() {
		super()
	}

	static void runConfigurator(MikroTikSettings settings) {
		new RouterConfigurator(settings).run()
	}

	List<Peer> getPeers() {
		List<AddressList> lists = getAddressList()

		List<Peer> peers = executeCommand("/interface/wireguard/peers/print").findAll {
			it != null
			!it.get("private-key").isEmpty() && it.comment != "ExternalWG" && it.comment != "InteriorWG"
		}.collect {
			Map<String, String> it ->
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
				peer.publicKey = findInterface(it.get("interface")).publicKey
				peer.peerInterface = it.get("interface")
				peer.presharedKey = it.get("preshared-key")

				peer.endpoint = settings.endpoint
				WgInterface wgInterface = findInterface(it.get("interface"))
				if (wgInterface != null) {
					peer.endpointPort = wgInterface.listenPort
				}

				String[] allowedAddressParts = it.get("allowed-address").split("/")
				if (allowedAddressParts.length > 0) {
					AddressList addressEntry = findPeerInAddressList(lists, allowedAddressParts.first())
					if (addressEntry != null) {
						peer.doubleVpn = !addressEntry.disabled
					}
				}
				return peer
		}

		return peers
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
			).network.first
			interfaces.add(etherInterface)
		}
		return interfaces
	}

	WgInterface findInterface(String interfaceName) {
		return wgInterfaces.find({
			it.name == interfaceName
		})
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
		return lists.find({
			it.address == address
		})
	}

	MikroTikInfo getMikroTikInfo() {
		MikroTikInfo mtInfo = new MikroTikInfo()
		if (wgInterfaces == null) {
			setWgInterfaces()
		}
		mtInfo.interfaces = wgInterfaces
		try {
			executeCommand("/system/routerboard/print").forEach {
				mtInfo.routerBoard = it.get('board-name')
				mtInfo.version = it.get('upgrade-firmware')
			}
		} catch (Exception e) {
			if (e.message.contains("no such command prefix")) {
				mtInfo.routerBoard = "<undefined>"
				mtInfo.version = "<undefined>"
			}
		}
		return mtInfo
	}

	void createNewPeer(String peerName) {
		Curve25519KeyPair keyPair = Curve25519.getInstance(Curve25519.JAVA).generateKeyPair()
		String pri = Base64.getEncoder().encodeToString(keyPair.getPrivateKey())
		String regex = /^(\d{1,3})\.(\d{1,3})\.(\d{1,3})/
		Matcher matcher = (settings.inputWgAddress =~ regex)
		String ip = "${matcher[0][1..3].join(".")}.${getHostNumber()}"
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
		String listId = findPeerInAddressList(lists, peer.allowedAddress.split('/').first()).id
		String queryParam = "${peer.doubleVpn ? 'disable' : 'enable'} numbers=$listId"
		executeCommand("/ip/firewall/address-list/$queryParam")
	}

	void removePeer(Peer peer) {
		String peerId = peer.id
		List<AddressList> lists = getAddressList()
		String addressListId = findPeerInAddressList(lists, peer.allowedAddress.split('/').first()).id
		executeCommand("/interface/wireguard/peers/remove numbers=$peerId")
		executeCommand("/ip/firewall/address-list/remove numbers=$addressListId")
	}

	void changeVpnRouting(WgInterface wgInterface) {
		String id = executeCommand('/ip/route/print where comment="WGMTEasy"')[".id"].first()
		executeCommand("/ip/route/set gateway=\"${wgInterface.name}\" numbers=${id}")
	}

	void setInterfaceStatus(WgInterface wgInterface) {
		String query = "/interface/wireguard/${wgInterface.disabled ? 'enable' : 'disable'} numbers=\"${wgInterface.name}\""
		executeCommand(query)
	}

	void deleteExternalInterface(WgInterface wgInterface) {
		try {
			String id = executeCommand(
					"/interface/wireguard/peers/print where interface=\"${wgInterface.name}\""
			)[".id"].first
			executeCommand("/interface/wireguard/peers/remove numbers=\"${id}\"")
			executeCommand("/interface/wireguard/remove numbers=\"${wgInterface.name}\"")
		} catch (Exception ex) {
			System.out.println("Fail delete peer: $ex")
		}
	}

	void createNewWgInterface(NewWireguardInterface wgInterface) {
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
			|${wgInterface.presharedKey !== null ? "preshared-key='${wgInterface.presharedKey}'" : ''}
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
}
