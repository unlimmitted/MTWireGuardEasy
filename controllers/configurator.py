import pywgkey
from datetime import datetime


class Configurator:

    def __init__(self, settings, api, mikrotik_host, port_forward_mode=False):
        self.service_client_key = pywgkey.WgKey()
        self.token = settings["token"]
        self.api = api

        self.host = mikrotik_host

        # Server Interface properties
        self.server_interface_name = "WG-WebMode-Easy-wg-in"
        self.server_network = settings["localWgNetwork"]
        self.server_listen_port = settings["localWgEndpointPort"]

        # Client Interface properties
        self.client_interface_name = "WG-WebMode-Easy-wg-out"
        self.preshared_key = ''
        if settings["presharedKey"] != "":
            self.preshared_key = settings["presharedKey"]
        self.client_private_key = settings["privateKey"]

        # Client Peer properties
        self.client_public_key = settings["publicKey"]
        self.endpoint = settings["endpoint"]
        self.endpoint_port = settings["endpointPort"]
        self.persistent_keep_alive = '00:00:20'

        # Other client properties
        self.client_allowed_address = settings["allowedAddress"]
        self.client_address = settings["ipAddress"]

        # Other settings
        self.wan_interface_name = settings["wanInterfaceName"]
        self.mtu = '1400'
        self.to_vpn_table_name = 'WG-WebMode-Easy-toVpnTable'
        self.local_network = settings["localNetwork"]
        max_num = int(self.server_network.split("/")[0].split(".")[-1]) + 1
        self.service_peer_address = self.server_network.replace(
            self.server_network.split('.')[-1],
            f'{max_num}' + "/" + self.server_network.split('.')[-1].split('/')[-1])

        self.port_forward_mode = port_forward_mode

    @staticmethod
    def format_network(network):
        format_net = network.replace(network.split('.')[-1], str(int(network.split('.')[-1].split('/')[0]) + 1))
        net_netmask = format_net + "/" + network.split('/')[-1]
        return format_net, net_netmask

    def __create_backup(self):
        system = self.api.get_resource('/system/')
        system.call("backup/save", {"name": f"WGMTEasy({datetime.now().date()})"})

    def __create_interfaces(self):
        interfaces = self.api.get_resource('/interface/wireguard')
        interfaces.add(
            name=self.server_interface_name,
            mtu=self.mtu,
            listen_port=self.server_listen_port)

        interfaces.add(
            name=self.client_interface_name,
            mtu=self.mtu,
            private_key=self.client_private_key)

    def __create_client_peer(self):
        client_peer = self.api.get_resource('/interface/wireguard/peers')
        client_peer.add(
            comment='dont touch',
            interface=self.client_interface_name,
            public_key=self.client_public_key,
            preshared_key=self.preshared_key,
            endpoint_address=self.endpoint,
            endpoint_port=self.endpoint_port,
            allowed_address=self.client_allowed_address,
            persistent_keepalive=self.persistent_keep_alive
        )

    def __create_service_server_peer(self):
        server_peer = self.api.get_resource('/interface/wireguard/peers')

        server_peer.add(
            interface=self.server_interface_name,
            public_key=self.service_client_key.pubkey,
            allowed_address=self.service_peer_address,
            comment='service',
            persistent_keepalive=self.persistent_keep_alive
        )

    def __create_routing_table(self):
        routing = self.api.get_resource('/routing/table')
        routing.add(
            disabled='no',
            fib='True',
            name=self.to_vpn_table_name
        )

    def __create_ip_rules(self):
        ip_addresses = self.api.get_resource('/ip/address')

        # Server network
        ip_addresses.add(
            address=self.service_peer_address.split('/')[0] + "/24",
            interface=self.server_interface_name
        )
        # Client
        ip_addresses.add(
            address=self.client_address.split('/')[0] + "/24",
            interface=self.client_interface_name
        )
        # Edit firewall
        firewall_mangle = self.api.get_resource('/ip/firewall/mangle')
        firewall_mangle.add(
            action='mark-routing',
            chain='prerouting',
            src_address=self.server_network.split('/')[0] + "/24",
            dst_address=f'!{self.server_network.split("/")[0] + "/24"}',
            src_address_list='WG-WebMode-Easy-ToVpn-Addresses',
            in_interface=self.server_interface_name,
            new_routing_mark=self.to_vpn_table_name,
            passthrough='yes'
        )

        firewall_service_port = self.api.get_resource('/ip/firewall/')
        firewall_service_port.call('service-port/enable', {'numbers': 'irc'})
        firewall_service_port.call('service-port/enable', {'numbers': 'rtsp'})
        # IP Routes
        routes = self.api.get_resource('/ip/route')
        routes.add(
            distance='1',
            dst_address='0.0.0.0/0',
            gateway=self.client_interface_name,
            routing_table=self.to_vpn_table_name,
            scope='30',
            suppress_hw_offload='no',
            target_scope='10'
        )

    def __add_port_forward(self):
        firewall_nat = self.api.get_resource('/ip/firewall/nat')
        firewall_nat.add(
            comment="WireGuard Port Forward",
            action='dst-nat',
            chain='dstnat',
            protocol='udp',
            dst_port=self.server_listen_port,
            in_interface=self.wan_interface_name,
            to_addresses=self.host
        )
        firewall_nat.add(
            comment="WireGuard Port Forward",
            action='masquerade',
            chain='srcnat',
            out_interface=self.client_interface_name
        )

    def run(self):
        self.__create_backup()
        self.__create_interfaces()
        self.__create_client_peer()
        self.__create_service_server_peer()
        self.__create_routing_table()
        self.__create_ip_rules()
        if self.port_forward_mode is True:
            self.__add_port_forward()
