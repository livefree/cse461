/**
 * @author Runfeng Liu (runfengl)
 *	   Course:	CSC 461
 *    Project:	Socket API part 2
 */

import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
public class Client {
    public static void main(String[] args) throws Exception {
        // The server will run on the host on attu2.cs.washington.edu and attu3.cs.washington.edu,
        // listening for incoming packets on UDP port 12235
//        String serverHost = "attu2.cs.washington.edu";
        String serverHost = "localhost";
        int UDPPort = 12235;

        // Stage A: Send a "hello world" message to the server
        byte[] message = new byte[]{'h', 'e', 'l', 'l', 'o', ' ', 'w', 'o', 'r', 'l', 'd', '\0'};

        int psecret = 0; // For stage A, psecret is defined as 0.
        short id = 208; // Last 3 digits of student id.
        short step = 1; // Client's step is always be 1

        // Create a DatagramSocket object to send and receive packets
        DatagramSocket UDPSocket = new DatagramSocket();

        // data in network-byte order (big-endian order) and 4-byte aligned
        Packet outPacket = new Packet(psecret, step, id, message);

        // Use the socket to send a message to a server
        InetAddress serverAddress = InetAddress.getByName(serverHost);
        // create buffer to store send data
        byte[] data = outPacket.toNetworkBytes();
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, serverAddress, UDPPort);

//        System.out.println(String.format("Sending packet %d bytes to %s:%s", sendPacket.getLength(), sendPacket.getAddress().toString(), sendPacket.getPort()));
        UDPSocket.send(datagramPacket);
        System.out.println("Stage A:");
        System.out.printf("Send UDP packet (%d bytes) to %s:%s%n",
                datagramPacket.getLength(),
                datagramPacket.getAddress().toString(),
                datagramPacket.getPort());


        // create a DatagramPacket object to receive data
//        System.out.println(String.format("Listening from %s on port %d ...", serverAddress.toString(), UDPPort));
        byte[] buffer = new byte[1024];
        datagramPacket = new DatagramPacket(buffer, buffer.length);

        // Receive a packet Step a2
        UDPSocket.receive(datagramPacket);

        // Store received data
        data = new byte[datagramPacket.getLength()]; // create a new byte array with the correct size
        System.arraycopy(datagramPacket.getData(), 0, data, 0, datagramPacket.getLength()); // copy the data to the new array

        Packet inPacket = new Packet(data);

        //read incoming payloas
        int num = inPacket.getIntFromNetworkBytes(inPacket.getPayload(), 0);
        int len = inPacket.getIntFromNetworkBytes(inPacket.getPayload(), 4);
        int udp_port = inPacket.getIntFromNetworkBytes(inPacket.getPayload(), 8);
        int secretA = inPacket.getIntFromNetworkBytes(inPacket.getPayload(), 12);
        System.out.printf("""
                        Received UDP packet from %s:%d\s
                        Received header: %s\s
                        Received message: num = %d, len = %d, udp_port = %d, secretA = %d%n""",
                datagramPacket.getAddress().toString(), datagramPacket.getPort(),
                inPacket.getHeader(), num, len, udp_port, secretA);

        // Stage b
        // Transmit UDP packets num times to the server on port udp_port
        // step b1
        System.out.println("Stage B:");

        int packet_id = 0;
        int numAttempts = 0;
        while (packet_id < num && numAttempts++ < 100) {
            // the payload start with 4 byte packet_id, padding len num of \0
            outPacket = new Packet(new Header(secretA, step, id));
            outPacket.payloadBuild(packet_id);
            outPacket.payloadBuild((byte)'\0', len);

            datagramPacket = new DatagramPacket(outPacket.toNetworkBytes(), outPacket.toNetworkBytes().length,
                    serverAddress, udp_port); // updated

            try {
                // Send the packet
                System.out.printf("\033[2K Sending UDP packet #%3d of %d\r", packet_id, num);
                UDPSocket.send(datagramPacket);

                // Set a timeout of 0.5 seconds for the receive() method
                UDPSocket.setSoTimeout(500);

                //receive ACK from the server
                buffer = new byte[1024];
                datagramPacket = new DatagramPacket(buffer, buffer.length);

                // Receive a packet
                UDPSocket.receive(datagramPacket);

                // Store received data
                data = new byte[datagramPacket.getLength()]; // create a new byte array with the correct size
                System.arraycopy(datagramPacket.getData(), 0, data, 0, datagramPacket.getLength()); // copy the data to the new array

                inPacket = new Packet(data);
                int acked_packet_id = inPacket.getIntFromNetworkBytes(inPacket.getPayload(), 0);
                if (acked_packet_id == packet_id) {
                    packet_id++;
                }
                numAttempts = 0;
            } catch (java.net.SocketTimeoutException e) {
                // Handle the timeout exception
//                System.err.println("Timeout waiting for response from server. " + packet_id);
            }

        }

