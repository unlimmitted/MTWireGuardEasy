package ru.unlimmitted.mtwgeasy.services

import com.fasterxml.jackson.databind.ObjectMapper
import me.legrange.mikrotik.ApiConnection
import org.springframework.stereotype.Service
import ru.unlimmitted.mtwgeasy.dto.*

import java.util.regex.Matcher
import java.util.regex.Pattern

@Service
class MikroTikService {

	ApiConnection connect
	MtSettings settings

	void connectToMikroTik() {
		try {
			connect = ApiConnection.connect(System.getenv("GATEWAY"))
			connect.login(System.getenv("MIKROTIK_USER"), System.getenv("MIKROTIK_PASSWORD"))
			readSettings()
		} catch (exception) {
			println(exception)
		}
	}

	List<Peer> getPeers() {
		List<Peer> peers = new ArrayList<>()

		connect.execute("/interface/wireguard/peers/print").forEach((Map<String, String> it) -> {
			String comment = it.get("comment")
			if (comment != null && comment != "dont touch" && comment != "service") {
				Peer peer = new Peer()

				peer.setId(it.get(".id"))
				peer.setAllowedAddress(it.get("allowed-address"))
				peer.setTx(it.get("tx"))
				peer.setLastHandshake(it.get("last-handshake"))
				peer.setRx(it.get("rx"))

				String[] commentParts = comment.split("\n")
				if (commentParts.length > 1) {
					peer.setPrivateKey(commentParts[commentParts.length - 1].replace(" ", ""))
					peer.setName(commentParts[0])
					peer.setComment(commentParts[0])
				}

				peer.setCurrentEndpointPort(it.get("current-endpoint-port"))
				peer.setCurrentEndpointAddress(it.get("current-endpoint-address"))
				peer.setPublicKey(it.get("public-key"))
				peer.setPeerInterface(it.get("interface"))
				peer.setPresharedKey(it.get("preshared-key"))

				peer.setEndpoint(settings.getEndpoint())
				WgInterface wgInterface = findInterface(it.get("interface"))
				if (wgInterface != null) {
					peer.setEndpointPort(wgInterface.getListenPort())
				}

				String[] allowedAddressParts = it.get("allowed-address").split("/")
				if (allowedAddressParts.length > 0) {
					AddressList addressEntry = findPeerInAddressList(allowedAddressParts[0])
					if (addressEntry != null) {
						peer.setDoubleVpn(!addressEntry.disabled)
					}
				}

				peers.add(peer)
			}
		})

		return peers
	}

	List<WgInterface> getInterfaces() {
		List<WgInterface> wgInterfaces = new ArrayList<>()
		connect.execute('/interface/wireguard/print').forEach({
			WgInterface wgInterface = new WgInterface()
			wgInterface.setName(it.get('name'))
			wgInterface.setRunning(it.get('running').toBoolean())
			wgInterface.setPrivateKey(it.get('private-key'))
			wgInterface.setPublicKey(it.get('public-key'))
			wgInterface.setDisabled(it.get('disabled').toBoolean())
			wgInterface.setListenPort(it.get('listen-port'))
			wgInterface.setMtu(it.get('mtu'))
			wgInterfaces.add(wgInterface)
		})
		return wgInterfaces
	}

	WgInterface findInterface(String interfaceName) {
		return getInterfaces().find({
			it.name == interfaceName
		})
	}

	List<AddressList> getAddressList() {
		List<AddressList> addressListList = new ArrayList<>()
		connect.execute('/ip/firewall/address-list/print').forEach({
			AddressList addressList = new AddressList()
			addressList.setId(it.get('.id'))
			addressList.setDisabled(it.get('disabled').toBoolean())
			addressList.setComment(it.get('comment'))
			addressList.setListName(it.get('list'))
			addressList.setAddress(it.get('address'))
			addressListList.add(addressList)
		})
		return addressListList
	}

	AddressList findPeerInAddressList(String address) {
		return getAddressList().find({
			it.address == address
		})
	}

	MtSettings readSettings() {
		ObjectMapper objectMapper = new ObjectMapper()
		settings = objectMapper.readValue(
				connect.execute('/file/print').find {
					it.name == 'WG-WebMode-Settings.conf'
				}.contents,
				MtSettings.class
		)
		return settings
	}

	Integer getNewIp() {
		List<Integer> results = new ArrayList<>()
		connect.execute("/interface/wireguard/peers/print").forEach {
			String regex = "(?:\\d+\\.){3}(\\d{1,3})\\/\\d+"
			Pattern pattern = Pattern.compile(regex)
			Matcher matcher = pattern.matcher(it.get('allowed-address'))
			if (matcher.find()) {
				results.add(matcher.group(1).toInteger())
			}
		}
		return results.max() + 1
	}

	MtInfo getMtInfo() {
		MtInfo mtInfo = new MtInfo()
		connect.execute('/system/routerboard/print').forEach {
			mtInfo.routerBoard = it.get('board-name')
			mtInfo.version = it.get('upgrade-firmware')
			mtInfo.interfaces = getInterfaces()
		}
		return mtInfo
	}

	void createNewPeer (String peerName) {
		WireGuardKeyGen wireGuardKeyGen = new WireGuardKeyGen()
		KeyPair keyPair = wireGuardKeyGen.keyPair()
		String ip = "10.10.10.${getNewIp()}"
		String peerQueryParams = "interface=${settings.localWgInterfaceName} comment=\"${keyPair.privateKey}\"" +
				" public-key=\"${keyPair.publicKey}\" allowed-address=$ip/32" +
				" persistent-keepalive='00:00:20' name=${peerName}"
		connect.execute("/interface/wireguard/peers/add $peerQueryParams")
		String addressListQueryParam = "address=$ip list=${settings.toVpnAddressList} comment=$peerName"
		connect.execute("/ip/firewall/address-list/add $addressListQueryParam")
	}

	void changeRouting(Peer peer) {
		String listId = findPeerInAddressList(peer.allowedAddress.split('/').first()).id
		String queryParam = "${peer.doubleVpn ? 'disable' : 'enable'} numbers=$listId"
		connect.execute("/ip/firewall/address-list/$queryParam")
	}

	void removePeer(Peer peer) {
		String peerId = peer.id
		String addressListId = findPeerInAddressList(peer.allowedAddress.split('/').first()).id
		connect.execute("/interface/wireguard/peers/remove numbers=$peerId")
		connect.execute("/ip/firewall/address-list/remove numbers=$addressListId")
	}

	MikroTikService() {
		this.connectToMikroTik()
	}
}
