class Firewall (object):
  """
  A Firewall object is created for each switch that connects.
  A Connection object for that switch is passed to the __init__ function.
  """
  def __init__ (self, connection):
    # Keep track of the connection to the switch so that we can
    # send it messages!
    self.connection = connection

    # This binds our PacketIn event listener
    connection.addListeners(self)

    # Add switch rules here
    # 1. accept all ICMP packets
    # self.add_rule("dl_type=0x0800,nw_proto=1", [of.ofp_action_output(port=of.OFPP_FLOOD)])

    # 2. accept all ARP packets
    # self.add_rule("dl_type=0x0806", [of.ofp_action_output(port=of.OFPP_FLOOD)])

    # 3. drop all other IPv4 packets
    # self.add_rule("dl_type=0x0800", [of.ofp_action_output(port=of.OFPP_NONE)])

  def add_rule(self, match, actions):
    """
    Adds a flow rule to the switch
    """
    flow_mod = of.ofp_flow_mod()
    flow_mod.match = self.parse_match(match)
    
    for action in actions:
      flow_mod.actions.append(action)
    self.connection.send(flow_mod)
  
  def parse_match(self, match_str):
    """
    Parses a match string and returns an ofp_match object
    """
    match = of.ofp_match()
  
    match_elements = match_str.split(',')
  
    for element in match_elements:
      key, value = element.split('=')
  
      if key == 'dl_type':
        match.dl_type = int(value, 0)
      elif key == 'nw_proto':
        match.nw_proto = int(value)
      # Add any other attributes you need to match here
  
    return match

  def _handle_PacketIn (self, event):
    """
    Packets not handled by the router rules will be
    forwarded to this method to be handled by the controller
    """

    packet = event.parsed # This is the parsed packet data.
    if not packet.parsed:
      log.warning("Ignoring incomplete packet")
      return

    packet_in = event.ofp # The actual ofp_packet_in message.
    print("Unhandled packet: " + str(packet))

def launch ():
  """
  Starts the component
  """
  def start_switch (event):
    log.debug("Controlling %s" % (event.connection,))
    Firewall(event.connection)
  core.openflow.addListenerByName("ConnectionUp", start_switch)
