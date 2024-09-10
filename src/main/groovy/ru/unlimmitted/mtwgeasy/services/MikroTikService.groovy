package ru.unlimmitted.mtwgeasy.services

import me.legrange.mikrotik.ApiConnection
import org.springframework.stereotype.Service
import ru.unlimmitted.mtwgeasy.dto.AddressList
import ru.unlimmitted.mtwgeasy.dto.Peer
import ru.unlimmitted.mtwgeasy.dto.WgInterface

@Service
class MikroTikService {

	private ApiConnection connect

	void connectToMikroTik() {
		try {
			connect = ApiConnection.connect(System.getenv("GATEWAY"))
			connect.login(System.getenv("MIKROTIK_USER"), System.getenv("MIKROTIK_PASSWORD"))
		} catch (exception) {
			println(exception)
		}
	}

	List<Peer> getPeers() {
		List<Peer> peers = new ArrayList<>()
		connect.execute('/interface/wireguard/peers/print').forEach({
			Peer peer = new Peer()
			if (it['comment'] != 'dont touch' && it['comment'] != 'service') {
				peer.allowedAddress = it['allowed-address']
				peer.tx = it['tx']
				peer.lastHandshake = it['last-handshake']
				peer.rx = it['rx']
				peer.privateKey = it['comment'].split('\n')[-1].replace(' ', '')
				peer.currentEndpointPort = it['current-endpoint-port']
				peer.currentEndpointAddress = it['current-endpoint-address']
				peer.publicKey = it['public-key']
				peer.peerInterface = it['interface']
				peer.allowedAddress = it['allowed-address']
				peer.presharedKey = it['preshared-key']
				peer.comment = it['comment'].split('\n')[0]
				peer.name = it['comment'].split('\n')[0]
//				peer.endpoint = Из настроек придется брать
				peer.endpointPort = findInterface(it['interface']).listenPort
				peer.doubleVpn = !findPeerInAddressList(it['allowed-address'].split('/')[0]).disabled
				peers.add(peer)
			}
		})
		return peers
	}

	List<WgInterface> getInterfaces() {
		List<WgInterface> wgInterfaces = new ArrayList<>()
		connect.execute('/interface/wireguard/print').forEach({
			WgInterface wgInterface = new WgInterface()
			wgInterface.name = it['name']
			wgInterface.running = it['running'].toBoolean()
			wgInterface.privateKey = it['private-key']
			wgInterface.publicKey = it['public-key']
			wgInterface.disabled = it['disabled'].toBoolean()
			wgInterface.listenPort = it['listen-port']
			wgInterface.mtu = it['mtu']
			wgInterfaces.add(wgInterface)
		})
		return wgInterfaces
	}

	WgInterface findInterface(String interfaceName) {
		return getInterfaces().find({
			it.name = interfaceName
		})
	}

	List<AddressList> getAddressList() {
		List<AddressList> addressListList = new ArrayList<>()
		connect.execute('/ip/firewall/address-list/print').forEach({
			AddressList addressList = new AddressList()
			addressList.disabled = it['disabled'].toBoolean()
			addressList.comment = it['comment']
			addressList.listName = it['list']
			addressList.address = it['address']
			addressListList.add(addressList)
		})
		return addressListList
	}

	AddressList findPeerInAddressList(String address) {
		return getAddressList().find({
			it['address'] = address
		})
	}

	MikroTikService() {
		this.connectToMikroTik()
	}
}
