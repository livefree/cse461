/**
 * @author Runfeng Liu (runfengl)
 *	   Course:	CSC 461
 *    Project:	Socket API part 2
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;


public class Packet {
    public static final int HEAD_BYTE_SIZE = 12;
    private Header header;
    private byte[] payload;

    public Packet(Header header, byte[] payload) {
        this.header = header;
        this.payload = payload;
        this.header.setPayloadLen(this.payload.length);
    }

    public Packet(int psecret, short step, short id, byte[] payload) {
        this(new Header(psecret, step, id), payload);
    }

    /**
     * Construct a packet from a string that received from a UDP port in a DatagramPacket
     *
     */
    public Packet(byte[] data) throws IOException {
        this.header = new Header(Arrays.copyOfRange(data, 0, HEAD_BYTE_SIZE));
        int payloadLength = header.getPayloadLen();
        if (data.length == HEAD_BYTE_SIZE) {
            this.payload = null; // empty payload
        } else {
            this.payload = Arrays.copyOfRange(data, HEAD_BYTE_SIZE, HEAD_BYTE_SIZE + payloadLength);

        }
    }

    public Packet(Header header) {
        this.header = header;
        this.payload = null;
    }

    public byte[] toNetworkBytes() throws IOException {
        ByteArrayOutputStream packetStream = new ByteArrayOutputStream();

        // write header in big endian order by default
        packetStream.write(ByteBuffer.allocate(4).putInt(header.getPayloadLen()).array());
        packetStream.write(ByteBuffer.allocate(4).putInt(header.getPSecret()).array());
        packetStream.write(ByteBuffer.allocate(2).putShort(header.getStep()).array());
        packetStream.write(ByteBuffer.allocate(2).putShort(header.getId()).array());
        // write payload
        packetStream.write(payload);

        int totalLength;
        totalLength = header.getPayloadLen() + 12; // 12 bytes for header fields

        int paddingSize = (totalLength % 4 == 0) ? 0 : 4 - (totalLength % 4);
        byte[] padding = new byte[paddingSize];
        packetStream.write(padding);
        return packetStream.toByteArray();
    }

    public void payloadBuild(byte[] data) {
        if (data == null || data.length == 0) {
            return;
        }
        if (payload == null || payload.length == 0) {
            payload = data;
            header.setPayloadLen(payload.length);
            return;
        }

        byte[] result = new byte[payload.length + data.length];
        System.arraycopy(payload, 0, result, 0, payload.length);
        System.arraycopy(data, 0, result, payload.length, data.length);
        payload = result;
        this.header.setPayloadLen(payload.length);

    }

    public void payloadBuild(byte b, int len) {
        if (len == 0) {
            return;
        }
        byte[] data = new byte[len];
        Arrays.fill(data, b);

        if (payload == null) {
            byte[] result = new byte[data.length];
            System.arraycopy(data, 0, result, 0, data.length);
            payload = result;
            this.header.setPayloadLen(payload.length);
        } else {
            byte[] result = new byte[payload.length + data.length];
            System.arraycopy(payload, 0, result, 0, payload.length);
            System.arraycopy(data, 0, result, payload.length, data.length);
            payload = result;
            this.header.setPayloadLen(payload.length);
        }
    }

    public void payloadBuild(int num) {
        byte[] b = ByteBuffer.allocate(4).putInt(num).array();
        payloadBuild(b);
    }

    public int getIntFromNetworkBytes(byte[] bytes, int startPos) {
        return ((bytes[startPos] & 0xFF) << 24) |
                ((bytes[startPos + 1] & 0xFF) << 16) |
                ((bytes[startPos + 2] & 0xFF) << 8) |
                (bytes[startPos + 3] & 0xFF);
    }

    public byte getCharFromNetworkBytes(byte[] bytes, int startPos) {
        return bytes[startPos];
    }

    public Header getHeader() {
        return header;
    }

    public byte[] getPayload() {
        return payload;
    }

    public int getPacketSize() {
        return HEAD_BYTE_SIZE + payload.length;
    }

    @Override
    public String toString() {
        return "Packet {" + header + ",\npayload("
                + payload.length + " byte) "
                + Arrays.toString(payload) + " }";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Packet packet = (Packet) o;
        return Objects.equals(header, packet.header) && Arrays.equals(payload, packet.payload);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(header);
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }
}
