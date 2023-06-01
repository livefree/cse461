/**
 * @author Runfeng Liu (runfengl)
 *	   Course:	CSC 461
 *    Project:	Socket API part 2
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Random;

public class Server {

    public static final byte[] MESSAGE = new byte[]{'h', 'e', 'l', 'l', 'o', ' ', 'w', 'o', 'r', 'l', 'd', '\0'};
    public static final int SERVER_UDP_PORT = 12235;
    public static final int TIMEOUT = 3000;
    public static short SERVER_STEP = 2;
    public static short CLIENT_STEP = 1;

    public static void main(String[] args) throws IOException {
        // Create a DatagramSocket object to send and receive packets
        DatagramSocket UDPSocket = new DatagramSocket(SERVER_UDP_PORT);

        // Server keep listening on UDP port
        while (true) {
            // create a DatagramPacket object to receive data
            byte[] buffer = new byte[1024];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

            // receive packet from client
            UDPSocket.receive(datagramPacket);

            // Create a new ClientHandler instance and start it
            ClientHandler clientHandler = new ClientHandler(datagramPacket);
            clientHandler.start();
        }

    }// end main

} // end class

class ClientHandler extends Thread {
    private DatagramPacket datagramPacket;

    public ClientHandler(DatagramPacket datagramPacket) {
        this.datagramPacket = datagramPacket;
    }

    @Override
    public void run() {
        int secret = 0;
        int remote_port;
        InetAddress remote_address;
        try {
            // process first packet from client
            remote_port = datagramPacket.getPort();
            remote_address = datagramPacket.getAddress();

            // create header and packet used to verify client3
            Header expectedHeader = new Header(secret, Server.CLIENT_STEP, (short) 0);
            Packet expectedPacket = new Packet(expectedHeader);
            expectedPacket.payloadBuild(Server.MESSAGE);

            // Store received data
            byte[] data = new byte[datagramPacket.getLength()]; // create a new byte array with the correct size
            System.arraycopy(datagramPacket.getData(), 0, data, 0, datagramPacket.getLength()); // copy the data to the new array
            // Verify length
            if (data.length != expectedPacket.getPacketSize()) {
                // fail
                System.out.println("fail 0");
                return;
            }
            Packet inPacket = new Packet(data);
            Header client_header = inPacket.getHeader();
            // update expected head's stu id
            short stu_id = client_header.getId();
            expectedHeader.setId(stu_id);

            System.out.printf("Receive UDP packet from %s:%d%n", remote_address, remote_port);
            System.out.println("Received client header" + client_header);

            if (!inPacket.equals(expectedPacket)) {
                //fail
                return;
            }

            expectedHeader.setId(inPacket.getHeader().getId());

            // create response packet for stage A
            Header server_header = new Header(secret, Server.SERVER_STEP, stu_id);
            Packet outPacket = new Packet(server_header.getHeader());

            // create a new Random instance
            Random random = new Random();

            // add num len udp_port secretA to payload
            int num = random.nextInt(100) + 10; //
            int len = random.nextInt(100) + 10;
            int secretA = random.nextInt(Short.MAX_VALUE);

            // create a new response socket with a new udp port
            DatagramSocket responseSocket= new DatagramSocket();

            // Set timeout
            responseSocket.setSoTimeout(Server.TIMEOUT);
            int udp_port = responseSocket.getLocalPort();

            // put information in payload
            outPacket.payloadBuild(num); // num
            outPacket.payloadBuild(len); // len
            outPacket.payloadBuild(udp_port); // udp_port
            outPacket.payloadBuild(secretA); // secretA

            // respond to client. provide information used for next step
            data = outPacket.toNetworkBytes();
            datagramPacket = new DatagramPacket(data, data.length, remote_address, remote_port);
            responseSocket.send(datagramPacket);
            System.out.println(">>>Stage A. Send\n" + outPacket);

            ////////////
            // Stage B//
            ////////////
            System.out.println(">>> Stage B");
            // update expected header
            expectedHeader.setPayloadLen(len + 4);
            expectedHeader.setPsecret(secretA); // set secret to previous
            expectedHeader.setId(stu_id);

            // receive num of packet
            int acked_packet_id = 0;
            while (acked_packet_id < num) {
                // expected packet
                expectedPacket = new Packet(expectedHeader);
                expectedPacket.payloadBuild(acked_packet_id); // first packet id is 0
                expectedPacket.payloadBuild(new byte[len]); // len of 0's

                //receive packet from the client
                byte[] buffer = new byte[1024];
                datagramPacket = new DatagramPacket(buffer, buffer.length);
                responseSocket.receive(datagramPacket);

                // get data
                data = new byte[datagramPacket.getLength()]; // create a new byte array with the correct size
                System.arraycopy(datagramPacket.getData(), 0, data, 0, datagramPacket.getLength()); // copy the data to the new array

                if (data.length < Packet.HEAD_BYTE_SIZE) {
                    //fail
                    System.out.println("Stage B fail 0");
                    responseSocket.close();
                    return;
                }

                inPacket = new Packet(data);

                // verify packet
                if (!inPacket.equals(expectedPacket)) {
                    System.out.println("Stage B");
                    System.out.println("expected " + expectedPacket + "\nreceived " + inPacket);
                    responseSocket.close();
                    return;
                }

                // send ack packet
                outPacket = new Packet(expectedHeader);
                outPacket.payloadBuild(acked_packet_id);
                data = outPacket.toNetworkBytes();
                acked_packet_id++;

                datagramPacket = new DatagramPacket(data, data.length, remote_address, remote_port);
                responseSocket.send(datagramPacket);


            }
            System.out.printf("ACK %d UDP packets from client%n", acked_packet_id);
            // b2
            // send last UDP packet contain tcp_port for Stage C
            // create a TCP server socket
            ServerSocket serverSocket = new ServerSocket(0); // create a server socket on port 1234
            System.out.println("TCP Server started on " + InetAddress.getLocalHost());

            // get tcp port and secret b
            int tcp_port = serverSocket.getLocalPort();
            int secretB = random.nextInt(Short.MAX_VALUE);
            server_header.setPsecret(secretB);

            // create packet
            outPacket = new Packet(server_header);
            outPacket.getHeader().setPsecret(secretA);
            outPacket.payloadBuild(tcp_port);
            outPacket.payloadBuild(secretB);
            data = outPacket.toNetworkBytes();

            // create a new datagram packet
            datagramPacket = new DatagramPacket(data, data.length, remote_address, remote_port);
            responseSocket.send(datagramPacket);
            responseSocket.close();

            ////////////
            // Stage C//
            ////////////
            System.out.println(">>> Stage C");
            // stage c1
            // accept incoming connections and handle them in a loop
            Socket socket = serverSocket.accept(); // accept a new connection from a client
            socket.setSoTimeout(Server.TIMEOUT);
            System.out.println("Accepted connection from client from" + socket.getInetAddress().getHostAddress());

            // stage c2
            // send three int and a char to client
            int num2 = random.nextInt(100) + 10;
            int len2 = random.nextInt(100) + 10;
            int secretC = random.nextInt(Short.MAX_VALUE);
            byte c = (byte) random.nextInt(128); //Random ASCII character

            // create packet
            server_header = new Header(secretC, Server.SERVER_STEP, stu_id);
            outPacket = new Packet(server_header);
            outPacket.payloadBuild(num2);
            outPacket.payloadBuild(len2);
            outPacket.payloadBuild(secretC);
            outPacket.payloadBuild(c, 1);

            // get the output stream of the socket
            OutputStream outputStream = socket.getOutputStream();

            // send data
            outputStream.write(outPacket.toNetworkBytes());
            System.out.println("Send packet\n" + outPacket);

            ////////////
            // Stage D//
            ////////////
            System.out.println(">>>Stage D");
            // stage d1
            // update expected header with new secret C
            expectedHeader.setPsecret(secretC);
            expectedPacket = new Packet(expectedHeader);

            // expected payload of length len2 filled with char c
            expectedPacket.payloadBuild(c, len2);

            // create input stream
            InputStream in = socket.getInputStream();

            // receive num2 payloads
            int i = 0;
            while (i < num2) {
                // buffer incoming packet
                byte[] buffer = new byte[1024];
                int bytesRead = in.read(buffer, 0, expectedPacket.toNetworkBytes().length);
                if (bytesRead == -1) {
                    // read fail
                    return;
                }
                inPacket = new Packet(buffer);

                // verify
                if (!inPacket.equals(expectedPacket)) {
                    System.out.printf(String.format("Packet received is not correct! \n Expected: %s\n %s", expectedPacket, inPacket));
                    return;
                }
                System.out.printf("\r\033[2K %d of %d packets received", i + 1, num2);
                i++;
            }
            System.out.println();
            // d2
            if (i != num2) {
                socket.close();
            }
            // generate secret D
            int secretD = random.nextInt(Short.MAX_VALUE);
            outPacket = new Packet(expectedHeader);
            outPacket.getHeader().setPsecret(secretD);
            outPacket.payloadBuild(secretD);
            // send last secret to client
            outputStream.write(outPacket.toNetworkBytes());
            System.out.println("Current connection closed");
        } catch (SocketTimeoutException e) {
            System.err.println("Timeout occurred while handling the client.");
            // Close resources specific to this client handler if needed
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}