        //step b2
        //receive ACK from the server
        buffer = new byte[1024];
        datagramPacket = new DatagramPacket(buffer, buffer.length);
        UDPSocket.setSoTimeout(2000);
        UDPSocket.receive(datagramPacket);

        // Store received data
        data = new byte[datagramPacket.getLength()]; // create a new byte array with the correct size
        System.arraycopy(datagramPacket.getData(), 0, data, 0, datagramPacket.getLength()); // copy the data to the new array

        inPacket = new Packet(data);
        int tcp_port = inPacket.getIntFromNetworkBytes(inPacket.getPayload(), 0);
        int secretB = inPacket.getIntFromNetworkBytes(inPacket.getPayload(), 4);
        System.out.printf("""
                        Received UDP packet from %s:%d\s
                        Received header: %s\s
                        Received message: TCP port = %d, secret B = %d%n""",
                datagramPacket.getAddress().toString(), datagramPacket.getPort(),
                inPacket.getHeader(), tcp_port, secretB);

        // close socket
        UDPSocket.close();

        ////////////
        // Stage C//
        ////////////
        // Open a TCP connection
        System.out.println("Stage C");
        Socket socket = new Socket(serverAddress, tcp_port);
        System.out.printf("Open a TCP connection to %s:%d%n", serverAddress.toString(), tcp_port);

        // Create a new socket and send a SYN packet to the server
        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        byte[] response = new byte[1024];
        in.read(response); // Wait for response
        inPacket = new Packet(response);
        System.out.println(inPacket);

        int num2 = inPacket.getIntFromNetworkBytes(inPacket.getPayload(), 0);
        int len2 = inPacket.getIntFromNetworkBytes(inPacket.getPayload(), 4);
        int secretC = inPacket.getIntFromNetworkBytes(inPacket.getPayload(), 8);
        byte ch = inPacket.getCharFromNetworkBytes(inPacket.getPayload(), 12);

        System.out.printf("Received TCP packet from %s:%d" +
                        "\nReceived message: num2: %d, len2: %d, secretC: %d, char: 0x%02X%n",
                serverAddress, tcp_port, num2, len2, secretC, ch);

        ////////////
        // Stage D//
        ////////////
        System.out.println("Stage D");
        // Create a packet that payload with of length len2 filled with char c
        outPacket = new Packet(new Header(secretC, step, id));
        outPacket.payloadBuild(ch, len2);

        System.out.printf(String.format("Sending %d TCP packets to %s:%d\r",
                num2, socket.getInetAddress(), socket.getPort()));

        try {
            // send this packet num2 times to the server
            int syn = 0;
            while (syn < num2) {
                out.write(outPacket.toNetworkBytes());
                syn++;
            }
        } catch (SocketException e) {
            System.out.println(e);
        }
        System.out.printf("\u001B[2KSend %d TCP packets (%d bytes) to %s:%d.%n", num2, len2,
                socket.getInetAddress(), socket.getPort());

        // receive response from the sever
        response = new byte[1024];
        if (socket.isConnected() && !socket.isClosed()) {
            in.read(response);
            inPacket = new Packet(response);
            int secretD = inPacket.getIntFromNetworkBytes(inPacket.getPayload(), 0);
            System.out.printf("Received TCP packet from %s:%d" +
                            "\nReceived message: secret D: %d%n",
                    socket.getInetAddress(), socket.getPort(), secretD);
        }
    }

}
