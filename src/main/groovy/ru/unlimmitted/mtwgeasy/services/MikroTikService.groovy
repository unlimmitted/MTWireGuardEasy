package ru.unlimmitted.mtwgeasy.services

import com.fasterxml.jackson.databind.ObjectMapper
import me.legrange.mikrotik.ApiConnection
import org.springframework.stereotype.Service
import org.whispersystems.curve25519.Curve25519
import org.whispersystems.curve25519.Curve25519KeyPair
import ru.unlimmitted.mtwgeasy.dto.*

import java.time.Instant
import java.util.regex.Matcher

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
		return executeCommand('/file/print').find { it.name == 'WGMTSettings.conf' }
	}

	static void runConfigurator(MtSettings settings) {
		RouterConfigurator routerConfigurator = new RouterConfigurator(settings)
		routerConfigurator.run()
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

	void saveInterfaceTraffic() {
		final String fileName = "traffic_rate.txt"
		String json = executeCommand('/file/print')
				.find { it.name == fileName }?.contents
				?: "[]"
		if (json != "[]") {
			Integer number = executeCommand("/file/print")
					.indexed()
					.find { index, it -> it.name == fileName }
					.key
			executeCommand("/file/remove numbers=$number")
		}
		Long sumOfTx = getMtInfo().interfaces.sum { it.txByte.toLong() } as Long
		Long sumOfRx = getMtInfo().interfaces.sum { it.rxByte.toLong() } as Long
		TrafficRate rate = new TrafficRate(sumOfTx, sumOfRx, Instant.now())
		ObjectMapper mapper = new ObjectMapper()
		List<TrafficRate> rates = mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, TrafficRate.class))
		rates.add(rate)
		json = mapper.writeValueAsString(rates)
		executeCommand("/file/add name=\"$fileName\" contents='${json}'")
	}

}
