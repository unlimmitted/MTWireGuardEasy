import json
import logging
import os
import time
import jwt
import pywgkey
import routeros_api
from controllers.configurator import Configurator
from cryptography.fernet import Fernet

SECRET_KEY = str(os.environ["SECRET_KEY"])
ALGORITHM = str(os.environ["ALGORITHM"])

mikrotik_settings = []
mikrotik_user = {}
response = {
    "wg_peers": [],
    "errors": [],
    "setting_status": False
}

fernet = Fernet(Fernet.generate_key())


class MikroTik:

    def __init__(self, token: str):
        self.token = token
        self.api = None
        self.data = None
        self.get_api()

    def get_api(self):
        try:
            key = jwt.decode(self.token, SECRET_KEY, algorithms=ALGORITHM)
            self.data = mikrotik_user[key['username']]
            connect = routeros_api.RouterOsApiPool(
                host=self.data["host"],
                username=self.data["username"],
                password=fernet.decrypt(self.data["password"]),
                plaintext_login=True)
            self.api = connect.get_api()
            return True
        except Exception as error:
            print(error)
            return "UNAUTHORIZED", 401

    def try_get_settings(self):
        try:
            content = self.api.get_resource('/file').get(name='WG-WebMode-Settings.txt')[0]
            json.loads(content['contents'].replace("'", '"'))
            return True
        except Exception as error:
            logging.info(error)
            response["setting_status"] = False
            response["errors"].append({"error": "Settings not found"})

    def get_id_address_list(self, client_id):
        addresses_to_vpn = self.api.get_resource("/ip/firewall/address-list/").get()
        peer = self.api.get_resource('/interface/wireguard/peers').get(id=client_id)
        id_list = ''
        for item in addresses_to_vpn:
            try:
                if item.get("comment") == peer[0].get('comment').split('\n')[0]:
                    id_list = item.get("id")
            except TypeError as error:
                logging.warning(error)
        return id_list

    @staticmethod
    def check_peer_comment(peers):
        for index, peer in enumerate(peers):
            if peer.get("comment") is None:
                peers.pop(index)
        return peers

    def get_free_ip(self):
        list_get = self.api.get_resource('/interface/wireguard/peers').get(interface="WG-WebMode-Easy-wg-in")
        peers = self.check_peer_comment(list_get)
        nums = [str(d['allowed-address']).split('.')[3] for d in peers]
        max_ip = max([int(str(el[0:el.index('/')])) for el in nums])
        local_wg_network = self.get_router_settings()[2]["localWgNetwork"]
        return local_wg_network.replace(
            local_wg_network.split('.')[-1], f'{max_ip + 1}' + "/" + local_wg_network.split('.')[-1].split('/')[-1])

    def list_peers(self, client_id=None):
        addresses_to_vpn = self.api.get_resource("/ip/firewall/address-list/")

        if client_id is not None:
            peers = self.api.get_resource("interface/wireguard/peers/")
            addresses_to_vpn.remove(id=self.get_id_address_list(client_id))
            peers.remove(id=client_id)

        peers = self.api.get_resource("interface/wireguard/peers/")
        clients_from_router = peers.get(interface='WG-WebMode-Easy-wg-in')

        peers = self.check_peer_comment(clients_from_router)

        for index, client in enumerate(peers):

            client["server-public-key"] = self.get_router_settings()[1]
            client["endpoint-address"] = mikrotik_settings[0]["localWgEndpoint"]
            client["endpoint-port"] = self.get_router_settings()[0]

            if len(addresses_to_vpn.get(comment=client.get("comment").split('\n')[0])) != 0:
                if not addresses_to_vpn.get(comment=client.get("comment").split('\n')[0])[0].get('disabled') == 'true':
                    client["double-vpn"] = True
                else:
                    client["double-vpn"] = False

        for index, client in enumerate(peers):
            if client.get("comment") == "service":
                peers.pop(index)

        response["wg_peers"] = peers
        return response

    def get_router_settings(self, port_forward_mode=None, configurate=None):
        content = self.api.get_resource('/file').get(name='WG-WebMode-Settings.txt')[0]
        settings = json.loads(content['contents'])
        if settings not in mikrotik_settings:
            mikrotik_settings.append(settings)
        response["setting_status"] = True
        if configurate is True:
            cfg = Configurator(settings, self.api, self.data["host"], port_forward_mode)
            cfg.run()
        settings["server-public-key"] = self.api.get_resource('/interface/wireguard').get(name="WG-WebMode-Easy-wg-in")[
            0].get(
            'public-key')
        return settings["localWgEndpointPort"], settings["server-public-key"], settings

    def create_new_peer(self, peer):
        get_duplicate = self.api.get_resource('/interface/wireguard/peers').get()
        peers = self.check_peer_comment(get_duplicate)
        for index, client in enumerate(peers):
            if peer.get("comment") == client.get("comment").split('\n')[0]:
                response["errors"].append({"error": "Peer already exists"})
                return response
        keys_pair = pywgkey.WgKey()
        client_ip = self.get_free_ip()
        wg_peers = self.api.get_resource('/interface/wireguard/peers')
        addresses_to_vpn = self.api.get_resource("/ip/firewall/address-list/")
        wg_peers.add(
            interface="WG-WebMode-Easy-wg-in",
            comment=f"{peer.get('comment')}\n {keys_pair.privkey}",
            public_key=keys_pair.pubkey,
            allowed_address=client_ip,
            persistent_keepalive='00:00:20')
        addresses_to_vpn.add(address=client_ip.split("/")[0], list='WG-WebMode-Easy-ToVpn-Addresses',
                             comment=peer.get("comment"))
        return self.list_peers()

    def change_vpn_route(self, peer_vpn):
        addresses_to_vpn = self.api.get_resource("/ip/firewall/address-list/")
        peer_vpn_status = peer_vpn.get("status")
        id_peer = peer_vpn.get("id")
        if peer_vpn_status:
            addresses_to_vpn.call('disable', {'numbers': self.get_id_address_list(id_peer)})
        else:
            addresses_to_vpn.call('enable', {'numbers': self.get_id_address_list(id_peer)})

    def set_settings(self, settings):
        port_forward_mode = settings.get("portForward")
        preshared_key = settings.get("presharedKey")
        del settings["portForward"], settings["presharedKey"]
        for index, value in enumerate(list(settings)):
            if list(settings)[index][-1] == "":
                response["errors"].append({"error": "Not all parameters are specified"})
                return response
        settings["presharedKey"] = preshared_key
        self.api.get_resource('/file').add(
            name='WG-WebMode-Settings.txt',
            contents=str(dict(settings)).replace("'", "\""))
        time.sleep(1)
        self.get_router_settings(port_forward_mode, True)
        return self.list_peers()


def authenticate_user(host: str, username: str, password: str):
    try:
        connect = routeros_api.RouterOsApiPool(
            host=host,
            username=username,
            password=password,
            plaintext_login=True)
        connect.get_api()
        encrypt_password = fernet.encrypt(bytes(password, 'UTF-8'))
        mikrotik_user[username] = {"host": host,
                                   "username": username,
                                   "password": encrypt_password
                                   }
        return username
    except routeros_api.exceptions.RouterOsApiCommunicationError:
        return False
    except routeros_api.exceptions.RouterOsApiConnectionError:
        return False
