/**
 * @author Runfeng Liu (runfengl)
 *	   Course:	CSC 461
 *    Project:	Socket API part 1
 */
import java.nio.ByteBuffer;
import java.util.Objects;

public class Header {
    private int payloadLen;
    private int psecret;
    private short step;
    private short id;

    public Header(int psecret, short step, short id) {
        this.payloadLen = 0;
        this.psecret = psecret;
        this.step = step;
        this.id = id;
    }

    public Header(byte[] bytes) {
        this.payloadLen = ByteBuffer.wrap(bytes, 0, 4).getInt();
        this.psecret = ByteBuffer.wrap(bytes, 4, 4).getInt();
        this.step = ByteBuffer.wrap(bytes, 8, 2).getShort();
        this.id = ByteBuffer.wrap(bytes, 10, 2).getShort();
    }

    public byte[] getHeader() {
        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.putInt(payloadLen);
        buffer.putInt(psecret);
        buffer.putShort(step);
        buffer.putShort(id);
        return buffer.array();
    }

    public int getPayloadLen() {
        return payloadLen;
    }

    public int getPSecret() {
        return psecret;
    }

    public short getStep() {
        return step;
    }

    public short getId() {
        return id;
    }

    public void setPayloadLen(int payloadLen) {
        this.payloadLen = payloadLen;
    }

    public void setId(short id) {
        this.id = id;
    }

    public void setPsecret(int psecret) {
        this.psecret = psecret;
    }


    @Override
    public String toString() {
        return "Header{ " +
                "payloadLength: " + payloadLen +
                ", psecret: " + psecret +
                ", step: " + step +
                ", stu id:  " + id +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Header header = (Header) o;
        return payloadLen == header.payloadLen && psecret == header.psecret && step == header.step && id == header.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(payloadLen, psecret, step, id);
    }
}

