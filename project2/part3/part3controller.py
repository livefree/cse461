# Part 3 of UWCSE's Project 3
#
# based on Lab Final from UCSC's Networking Class
# which is based on of_tutorial by James McCauley

from pox.core import core
import pox.openflow.libopenflow_01 as of
from pox.lib.addresses import IPAddr, IPAddr6, EthAddr

log = core.getLogger()

# statically allocate a routing table for hosts
# MACs used in only in part 4
IPS = {
    "h10": ("10.0.1.10", '00:00:00:00:00:01'),
    "h20": ("10.0.2.20", '00:00:00:00:00:02'),
    "h30": ("10.0.3.30", '00:00:00:00:00:03'),
    "serv1": ("10.0.4.10", '00:00:00:00:00:04'),
    "hnotrust": ("172.16.10.100", '00:00:00:00:00:05'),
}


class Part3Controller(object):
    """
    A Connection object for that switch is passed to the __init__ function.
    """

    def __init__(self, connection):
        print(connection.dpid)
        # Keep track of the connection to the switch so that we can
        # send it messages!
        self.connection = connection

        # This binds our PacketIn event listener
        connection.addListeners(self)
        # use the dpid to figure out what switch is being created
        if (connection.dpid == 1):
            self.s1_setup()
        elif (connection.dpid == 2):
            self.s2_setup()
        elif (connection.dpid == 3):
            self.s3_setup()
        elif (connection.dpid == 21):
            self.cores21_setup()
        elif (connection.dpid == 31):
            self.dcs31_setup()
        else:
            print("UNKNOWN SWITCH")
            exit(1)

    def s1_setup(self):
        # put switch 1 rules here
        self.add_flood_rule()

    def s2_setup(self):
        # put switch 2 rules here
        self.add_flood_rule()

    def s3_setup(self):
        # put switch 3 rules here
        self.add_flood_rule()

    def dcs31_setup(self):
        # put datacenter switch rules here
        self.add_flood_rule()

    def add_flood_rule(self):
        # used for secondary routers to flood traffic
        msg = of.ofp_flow_mod()
        msg.priority = 1
        msg.actions.append(of.ofp_action_output(port=of.OFPP_FLOOD))
        self.connection.send(msg)
    
    def add_cores21_rule(self, priority, dl_type, proto, src, dst, port_num):
        msg = of.ofp_flow_mod()
        if priority != None:
            msg.priority = priority
        if dl_type != None:
            msg.match.dl_type = dl_type
        if proto != None:
            msg.match.proto = proto
        if src != None:
            msg.match.nw_src = src
        if dst != None:
            msg.match.nw_dst = dst
        if port_num != None:
            msg.actions.append(of.ofp_action_output(port = port_num))
        self.connection.send(msg)

    def cores21_setup(self):
        h10_port = 1
        h20_port = 2
        h30_port = 3
        serv1_port = 4
        hnotrust1_port = 5
        # drop rules
        self.add_cores21_rule(30, 0x0800, 1, IPS["hnotrust"][0], None, None)
        self.add_cores21_rule(20, 0x0800, None, IPS["hnotrust"][0], IPS["serv1"][0], None)
        # to hosts
        self.add_cores21_rule(10, 0x0800, None, None, IPS["h10"][0], h10_port)
        self.add_cores21_rule(10, 0x0800, None, None, IPS["h20"][0], h20_port)
        self.add_cores21_rule(10, 0x0800, None, None, IPS["h30"][0], h30_port)
        self.add_cores21_rule(10, 0x0800, None, None, IPS["serv1"][0], serv1_port)
        self.add_cores21_rule(10, 0x0800, None, None, IPS["hnotrust"][0], hnotrust1_port)
        self.add_flood_rule()
        # host 10
        """
        msg_10 = of.ofp_flow_mod()
        msg_10.priority = 10
        msg_10.match.dl_type = 0x0800  # IPv4
        msg_10.match.nw_dst = IPS["h10"][0]  # to host 10
        # send it out through port 1
        msg_10.actions.append(of.ofp_action_output(port=h10_port))  
        self.connection.send(msg_10)
        print(msg_10)
        """
        # host 20
        """
        msg_20 = of.ofp_flow_mod()
        msg_20.priority = 10
        msg_20.match.dl_type = 0x0800  # IPv4
        msg_20.match.nw_dst = IPS["h20"][0]  # to host 20
        # send it out -> port 2
        msg_20.actions.append(of.ofp_action_output(port=h20_port))  
        self.connection.send(msg_20)

        # to host 30
        msg_30 = of.ofp_flow_mod()
        msg_30.priority = 10
        msg_30.match.dl_type = 0x0800  # IPv4
        msg_30.match.nw_dst = IPS["h30"][0]  # to host 30
        # send it out -> port 3
        msg_30.actions.append(of.ofp_action_output(port=h30_port))  
        self.connection.send(msg_30)

        # to dcs31
        msg_dcs31 = of.ofp_flow_mod()
        msg_dcs31.priority = 10
        msg_dcs31.match.dl_type = 0x0800  # IPvdc
        msg_dcs31.match.nw_dst = IPS["serv1"][0]  # to host dcs31
        # send it out -> port 4
        msg_dcs31.actions.append(of.ofp_action_output(port=serve1_port))  
        self.connection.send(msg_dcs31)

        # to untrusted network
        msg_hnotrust1 = of.ofp_flow_mod()
        msg_hnotrust1.priority = 10
        msg_hnotrust1.match.dl_type = 0x0800  # IPv4
        # send it out -> port 5
        msg_hnotrust1.actions.append(of.ofp_action_output(port=hnotrust1_port)) 
        self.connection.send(msg_hnotrust1)
        """        

    # block all ICMP traffic from untrusted host
    def add_drop_ICMP_rule(self):
        # block all ICMP traffic from untrusted host
        msg = of.ofp_flow_mod()
        msg.priority = 11
        msg.match.dl_type = 0x800
        msg.match.proto = 1
        msg.match.nw_src = IPS['hnotrust'][0]
        self.connection.send(msg)

    # block all ipv4 traffic from untrusted to serv1
    def add_drop_IPv4_rule(self):
        # block all ipv4 traffic from untrusted host to server
        msg = of.ofp_flow_mod()
        msg.priority = 10
        msg.match.dl_type = 0x800
        msg.match.nw_src = IPS['hnotrust'][0]
        msg.match.nw_dst = IPS['serv1'][0]
        self.connection.send(msg)

    # used in part 4 to handle individual ARP packets
    # not needed for part 3 (USE RULES!)
    # causes the switch to output packet_in on out_port
    def resend_packet(self, packet_in, out_port):
        msg = of.ofp_packet_out()
        msg.data = packet_in
        action = of.ofp_action_output(port=out_port)
        msg.actions.append(action)
        msg.action.append(of.ofp_action_output(None))
        self.connection.send(msg)

    def _handle_PacketIn(self, event):
        """
        Packets not handled by the router rules will be
        forwarded to this method to be handled by the controller
        """

        packet = event.parsed  # This is the parsed packet data.
        if not packet.parsed:
            log.warning("Ignoring incomplete packet")
            return

        packet_in = event.ofp  # The actual ofp_packet_in message.
        print("Unhandled packet from " + str(self.connection.dpid) + ":" + packet.dump())


def launch():
    """
    Starts the component
    """

    def start_switch(event):
        log.debug("Controlling %s" % (event.connection,))
        Part3Controller(event.connection)

    core.openflow.addListenerByName("ConnectionUp", start_switch)