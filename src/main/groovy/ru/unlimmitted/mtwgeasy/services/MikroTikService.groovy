package ru.unlimmitted.mtwgeasy.services

import com.fasterxml.jackson.databind.ObjectMapper
import me.legrange.mikrotik.ApiConnection
import org.springframework.stereotype.Service
import ru.unlimmitted.mtwgeasy.dto.*

@Service
class MikroTikService extends MikroTikExecutor {

	ApiConnection connect
	MtSettings settings
	List<WgInterface> wgInterfaces

	MikroTikService() {
		super()
		connect = super.connect
		wgInterfaces = super.wgInterfaces
		settings = readSettings()
	}

	MtSettings readSettings() {
		ObjectMapper objectMapper = new ObjectMapper()
		if (isSettings()) {
			return objectMapper.readValue(
					executeCommand('/file/print').find {
						it.name == 'WGMTSettings.conf'
					}.contents.replace("\\\"", "\""),
					MtSettings.class
			)
		} else {
			MtSettings mtSettings = new MtSettings()
			return mtSettings
		}
	}

	Boolean isSettings() {
		Map<String, String> result = executeCommand('/file/print').find {
			it.name == 'WGMTSettings.conf'
		}
		return result
	}

	static void runConfigurator(MtSettings settings) {
		RouterConfigurator routerConfigurator = new RouterConfigurator(settings)
		routerConfigurator.run()
	}

	List<Peer> getPeers() {
		List<AddressList> lists = getAddressList()

		List<Peer> peers = executeCommand("/interface/wireguard/peers/print").findAll {
			it != null
			it.comment != null && it.comment != "ExternalWG" && it.comment != "InteriorWG"
		}.collect {
			Map<String, String> it ->
				Peer peer = new Peer()

				peer.id = it.get(".id")
				peer.allowedAddress = it.get("allowed-address")
				peer.tx = it.get("tx")
				peer.lastHandshake = it.get("last-handshake")
				peer.rx = it.get("rx")

				peer.privateKey = it.get('comment')
				peer.name = it.get("name")

				peer.currentEndpointPort = it.get("current-endpoint-port")
				peer.currentEndpointAddress = it.get("current-endpoint-address")
				peer.publicKey = findInterface(it.get("interface")).publicKey
				peer.peerInterface = it.get("interface")
				peer.presharedKey = it.get("preshared-key")

				peer.endpoint = settings.getEndpoint()
				WgInterface wgInterface = findInterface(it.get("interface"))
				if (wgInterface != null) {
					peer.endpointPort = wgInterface.getListenPort()
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
			addressList.disabled = it.get('disabled').toBoolean()
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

	MtInfo getMtInfo() {
		MtInfo mtInfo = new MtInfo()
		executeCommand('/system/routerboard/print').forEach {
			mtInfo.routerBoard = it.get('board-name')
			mtInfo.version = it.get('upgrade-firmware')
			mtInfo.interfaces = wgInterfaces
		}
		return mtInfo
	}

	void createNewPeer(String peerName) {
		WireGuardKeyGen wireGuardKeyGen = new WireGuardKeyGen()
		KeyPair keyPair = wireGuardKeyGen.keyPair()
		String ip = "10.10.10.${getHostNumber()}"
		String peerQueryParams = "interface=${settings.localWgInterfaceName} comment=\"${keyPair.privateKey}\"" +
				" public-key=\"${keyPair.publicKey}\" allowed-address=$ip/32" +
				" persistent-keepalive='00:00:20' name=${peerName}"
		executeCommand("/interface/wireguard/peers/add $peerQueryParams")
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

}
