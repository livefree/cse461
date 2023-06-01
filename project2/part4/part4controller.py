# Part 4 of UWCSE's Project 2
#
# based on Lab Final from UCSC's Networking Class
# which is based on of_tutorial by James McCauley

from pox.core import core
import pox.openflow.libopenflow_01 as of
from pox.lib.addresses import IPAddr, IPAddr6, EthAddr
from pox.lib.packet.ethernet import ethernet
from pox.lib.packet.arp import arp	

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

# dpid to mac address. From l3_learning.py
def dpid_to_mac(dpid):
    # converting the 48 least significant bits of dpid into a zero-padded, 12-character-long hexadecimal string.
    return EthAddr("%012x" % (dpid & 0xffFFffFFffFF,))


class Entry(object):
    """
    From l3_learning.py but only keep port and mac
    port: determine which port to forward traffic out of.
    mac: used to answer ARP replies.
    """
    def __init__(self, port, mac):
        self.port = port
        self.mac = mac


class Part4Controller(object):
    """
    A Connection object for that switch is passed to the __init__ function.
    """

    def __init__(self, connection):
        print(connection.dpid)
        # Keep track of the connection to the switch so that we can
        # send it messages!
        self.connection = connection
        # Create a table to map IP addresses to Entries
        self.arpTable = {}
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
            msg.actions.append(of.ofp_action_output(port=port_num))
        self.connection.send(msg)

    def add_flood_rule(self):
        # used for secondary routers to flood traffic
        msg = of.ofp_flow_mod()
        msg.priority = 1
        msg.actions.append(of.ofp_action_output(port=of.OFPP_FLOOD))
        self.connection.send(msg)

    def cores21_setup(self):
        # h10_port = 1
        # h20_port = 2
        # h30_port = 3
        # serv1_port = 4
        # hnotrust1_port = 5
        # drop rules
        self.add_cores21_rule(30, 0x0800, 1, IPS["hnotrust"][0], None, None)
        self.add_cores21_rule(20, 0x0800, None, IPS["hnotrust"][0], IPS["serv1"][0], None)
        # Don't need for part4, learn from handle incoming packets
        # to hosts
        # self.add_cores21_rule(10, 0x0800, None, None, IPS["h10"][0], h10_port)
        # self.add_cores21_rule(10, 0x0800, None, None, IPS["h20"][0], h20_port)
        # self.add_cores21_rule(10, 0x0800, None, None, IPS["h30"][0], h30_port)
        # self.add_cores21_rule(10, 0x0800, None, None, IPS["serv1"][0], serv1_port)
        # self.add_cores21_rule(10, 0x0800, None, None, IPS["hnotrust"][0], hnotrust1_port)
        # self.add_flood_rule()

    # used in part 4 to handle individual ARP packets
    # not needed for part 3 (USE RULES!)
    # causes the switch to output packet_in on out_port
    def resend_packet(self, packet_in, out_port):
        msg = of.ofp_packet_out()
        msg.data = packet_in
        action = of.ofp_action_output(port=out_port)
        msg.actions.append(action)
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
        dpid = self.connection.dpid

        # handle ARP request
        if (packet.type == packet.ARP_TYPE) and (packet.payload.opcode == arp.REQUEST):
            if dpid not in self.arpTable:
                # New table entry
                self.arpTable[dpid] = {}
            if packet.src not in self.arpTable[dpid]:
                self.arpTable[dpid][packet.payload.protosrc] = Entry(event.port, packet.src)
                # IP message
                msg = of.ofp_flow_mod()
                msg.priority = 10
                msg.match.dl_type = 0x0800
                msg.match.nw_dst = packet.next.protosrc
                msg.actions.append(of.ofp_action_dl_addr.set_src(dpid_to_mac(dpid)))
                msg.actions.append(of.ofp_action_dl_addr.set_dst(packet.src))
                msg.actions.append(of.ofp_action_output(port=event.port))
            # build reply arp
            arp_reply = arp()
            arp_reply.hwsrc = dpid_to_mac(dpid)
            arp_reply.hwdst = packet.src
            arp_reply.opcode = arp.REPLY
            arp_reply.protosrc = packet.next.protodst  # next and payload both work for arp
            arp_reply.protodst = packet.next.protosrc
            # modify ethernet header
            ether = ethernet()
            ether.type = ethernet.ARP_TYPE
            ether.dst = packet.src
            ether.src = dpid_to_mac(dpid)
            ether.payload = arp_reply
            # send message and arp reply
            self.connection.send(msg)
            self.resend_packet(ether, event.port)

            print("Unhandled packet from " + str(self.connection.dpid) + ":" + packet.dump())


def launch():
    """
    Starts the component
    """

    def start_switch(event):
        log.debug("Controlling %s" % (event.connection,))
        Part4Controller(event.connection)

    core.openflow.addListenerByName("ConnectionUp", start_switch)