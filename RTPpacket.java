import java.util.Arrays;

public class RTPpacket {

    // Size of the RTP header:
    static int HEADER_SIZE = 12;

    // Fields that compose the RTP header
    private int version;
    private int padding;
    private int extension;
    private int cc;
    private int marker;
    private int payloadType;
    private int sequenceNumber;
    private int timeStamp;
    private int ssrc;

    // Bitstream of the RTP header
    private byte[] header;

    // Size of the RTP payload
    private int payloadSize;
    // Bitstream of the RTP payload
    private byte[] payload;

    public RTPpacket(int pType, int frameNb, int time, byte[] data, int dataLength) {
        // Fill by default header fields:
        version = 2;
        padding = 0;
        extension = 0;
        cc = 0;
        marker = 0;
        ssrc = 0;

        // Fill changing header fields:
        sequenceNumber = frameNb;
        timeStamp = time;
        payloadType = pType;

        // Build the header bitstream:
        header = new byte[HEADER_SIZE];

        // Fill the header array of byte with RTP header fields
        header[0] = (byte) ((version << 6) | (padding << 5) | (extension << 4) | cc);
        header[1] = (byte) ((marker << 7) | (payloadType & 0x000000FF));
        header[2] = (byte) (sequenceNumber >> 8);
        header[3] = (byte) (sequenceNumber & 0xFF);
        header[4] = (byte) (timeStamp >> 24);
        header[5] = (byte) (timeStamp >> 16);
        header[6] = (byte) (timeStamp >> 8);
        header[7] = (byte) (timeStamp & 0xFF);
        header[8] = (byte) (ssrc >> 24);
        header[9] = (byte) (ssrc >> 16);
        header[10] = (byte) (ssrc >> 8);
        header[11] = (byte) (ssrc & 0xFF);

        // Fill the payload bitstream:
        payloadSize = dataLength;
        payload = Arrays.copyOf(data, dataLength);
    }


    public RTPpacket(byte[] packet, int packetSize) {
        // Fill default fields:
        version = 2;
        padding = 0;
        extension = 0;
        cc = 0;
        marker = 0;
        ssrc = 0;

        // Check if total packet size is lower than the header size
        if (packetSize >= HEADER_SIZE) {
            // Get the header bitstream:
            header = Arrays.copyOfRange(packet, 0, HEADER_SIZE);

            // Get the payload bitstream:
            payloadSize = packetSize - HEADER_SIZE;
            payload = Arrays.copyOfRange(packet, HEADER_SIZE, packetSize);

            // Interpret the changing fields of the header:
            payloadType = header[1] & 127;
            sequenceNumber = unsignedInt(header[3]) + 256 * unsignedInt(header[2]);
            timeStamp = unsignedInt(header[7]) + 256 * unsignedInt(header[6]) + 65536 * unsignedInt(header[5])
                    + 16777216 * unsignedInt(header[4]);
        }
    }

    // Get payload: return the payload bitstream of the CustomRTPpacket and its size
    public int getPayload(byte[] data) {
        System.arraycopy(payload, 0, data, 0, payloadSize);
        return payloadSize;
    }

    public int getPayloadLength() {
        return payloadSize;
    }

    public int getLength() {
        return payloadSize + HEADER_SIZE;
    }

    // Get packet: returns the packet bitstream and its length
    public int getPacket(byte[] packet) {
        System.arraycopy(header, 0, packet, 0, HEADER_SIZE);
        System.arraycopy(payload, 0, packet, HEADER_SIZE, payloadSize);
        return payloadSize + HEADER_SIZE;
    }

    public int getTimestamp() {
        return timeStamp;
    }


    public int getSequenceNumber() {
        return sequenceNumber;
    }


    public int getPayloadType() {
        return payloadType;
    }

    // Print headers without the SSRC
    public void printHeader() {
        System.out.print("[RTP-Header] ");
        System.out.println("Version: " + version + ", Padding: " + padding + ", Extension: " + extension + ", CC: " + cc
                + ", Marker: " + marker + ", PayloadType: " + payloadType + ", SequenceNumber: " + sequenceNumber
                + ", TimeStamp: " + timeStamp);
    }

    // Return the unsigned value of 8-bit integer nb
    private int unsignedInt(int nb) {
        return (nb >= 0) ? nb : 256 + nb;
    }
}
